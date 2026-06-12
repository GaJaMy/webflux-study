# MVC vs WebFlux First Summary

이 문서는 Spring MVC와 Spring WebFlux 비교 학습의 1차 마무리 기록이다.

## 실험 목적

목표는 WebFlux가 항상 더 빠르다는 결론을 내는 것이 아니라, 어떤 조건에서 MVC와 WebFlux가 다르게 동작하는지 수치로 확인하는 것이다.

핵심 관찰 대상:

```text
blocking / non-blocking
worker thread / event-loop thread
평균 응답 시간
p95 응답 시간
RPS
실패율
downstream 병목
```

## 모듈 구조

```text
webflux
├── mvc-api
├── webflux-api
├── mock-api
├── mock-webflux-api
├── load-test
└── docs
```

역할:

```text
mvc-api
→ Spring MVC 실험 서버, port 8080

webflux-api
→ Spring WebFlux 실험 서버, port 8081

mock-api
→ MVC + Thread.sleep 기반 외부 API mock, port 8090

mock-webflux-api
→ WebFlux + Mono.delay 기반 외부 API mock, port 8091

load-test
→ k6 부하 테스트 스크립트
```

## 실험 1: 단순 Delay

조건:

```text
300 VU
30s
delay 1000ms
```

결과:

| 구현 | RPS | avg | p95 | failed |
|---|---:|---:|---:|---:|
| MVC + Thread.sleep | 195.98/s | 1.49s | 2.00s | 0% |
| WebFlux + Mono.delay | 298.33/s | 1.00s | 1.01s | 0% |

결론:

```text
MVC는 Thread.sleep 동안 worker thread를 점유한다.
WebFlux는 Mono.delay로 지연을 예약하고 thread를 점유하지 않는다.
대기 작업이 non-blocking으로 표현되면 WebFlux가 고동시성에서 유리하다.
```

## 실험 2: WebFlux 내부 Blocking

조건:

```text
300 VU
30s
delay 1000ms
```

결과:

| 구현 | RPS | avg | p95 | failed |
|---|---:|---:|---:|---:|
| WebFlux + Mono.delay | 298.33/s | 1.00s | 1.01s | 0% |
| WebFlux + Thread.sleep | 17.47/s | 10.32s | 41.08s | 32.62% |
| WebFlux + Thread.sleep + boundedElastic | 110.30/s | 2.46s | 4.00s | 0% |

결론:

```text
WebFlux를 사용한다고 자동으로 non-blocking이 되는 것은 아니다.
event-loop thread에서 Thread.sleep 같은 blocking 코드를 실행하면 요청 처리뿐 아니라 네트워크 이벤트 처리까지 밀릴 수 있다.
boundedElastic은 event-loop를 보호하지만, blocking 작업 자체를 non-blocking으로 바꾸지는 않는다.
```

## 실험 3: 외부 API 호출, MVC Mock

조건:

```text
300 VU
30s
mock-api: MVC + Thread.sleep, port 8090
```

결과:

| 구현 | RPS | avg | p95 | failed |
|---|---:|---:|---:|---:|
| MVC + RestTemplate -> MVC mock | 193.64/s | 1.51s | 2.00s | 0% |
| WebFlux + WebClient -> MVC mock | 193.80/s | 1.50s | 1.94s | 0% |

결론:

```text
호출자 쪽이 WebFlux + WebClient여도 downstream인 mock-api가 MVC + Thread.sleep 구조로 병목이면 전체 처리량은 downstream 한계에 묶인다.
WebFlux는 내 서버의 thread 대기 문제를 줄일 수 있지만, 외부 서버의 병목을 없애지는 못한다.
```

## 실험 4: 외부 API 호출, WebFlux Mock

조건:

```text
300 VU
30s
mock-webflux-api: WebFlux + Mono.delay, port 8091
```

결과:

| 구현 | RPS | avg | p95 | failed |
|---|---:|---:|---:|---:|
| MVC + RestTemplate -> WebFlux mock | 193.60/s | 1.51s | 2.00s | 0% |
| WebFlux + WebClient -> WebFlux mock | 295.02/s | 1.01s | 1.05s | 0% |

결론:

```text
외부 API 서버가 충분히 버틸 수 있는 상황에서는 호출자 쪽 blocking/non-blocking 차이가 명확하게 드러난다.
MVC + RestTemplate은 외부 응답 대기 동안 worker thread를 점유한다.
WebFlux + WebClient는 외부 응답 대기 동안 event-loop thread를 점유하지 않는다.
```

## 핵심 결론

```text
1. WebFlux는 I/O 대기 중심의 고동시성 요청에서 장점이 있다.
2. WebFlux의 장점은 내부 작업이 non-blocking으로 구성될 때 나온다.
3. WebFlux 안에서 blocking 코드를 직접 실행하면 MVC보다 더 위험할 수 있다.
4. boundedElastic은 blocking 작업을 격리하는 타협안이지, non-blocking 전환이 아니다.
5. 전체 처리량은 호출자뿐 아니라 downstream 서버, DB, 네트워크 병목에도 영향을 받는다.
6. 단순히 WebFlux를 도입한다고 모든 성능 문제가 해결되지는 않는다.
```

## WebFlux가 적합한 경우

```text
외부 API 호출이 많고 대기 시간이 긴 서비스
SSE/streaming/long polling
동시 pending 요청이 많은 서비스
WebClient, R2DBC, Reactive Redis 등 non-blocking stack을 사용할 수 있는 경우
적은 thread로 많은 I/O 대기 요청을 처리해야 하는 경우
```

## WebFlux를 조심해야 하는 경우

```text
JPA/JDBC 중심 서비스
FeignClient, RestTemplate 같은 blocking client를 많이 쓰는 서비스
CPU 계산 중심 작업
팀이 reactive programming에 익숙하지 않은 경우
단순 CRUD 중심이고 MVC로 충분한 경우
```

## 다음 학습 후보

우선순위:

```text
1. WebClient timeout, fallback, retry 실험
2. SSE: MVC SseEmitter vs WebFlux Flux.interval
3. DB 비교: MVC + JPA vs WebFlux + R2DBC
4. blocking client를 boundedElastic으로 격리하는 외부 API 실험
```

## 참고 문서

```text
docs/study-checkpoint.md
→ 전체 실험 과정과 수치 기록

docs/webflux-concepts.md
→ Mono, Flux, WebClient, Spring Event 등 개념 정리

docs/reactor-operators.md
→ Reactor 연산자 정리

docs/traffic-bottlenecks.md
→ 트래픽 병목 원인별 정리

docs/k6.md
→ k6 설치와 사용법
```

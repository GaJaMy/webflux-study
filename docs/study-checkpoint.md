# Study Checkpoint

이 문서는 다른 PC에서 이어서 공부할 때 현재까지의 맥락을 빠르게 복구하기 위한 기록이다.

## 현재 목표

Spring MVC와 Spring WebFlux를 같은 `/delay` API로 구현하고, k6 부하 테스트로 blocking/non-blocking 차이를 수치로 확인한다.

## 현재 구조

```text
webflux
├── mvc-api
├── webflux-api
├── load-test
└── docs
```

- `mvc-api`: Spring MVC + `Thread.sleep(ms)` 기반 blocking delay API
- `webflux-api`: Spring WebFlux + `Mono.delay(Duration.ofMillis(ms))` 기반 non-blocking delay API
- `load-test`: k6 부하 테스트 스크립트

## 구현 완료

### MVC

Endpoint:

```text
GET http://localhost:8080/delay?ms=1000
```

구현 요지:

```text
DelayController -> DelayService -> DelayResponse
DelayService는 Thread.sleep(ms)를 사용한다.
요청 thread가 delay 시간 동안 점유된다.
```

### WebFlux

Endpoint:

```text
GET http://localhost:8081/delay?ms=1000
```

구현 요지:

```text
DelayController -> DelayService -> Mono<DelayResponse>
DelayService는 Mono.delay(Duration.ofMillis(ms))를 사용한다.
요청 thread를 sleep시키지 않고 delay 완료 이벤트를 예약한다.
```

## 테스트 명령

전체 테스트:

```bash
./gradlew test
```

MVC 실행:

```bash
./gradlew :mvc-api:bootRun
```

WebFlux 실행:

```bash
./gradlew :webflux-api:bootRun
```

## k6 스크립트

MVC load test:

```bash
k6 run load-test/delay-load.js
```

WebFlux load test:

```bash
k6 run load-test/webflux-delay-load.js
```

기본 비교 조건:

```text
VU: 300
Duration: 30s
Delay: 1000ms
k6 sleep: 없음
```

## 측정 결과

### MVC 결과

조건:

```text
GET /delay?ms=1000
300 VU
30s
```

결과:

```text
http_req_duration avg: 1.49s
http_req_duration p95: 2.00s
http_req_failed: 0.00%
http_reqs rate: 195.98/s
total requests: 6100
threshold p95<1500ms: failed
```

해석:

```text
MVC는 Thread.sleep 동안 Tomcat worker thread를 점유한다.
300 VU에서 worker thread 한계로 일부 요청이 큐에서 대기했다.
실패는 없었지만 p95가 2초로 증가했다.
```

### WebFlux 결과

조건:

```text
GET /delay?ms=1000
300 VU
30s
```

결과:

```text
http_req_duration avg: 1.00s
http_req_duration p95: 1.01s
http_req_failed: 0.00%
http_reqs rate: 298.33/s
total requests: 9000
threshold p95<1500ms: passed
```

해석:

```text
WebFlux는 Mono.delay로 delay 작업을 예약하고 요청 thread를 점유하지 않는다.
300 VU에서도 p95가 1초 근처로 유지되었다.
RPS는 300 VU 이론치에 가까운 약 298/s까지 나왔다.
```

## 현재까지의 핵심 결론

```text
MVC + Thread.sleep은 대기 시간 동안 worker thread를 점유한다.
WebFlux + Mono.delay는 대기 시간을 이벤트로 예약하고 thread를 점유하지 않는다.
같은 300 VU, 1초 delay 조건에서 WebFlux는 MVC보다 p95가 낮고 RPS가 높았다.
```

주의:

```text
이 결과는 I/O 대기 또는 timer delay 같은 non-blocking에 적합한 작업 기준이다.
WebFlux가 항상 MVC보다 빠르다는 뜻은 아니다.
WebFlux 안에서 blocking 코드를 쓰면 장점이 사라질 수 있다.
```

## 다음에 할 실험

다음 실험은 WebFlux 안에서 일부러 blocking 코드를 사용해 보는 것이다.

추가할 API:

```text
GET /blocking-delay?ms=1000
```

목표:

```text
WebFlux 프로젝트 안에서 Thread.sleep(ms)를 사용하면 어떤 성능 저하가 생기는지 확인한다.
WebFlux를 쓴다고 자동으로 non-blocking이 되는 것이 아님을 검증한다.
```

구현 순서:

```text
1. webflux-api에 BlockingDelayServiceTest 작성
2. BlockingDelayService 구현
3. DelayController에 /blocking-delay 추가
4. load-test/webflux-blocking-delay-load.js 작성
5. k6로 /delay와 /blocking-delay 결과 비교
```

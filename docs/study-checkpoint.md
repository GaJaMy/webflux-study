# Study Checkpoint

이 문서는 다른 PC에서 이어서 공부할 때 현재까지의 맥락을 빠르게 복구하기 위한 기록이다.

## 현재 목표

Spring MVC와 Spring WebFlux를 같은 `/delay` API로 구현하고, k6 부하 테스트로 blocking/non-blocking 차이를 수치로 확인한다.

## 현재 구조

```text
webflux
├── mvc-api
├── webflux-api
├── mock-api
├── load-test
└── docs
```

- `mvc-api`: Spring MVC + `Thread.sleep(ms)` 기반 blocking delay API
- `webflux-api`: Spring WebFlux + `Mono.delay(Duration.ofMillis(ms))` 기반 non-blocking delay API
- `mock-api`: 외부 API 호출 비교용 별도 지연 API 서버, port `8090`
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

Mock API 실행:

```bash
./gradlew :mock-api:bootRun
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
WebFlux + Thread.sleep은 event-loop thread를 막아 심각한 timeout과 응답 지연을 만들 수 있다.
```

주의:

```text
이 결과는 I/O 대기 또는 timer delay 같은 non-blocking에 적합한 작업 기준이다.
WebFlux가 항상 MVC보다 빠르다는 뜻은 아니다.
WebFlux 안에서 blocking 코드를 쓰면 장점이 사라질 수 있다.
```

개념 정리는 `docs/webflux-concepts.md`에 따로 기록했다. Mono/Flux, Spring Event, RestTemplate, Future, WebClient 차이가 헷갈리면 먼저 그 문서를 확인한다.

## 완료한 실험: WebFlux blocking

WebFlux 안에서 일부러 blocking 코드를 사용하는 실험까지 완료했다.

추가할 API:

```text
GET /blocking-delay?ms=1000
```

목표:

```text
WebFlux 프로젝트 안에서 Thread.sleep(ms)를 사용하면 어떤 성능 저하가 생기는지 확인한다.
WebFlux를 쓴다고 자동으로 non-blocking이 되는 것이 아님을 검증한다.
```

구현 완료:

```text
1. webflux-api에 BlockingDelayServiceTest 작성
2. BlockingDelayService 구현
3. DelayController에 /blocking-delay 추가
4. BlockingDelayControllerTest 작성
5. load-test/webflux-blocking-delay-load.js 작성
6. k6로 /delay와 /blocking-delay 결과 비교
```

### WebFlux blocking 결과

조건:

```text
GET /blocking-delay?ms=1000
300 VU
30s
Implementation: WebFlux + Thread.sleep
```

결과:

```text
checks_succeeded: 89.20%
checks_failed: 10.79%
status 200 check: 67%
http_req_duration avg: 10.32s
http_req_duration p95: 41.08s
http_req_duration max: 43.09s
http_req_failed: 32.62%
http_reqs rate: 17.47/s
total requests: 895
```

비교 기준:

```text
WebFlux + Mono.delay
- RPS: 298.33/s
- avg: 1.00s
- p95: 1.01s
- failed: 0.00%
```

해석:

```text
/blocking-delay는 WebFlux 앱 안에서 Thread.sleep(1000)을 실행한다.
이 경우 reactor-http-nio event-loop thread가 blocking된다.
event-loop thread는 요청 읽기, 응답 쓰기, 연결 처리, Controller 호출을 담당하므로 blocking 영향이 크게 퍼진다.
그 결과 RPS는 약 17/s로 떨어졌고, 실패율은 32.62%, p95는 41초까지 증가했다.
```

결론:

```text
WebFlux를 사용한다고 자동으로 non-blocking이 되는 것은 아니다.
WebFlux event-loop에서 blocking 코드를 실행하면 MVC보다 더 위험할 수 있다.
WebFlux의 장점은 내부 작업까지 non-blocking으로 구성될 때 나온다.
```

## 완료한 실험: boundedElastic

WebFlux에서 어쩔 수 없이 blocking 작업을 호출해야 하는 경우를 다루는 boundedElastic 실험까지 완료했다.

추가 API:

```text
GET /bounded-elastic-delay?ms=1000
```

목표:

```text
Thread.sleep 같은 blocking 작업을 event-loop에서 직접 실행하지 않고,
Schedulers.boundedElastic()로 격리하면 어떤 차이가 있는지 확인한다.
```

구현 요지:

```text
BoundedElasticDelayService
→ Mono.fromCallable(...)
→ 내부에서 Thread.sleep(ms)
→ subscribeOn(Schedulers.boundedElastic())
```

테스트 정리:

```text
DelayController는 하나이므로 Controller 테스트도 DelayControllerTest에 모은다.
/delay, /blocking-delay, /bounded-elastic-delay 테스트를 같은 Controller 테스트 클래스에서 관리한다.
boundedElastic thread 확인은 응답 threadName에 "boundedElastic"이 포함되는지 검증한다.
```

### WebFlux boundedElastic 결과

조건:

```text
GET /bounded-elastic-delay?ms=1000
300 VU
30s
Implementation: WebFlux + Thread.sleep + Schedulers.boundedElastic()
```

결과:

```text
checks_succeeded: 100.00%
checks_failed: 0.00%
http_req_duration avg: 2.46s
http_req_duration p95: 4.00s
http_req_duration max: 7.02s
http_req_failed: 0.00%
http_reqs rate: 110.30/s
total requests: 3780
```

비교:

```text
WebFlux + Mono.delay
- RPS: 298.33/s
- avg: 1.00s
- p95: 1.01s
- failed: 0.00%

WebFlux + Thread.sleep on event-loop
- RPS: 17.47/s
- avg: 10.32s
- p95: 41.08s
- failed: 32.62%

WebFlux + Thread.sleep on boundedElastic
- RPS: 110.30/s
- avg: 2.46s
- p95: 4.00s
- failed: 0.00%
```

해석:

```text
boundedElastic은 event-loop를 직접 blocking하지 않도록 보호한다.
그 결과 /blocking-delay에서 발생한 timeout과 실패율은 크게 줄었다.
하지만 Thread.sleep 자체는 여전히 blocking 작업이므로 boundedElastic thread를 점유한다.
요청이 많아지면 boundedElastic thread pool과 queue에서 대기 시간이 생긴다.
따라서 순수 non-blocking인 Mono.delay보다는 처리량이 낮고 p95가 높다.
```

결론:

```text
boundedElastic은 WebFlux에서 blocking 작업을 다룰 때 사용하는 타협안이다.
event-loop는 보호하지만 blocking 작업 자체가 non-blocking으로 바뀌는 것은 아니다.
가능하면 WebClient, R2DBC 같은 non-blocking 기술을 사용하고,
어쩔 수 없이 JPA/JDBC/FeignClient/RestTemplate/legacy SDK 같은 blocking 코드를 써야 할 때 격리 용도로 고려한다.
```

## 진행 중인 실험: 외부 API 호출 비교

외부 API 호출 비교를 진행한다.

```text
MVC + RestTemplate 또는 FeignClient
WebFlux + WebClient
WebFlux + blocking client + boundedElastic
```

목표:

```text
실무에서 자주 나오는 외부 API I/O 대기 상황을 MVC와 WebFlux로 비교한다.
```

## 외부 API 호출 비교

외부 API 역할을 별도 `mock-api` 모듈로 분리했다.

구조:

```text
mock-api: 8090
mvc-api: 8080
webflux-api: 8081
```

Mock API:

```text
GET http://localhost:8090/mock-external-delay?ms=1000
```

현재 `mock-api`는 Spring MVC + `Thread.sleep(ms)` 기반이다. 따라서 외부 API 서버 자체가 약 200 RPS 근처에서 병목이 될 수 있다.

### MVC + RestTemplate 결과

조건:

```text
GET http://localhost:8080/external-delay?ms=1000
300 VU
30s
Implementation: MVC + RestTemplate -> mock-api
```

결과:

```text
checks_succeeded: 100.00%
checks_failed: 0.00%
http_req_duration avg: 1.51s
http_req_duration p95: 2.00s
http_req_duration max: 2.34s
http_req_failed: 0.00%
http_reqs rate: 193.64/s
total requests: 6100
```

해석:

```text
RestTemplate은 blocking HTTP client다.
MVC worker thread는 mock-api 응답이 올 때까지 대기한다.
300 VU에서는 worker thread 한계 때문에 일부 요청이 큐에서 대기했고 p95가 약 2초가 되었다.
```

### WebFlux + WebClient 결과

조건:

```text
GET http://localhost:8081/external-delay?ms=1000
300 VU
30s
Implementation: WebFlux + WebClient -> mock-api
```

결과:

```text
checks_succeeded: 100.00%
checks_failed: 0.00%
http_req_duration avg: 1.50s
http_req_duration p95: 1.94s
http_req_duration max: 2.36s
http_req_failed: 0.00%
http_reqs rate: 193.80/s
total requests: 6100
threshold p95<1500ms: failed
```

해석:

```text
WebFlux + WebClient는 호출하는 webflux-api의 event-loop thread를 blocking하지 않는다.
하지만 호출 대상인 mock-api가 MVC + Thread.sleep 기반이라 mock-api의 worker thread 처리량이 병목이 되었다.
그 결과 MVC + RestTemplate과 WebFlux + WebClient의 k6 결과가 거의 비슷하게 나왔다.
```

결론:

```text
WebFlux + WebClient는 내 서버의 thread 대기 문제를 줄일 수 있다.
하지만 외부 API 서버 자체가 blocking 구조로 병목이면 전체 처리량은 외부 API 처리량에 묶인다.
k6의 RPS/p95만 보면 차이가 가려질 수 있으므로, thread 사용량이나 외부 서버 병목도 함께 봐야 한다.
```

다음 실험:

```text
mock-api를 WebFlux + Mono.delay 기반 non-blocking mock 서버로 바꾼다.
그 후 MVC + RestTemplate과 WebFlux + WebClient를 다시 비교한다.
예상: MVC는 호출 thread가 blocking되어 worker thread 한계에 묶이고, WebFlux는 더 높은 동시성을 보일 수 있다.
```

## 다음 시작 지점

다음 공부를 시작할 때는 여기서 이어간다.

```text
1. mock-api를 현재 Spring MVC + Thread.sleep에서 WebFlux + Mono.delay 구조로 바꾼다.
2. mock-api endpoint는 그대로 유지한다: GET /mock-external-delay?ms=1000
3. mock-api port도 그대로 유지한다: 8090
4. MVC + RestTemplate 결과를 다시 측정한다.
5. WebFlux + WebClient 결과를 다시 측정한다.
6. mock-api 병목이 사라졌을 때 호출자 쪽 MVC/WebFlux 차이가 어떻게 달라지는지 비교한다.
```

실행 순서:

```bash
./gradlew :mock-api:bootRun
./gradlew :mvc-api:bootRun
k6 run load-test/mvc-external-delay-load.js
```

```bash
./gradlew :mock-api:bootRun
./gradlew :webflux-api:bootRun
k6 run load-test/webflux-external-delay-load.js
```

# Spring MVC vs WebFlux Study

Spring MVC와 Spring WebFlux를 같은 요구사항으로 구현하고, 성능과 구조 차이를 수치로 기록하며 학습하는 프로젝트입니다.

## 목표

이 프로젝트의 목표는 단순히 WebFlux가 더 빠른지 확인하는 것이 아닙니다. MVC와 WebFlux가 어떤 상황에서 다르게 동작하는지, 특히 블로킹/논블로킹 I/O, 스레드 사용량, 응답 시간, 처리량 차이를 직접 측정하는 것입니다.

주요 비교 질문은 다음과 같습니다.

- MVC는 동시 요청이 늘 때 스레드가 얼마나 증가하는가?
- WebFlux는 같은 상황에서 스레드를 덜 사용하는가?
- 단순 CRUD에서도 WebFlux가 유리한가?
- 외부 API 호출이나 지연이 많은 상황에서 차이가 커지는가?
- JPA를 사용하면 WebFlux의 장점이 줄어드는가?
- R2DBC는 어떤 장점과 트레이드오프가 있는가?

## 진행 계획

1. MVC 기준 API를 먼저 구현합니다.
2. 같은 기능을 WebFlux 방식으로 다시 구현합니다.
3. 부하 테스트 도구로 동일 조건에서 측정합니다.
4. 응답 시간, 처리량, 실패율, 스레드 수, CPU/메모리 사용량을 기록합니다.
5. 결과를 문서화하며 어떤 상황에 어떤 기술이 적합한지 정리합니다.

## 현재 프로젝트 상태

현재 프로젝트는 Gradle 멀티 모듈입니다. `mvc-api`에는 Spring MVC 기준 구현이 있고, `webflux-api`에는 같은 실험을 WebFlux 방식으로 구현합니다. `mock-api`는 외부 API 호출 실험을 위한 별도 지연 API 서버입니다.

```text
.
├── build.gradle
├── settings.gradle
├── mvc-api
│   └── src
├── webflux-api
│   └── src
├── mock-api
│   └── src
├── load-test
├── docs
└── AGENTS.md
```

## 현재 구현 범위

초기 학습은 DB 없이 delay API만 사용합니다. 목적은 비즈니스 기능 구현이 아니라 MVC/WebFlux의 blocking, non-blocking 처리 차이를 분리해서 관찰하는 것입니다.

현재 구현한 API:

```text
MVC
GET http://localhost:8080/delay?ms=1000
GET http://localhost:8080/external-delay?ms=1000

WebFlux
GET http://localhost:8081/delay?ms=1000
GET http://localhost:8081/blocking-delay?ms=1000
GET http://localhost:8081/bounded-elastic-delay?ms=1000

Mock external API
GET http://localhost:8090/mock-external-delay?ms=1000
```

`/delay` API는 성능 비교의 핵심입니다. MVC에서는 `Thread.sleep(...)`으로 요청 스레드를 점유하고, WebFlux에서는 `Mono.delay(...)`로 논블로킹 지연을 구현해 차이를 비교합니다.

추가로 WebFlux 내부에서 `Thread.sleep(...)`을 직접 사용하는 `/blocking-delay`와, blocking 작업을 `Schedulers.boundedElastic()`으로 격리하는 `/bounded-elastic-delay`를 비교합니다.

외부 API 호출 비교에서는 `mock-api`를 별도 서버로 띄우고, MVC는 `RestTemplate`으로 `http://localhost:8090/mock-external-delay`를 호출합니다. 이렇게 해야 MVC 서버가 자기 자신을 호출하며 같은 worker pool을 고갈시키는 self-call 실험이 아니라, 실제 외부 API 대기 상황에 가까워집니다.

현재 delay 패키지 구조:

```text
mvc-api/src/main/java/com/gajamy/webflux
└── delay

webflux-api/src/main/java/com/gajamy/webflux/webfluxapi
└── delay

mock-api/src/main/java/com/gajamy/mockapi
└── delay
```

`products`, `orders` 같은 일반 CRUD 비교는 이후 DB/JPA/R2DBC 단계에서 확장합니다.

## 측정 항목

성능 테스트를 진행할 때 다음 값을 기록합니다.

- 평균 응답 시간
- p95 / p99 응답 시간
- 초당 처리량, RPS
- 실패율
- CPU 사용량
- 메모리 사용량
- 스레드 수

결과는 `docs/performance-log.md`에 시나리오별로 기록합니다.

예시:

```md
# Performance Log

## 2026-06-09 MVC baseline

### Scenario

- Endpoint: GET /delay?ms=1000
- Tool: k6
- Virtual users: 100
- Duration: 30s

### Result

- avg:
- p95:
- p99:
- RPS:
- failures:
- thread count:

### Notes

-
```

## 실행 명령어

프로젝트 루트에서 Gradle wrapper를 사용합니다.

```bash
./gradlew :mvc-api:bootRun
```

MVC 애플리케이션을 로컬에서 실행합니다.

```bash
./gradlew :webflux-api:bootRun
```

WebFlux 애플리케이션을 로컬에서 실행합니다. 기본 포트는 `8081`입니다.

```bash
./gradlew :mock-api:bootRun
```

외부 API 역할의 mock 애플리케이션을 로컬에서 실행합니다. 기본 포트는 `8090`입니다.

```bash
./gradlew test
```

JUnit 테스트를 실행합니다.

```bash
./gradlew build
```

컴파일, 테스트, 패키징을 수행합니다.

## 학습 메모

초반에는 복잡한 DB 연동보다 지연 API에 집중합니다. 현재는 다음 세 가지를 비교했습니다.

```text
MVC + Thread.sleep
WebFlux + Mono.delay
WebFlux + Thread.sleep
WebFlux + Thread.sleep + boundedElastic
```

이후 단계에서는 외부 API 호출 비교를 진행합니다.

```text
MVC + RestTemplate 또는 FeignClient
WebFlux + WebClient
WebFlux + blocking client + boundedElastic
```

그 다음 JPA 기반 MVC와 R2DBC 기반 WebFlux를 비교하면 블로킹 I/O와 논블로킹 I/O의 차이를 더 실무적으로 확인할 수 있습니다.

현재까지의 실험 결과와 다음 학습 단계는 `docs/study-checkpoint.md`에 기록합니다. 다른 PC에서 이어서 공부할 때 먼저 이 문서를 확인합니다.

WebFlux 기본 개념은 `docs/webflux-concepts.md`에 정리합니다.

Mono/Flux 연산자 사용법은 `docs/reactor-operators.md`에 정리합니다.

트래픽 장애 원인과 해결책은 `docs/traffic-bottlenecks.md`에 정리합니다.

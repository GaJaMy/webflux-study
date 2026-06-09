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

현재 프로젝트는 Gradle 멀티 모듈입니다. `mvc-api`에는 Spring MVC 기준 구현이 있고, `webflux-api`는 이후 같은 기능을 WebFlux로 구현하기 위한 최소 골격만 있습니다.

```text
.
├── build.gradle
├── settings.gradle
├── mvc-api
│   └── src
├── webflux-api
│   └── src
├── load-test
├── docs
└── AGENTS.md
```

## 첫 번째 구현 범위

가장 먼저 MVC 기준 API를 구현합니다. DB 없이 메모리 저장소로 시작하여 프레임워크 동작 차이에 집중합니다.

구현할 API:

```text
GET  /products
GET  /products/{id}
POST /orders
GET  /orders/{id}
GET  /delay?ms=1000
```

`/delay` API는 성능 비교의 핵심입니다. MVC에서는 `Thread.sleep(...)`으로 요청 스레드를 점유하고, WebFlux에서는 이후 `Mono.delay(...)`로 논블로킹 지연을 구현해 차이를 비교합니다.

MVC API 패키지 구조:

```text
mvc-api/src/main/java/com/gajamy/webflux
├── product
├── order
└── delay
```

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
./gradlew test
```

JUnit 테스트를 실행합니다.

```bash
./gradlew build
```

컴파일, 테스트, 패키징을 수행합니다.

## 학습 메모

초반에는 복잡한 DB 연동보다 메모리 저장소와 지연 API에 집중합니다. 이후 단계에서 JPA 기반 MVC와 R2DBC 기반 WebFlux를 비교하면 블로킹 I/O와 논블로킹 I/O의 차이를 더 명확하게 확인할 수 있습니다.

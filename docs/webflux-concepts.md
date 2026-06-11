# WebFlux Concepts

이 문서는 MVC/WebFlux 공부 중 헷갈렸던 개념을 다시 보기 위한 정리다.

## Mono와 Flux

`Mono<T>`는 나중에 값이 0개 또는 1개 발행될 수 있는 reactive 흐름이다.

```text
Mono<User> = 나중에 User가 0개 또는 1개 나올 수 있음
```

`Flux<T>`는 나중에 값이 0개 이상 여러 개 발행될 수 있는 reactive 흐름이다.

```text
Flux<User> = 나중에 User가 여러 개 나올 수 있음
```

WebFlux Controller가 `Mono`나 `Flux`를 반환하면 Spring WebFlux가 그 흐름을 구독하고, 값이 발행되면 HTTP 응답으로 작성한다.

## Mono/Flux와 Spring Event

`Mono`와 `Flux`는 Spring Event를 감싼 것이 아니다.

```text
Mono/Flux
→ 현재 요청의 응답 흐름을 비동기적으로 표현하는 Reactor 타입

Spring Event
→ 애플리케이션 내부에서 어떤 일이 발생했음을 listener에게 알리는 메커니즘
```

Spring Event는 보통 회원가입 완료, 주문 생성, 문서 업로드 완료 같은 후속 작업을 분리할 때 쓴다. 기본적으로는 동기 실행될 수 있고, `@Async`나 별도 executor 설정을 해야 비동기로 동작한다.

정리:

```text
Mono/Flux는 요청의 응답 흐름 표현에 가깝다.
Spring Event는 후속 작업 알림에 가깝다.
```

## Blocking과 Non-Blocking

Blocking은 기다리는 동안 thread를 점유한다.

```text
Thread.sleep
JDBC/JPA
RestTemplate
```

Non-blocking은 기다리는 작업을 등록하고 thread를 점유하지 않는다.

```text
Mono.delay
WebClient
R2DBC
```

MVC에서 `Thread.sleep`을 쓰면 Tomcat worker thread가 막힌다. WebFlux에서 `Thread.sleep`을 event-loop thread에서 실행하면 네트워크 이벤트 처리까지 밀릴 수 있어 더 위험할 수 있다.

## RestTemplate, Future, WebClient

`RestTemplate`은 blocking HTTP client다.

```text
외부 API 응답이 올 때까지 호출한 thread가 대기한다.
```

`Future`나 `CompletableFuture`는 나중에 완료될 결과를 표현한다. `CompletableFuture.supplyAsync(...)`를 쓰면 현재 thread의 작업을 다른 thread pool로 넘길 수 있다.

주의:

```text
Future는 blocking 작업을 non-blocking으로 바꾸는 마법이 아니다.
RestTemplate을 Future로 감싸면 다른 thread가 RestTemplate 응답을 기다릴 뿐이다.
```

`WebClient`는 non-blocking HTTP client다. 외부 API 호출 결과를 보통 `Mono`나 `Flux`로 반환한다.

주의:

```text
WebClient도 block()을 호출하면 그 지점은 blocking이다.
```

## WebFlux와 WebClient

WebFlux와 WebClient는 역할이 다르다.

```text
Spring WebFlux
→ 서버에서 HTTP 요청을 처리하는 웹 프레임워크

WebClient
→ 내 서버가 다른 서버로 HTTP 요청을 보낼 때 사용하는 클라이언트
```

지금 프로젝트의 `/delay` API는 외부 API를 호출하지 않으므로 WebClient가 필요 없다.

흐름:

```text
k6/curl/browser
→ WebFlux 서버
→ DelayController
→ DelayService
→ Mono<DelayResponse>
→ Spring WebFlux가 Mono 구독
→ DelayResponse를 JSON 응답으로 작성
```

Controller가 `Mono`를 반환할 때 그 Mono를 처리하는 것은 WebClient가 아니라 Spring WebFlux 런타임이다.

## 핵심 문장

```text
Mono는 값이 아니라 나중에 값이 나올 수 있는 비동기 흐름이다.
WebFlux는 Controller가 반환한 Mono/Flux를 구독해 HTTP 응답으로 연결한다.
WebClient는 외부 HTTP API를 호출할 때 사용하는 클라이언트다.
```

Mono/Flux 연산자와 함수 사용법은 `docs/reactor-operators.md`에 따로 정리한다.

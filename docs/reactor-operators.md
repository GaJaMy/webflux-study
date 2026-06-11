# Reactor Operators Guide

WebFlux를 제대로 쓰려면 `Mono`와 `Flux`가 제공하는 연산자를 익혀야 한다. 이 문서는 현재 프로젝트 학습에 필요한 연산자부터 정리한다.

## 기본 관점

`Mono`와 `Flux`는 값을 바로 들고 있는 객체가 아니라, 값이 나중에 발행될 수 있는 흐름이다.

```text
Mono<T> = 0개 또는 1개 값
Flux<T> = 0개 이상 여러 값
```

연산자는 이 흐름을 만들고, 바꾸고, 연결하고, 에러를 처리하는 함수다.

## 생성 연산자

### just

이미 가지고 있는 값을 `Mono`나 `Flux`로 감싼다.

```java
Mono.just("hello")
Flux.just("a", "b", "c")
```

주의:

```text
값이 이미 만들어져 있을 때 사용한다.
지연 실행이 필요한 경우 defer/fromSupplier를 고려한다.
```

### empty

값 없이 정상 완료되는 흐름을 만든다.

```java
Mono.empty()
Flux.empty()
```

예:

```text
조회 결과가 없지만 에러는 아닌 경우
```

### error

즉시 에러로 끝나는 흐름을 만든다.

```java
Mono.error(new IllegalArgumentException("invalid id"))
```

### defer

구독되는 시점에 흐름을 새로 만든다.

```java
Mono.defer(() -> Mono.just(System.currentTimeMillis()))
```

`just(System.currentTimeMillis())`는 조립 시점의 시간이 들어가지만, `defer`를 쓰면 구독 시점에 다시 계산된다.

### fromCallable

동기 함수를 `Mono`로 감싼다.

```java
Mono.fromCallable(() -> blockingRepository.findById(id))
```

주의:

```text
fromCallable 자체가 blocking을 non-blocking으로 바꾸지는 않는다.
blocking 작업이면 subscribeOn(Schedulers.boundedElastic()) 같은 별도 scheduler가 필요하다.
```

## 변환 연산자

### map

발행된 값을 다른 값으로 바꾼다.

```java
Mono<User> user = Mono.just("howard")
        .map(name -> new User(name));
```

사용 기준:

```text
T -> R
일반 값을 일반 값으로 바꿀 때 사용
```

현재 프로젝트 예:

```java
Mono.delay(Duration.ofMillis(ms))
        .map(ignored -> new DelayResponse(...))
```

### flatMap

값을 받아서 다시 `Mono`를 반환하는 비동기 작업을 이어 붙인다.

```java
userService.findById(id)
        .flatMap(user -> orderService.findLatestOrder(user.id()));
```

사용 기준:

```text
T -> Mono<R>
다음 작업도 비동기 흐름일 때 사용
```

`map`을 잘못 쓰면 `Mono<Mono<R>>`가 될 수 있다.

### flatMapMany

`Mono<T>`에서 여러 값을 내는 `Flux<R>`로 전환할 때 사용한다.

```java
Mono<User> userMono = userService.findById(id);

Flux<Order> orders = userMono
        .flatMapMany(user -> orderService.findOrders(user.id()));
```

## 연결 연산자

### then

앞의 값은 무시하고 완료 신호만 이어간다.

```java
saveLog()
        .then(sendNotification())
```

사용 기준:

```text
앞 작업의 결과값은 필요 없고, 완료 후 다음 작업을 실행하고 싶을 때
```

### thenReturn

앞 작업이 완료되면 지정한 값을 발행한다.

```java
saveLog()
        .thenReturn("OK")
```

주의:

```text
반환할 값을 완료 시점에 계산해야 하면 map이나 defer를 고려한다.
```

### zip

여러 `Mono`를 동시에 실행하고, 모두 완료되면 결과를 묶는다.

```java
Mono.zip(userMono, orderMono)
        .map(tuple -> new UserOrderResponse(tuple.getT1(), tuple.getT2()));
```

사용 기준:

```text
서로 독립적인 비동기 작업을 병렬로 진행하고 결과를 합칠 때
```

## 필터링과 빈 값 처리

### filter

조건에 맞는 값만 통과시킨다.

```java
userMono.filter(user -> user.active())
```

조건이 false면 `Mono.empty()`처럼 값 없이 완료된다.

### switchIfEmpty

값이 없을 때 다른 흐름으로 대체한다.

```java
userRepository.findById(id)
        .switchIfEmpty(Mono.error(new UserNotFoundException(id)));
```

### defaultIfEmpty

값이 없을 때 기본값을 발행한다.

```java
userNameMono.defaultIfEmpty("anonymous")
```

## 에러 처리

### onErrorReturn

에러가 발생하면 고정된 기본값을 반환한다.

```java
externalApi.call()
        .onErrorReturn(new ExternalResponse("fallback"));
```

### onErrorResume

에러가 발생하면 다른 `Mono`나 `Flux`로 복구한다.

```java
externalApi.call()
        .onErrorResume(error -> fallbackApi.call());
```

사용 기준:

```text
에러 종류에 따라 다른 복구 흐름을 실행하고 싶을 때
```

### onErrorMap

에러를 다른 예외로 변환한다.

```java
repository.findById(id)
        .onErrorMap(error -> new ServiceException("조회 실패", error));
```

### timeout

지정 시간 안에 완료되지 않으면 에러로 끝낸다.

```java
webClient.get()
        .uri("/external")
        .retrieve()
        .bodyToMono(String.class)
        .timeout(Duration.ofSeconds(2));
```

외부 API 호출에서는 timeout을 명시하는 습관이 중요하다.

## 부수 효과와 디버깅

### doOnNext

값이 발행될 때 부수 작업을 실행한다.

```java
userMono.doOnNext(user -> log.info("user={}", user.id()))
```

주의:

```text
값을 바꾸는 용도가 아니다.
값을 바꾸려면 map을 사용한다.
```

### doOnSubscribe

구독이 시작될 때 실행된다.

```java
mono.doOnSubscribe(subscription -> log.info("start"))
```

### doOnSuccess

`Mono`가 성공적으로 완료될 때 실행된다.

```java
mono.doOnSuccess(result -> log.info("success"))
```

### doOnError

에러가 발생했을 때 실행된다.

```java
mono.doOnError(error -> log.error("failed", error))
```

### doFinally

완료, 에러, 취소와 관계없이 마지막에 실행된다.

```java
mono.doFinally(signalType -> log.info("finished: {}", signalType))
```

## 시간 관련 연산자

### delay

지정 시간 뒤 값을 발행한다.

```java
Mono.delay(Duration.ofSeconds(1))
```

현재 프로젝트의 non-blocking delay 실험에서 사용했다.

### interval

일정 간격으로 계속 값을 발행한다.

```java
Flux.interval(Duration.ofSeconds(1))
```

SSE나 주기적 이벤트 실험에 사용할 수 있다.

## 스케줄러

### subscribeOn

구독 및 upstream 작업을 어떤 scheduler에서 실행할지 지정한다.

```java
Mono.fromCallable(() -> blockingRepository.findById(id))
        .subscribeOn(Schedulers.boundedElastic())
```

blocking 작업을 WebFlux event-loop에서 직접 실행하지 않기 위한 타협안이다.

### publishOn

이후 downstream 연산을 어떤 scheduler에서 실행할지 바꾼다.

```java
mono.publishOn(Schedulers.parallel())
        .map(this::cpuWork)
```

처음에는 `subscribeOn`과 `publishOn`을 남발하지 말고, blocking 코드 격리 목적부터 이해한다.

## 절대 주의할 메서드

### block

`Mono`나 `Flux`의 값을 동기적으로 기다린다.

```java
DelayResponse response = mono.block();
```

WebFlux event-loop thread에서 `block()`을 호출하면 non-blocking 장점이 사라지고 장애 원인이 될 수 있다.

## 자주 쓰는 선택 기준

```text
값을 만든다                         -> just, empty, error, defer, fromCallable
값을 변환한다                       -> map
다음 작업도 Mono/Flux다             -> flatMap
Mono에서 Flux로 바꾼다              -> flatMapMany
값이 없을 때 처리한다               -> switchIfEmpty, defaultIfEmpty
에러를 복구한다                     -> onErrorReturn, onErrorResume
에러를 변환한다                     -> onErrorMap
시간 제한을 둔다                    -> timeout
로그만 찍는다                       -> doOnNext, doOnError, doFinally
여러 Mono 결과를 합친다             -> zip
완료 후 다른 작업을 실행한다         -> then
blocking 코드를 별도 thread로 보낸다 -> fromCallable + subscribeOn(boundedElastic)
동기적으로 값을 꺼낸다              -> block, WebFlux 내부에서는 주의
```

## 다음 학습 추천

현재 프로젝트에서 다음 순서로 연산자를 직접 써본다.

```text
1. /blocking-delay 실험으로 block 위험성 확인
2. WebClient 외부 API 호출 실험에서 flatMap, timeout, onErrorResume 사용
3. SSE 실험에서 Flux.interval 사용
4. 조회 결과 없음 처리에서 switchIfEmpty 사용
```

## 참고

- Reactor operator guide: https://projectreactor.io/docs/core/release/reference/apdx-operatorChoice.html
- Reactor core features: https://projectreactor.io/docs/core/release/reference/coreFeatures.html
- Spring WebFlux annotated controllers: https://docs.spring.io/spring-framework/reference/web/webflux/controller.html
- Spring WebClient: https://docs.spring.io/spring-framework/reference/web/webflux-webclient.html

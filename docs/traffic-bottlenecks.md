# Traffic Bottlenecks Guide

트래픽이 많을 때 발생하는 장애는 원인에 따라 해결책이 다르다. “트래픽이 많다”보다 “어디가 막혔는가”를 먼저 봐야 한다.

## 1. Server Thread Exhaustion

서버가 요청을 처리할 thread를 모두 사용해서 새 요청이 대기하는 상황이다.

주로 MVC에서 blocking 작업이 많을 때 발생한다.

예:

```text
Thread.sleep
RestTemplate
FeignClient
JDBC/JPA
느린 외부 API 호출
```

증상:

```text
p95/p99 응답 시간 증가
요청 timeout
Tomcat worker thread 사용량 증가
CPU는 낮은데 응답은 느림
```

확인할 것:

```text
thread dump
Tomcat thread pool metrics
Actuator metrics
k6 p95/p99
```

해결책:

```text
Tomcat thread pool 조정
blocking 작업 시간 단축
MVC 비동기 처리
WebFlux 도입
작업 큐/worker 분리
```

WebFlux는 이 문제 중 “I/O 대기 때문에 thread가 낭비되는 상황”에 효과적이다.

## 2. Database Bottleneck

DB가 요청을 처리하지 못해 애플리케이션 전체가 느려지는 상황이다. 가장 흔한 병목 중 하나다.

원인:

```text
느린 SQL
인덱스 부재
N+1 문제
과도한 join
커넥션 풀 부족
락 경합
대량 조회
```

증상:

```text
DB CPU 상승
slow query 증가
connection timeout
HikariCP connection pool exhausted
특정 조회/저장 API만 느림
```

확인할 것:

```text
slow query log
EXPLAIN
DB connection pool metrics
JPA query log
락 대기 시간
```

해결책:

```text
인덱스 추가
쿼리 튜닝
N+1 제거
pagination 적용
커넥션 풀 조정
read replica
Redis cache
```

주의:

```text
JPA/JDBC는 blocking이다.
DB가 병목인데 WebFlux만 도입해도 근본 해결이 안 될 수 있다.
```

## 3. External API Latency

외부 API 응답이 느려져서 우리 API도 같이 느려지는 상황이다.

예:

```text
결제 API
지도 API
인증 API
알림 API
AI API
```

증상:

```text
특정 외부 연동 API의 p95/p99 증가
외부 장애가 우리 장애로 전파
요청 thread 대기 증가
재시도로 트래픽 증폭
```

확인할 것:

```text
외부 API latency
timeout 설정 여부
retry 횟수
HTTP client connection pool
외부 API 에러율
```

해결책:

```text
timeout 설정
retry 제한
circuit breaker
fallback
bulkhead
WebClient
비동기 처리
```

관련 기술:

```text
WebClient
Resilience4j
CircuitBreaker
Retry
Timeout
Bulkhead
```

주의:

```text
무제한 retry는 장애를 키울 수 있다.
retry는 timeout, circuit breaker와 함께 설계해야 한다.
```

## 4. CPU Saturation

CPU가 실제 계산 작업으로 가득 차서 모든 요청 처리가 느려지는 상황이다.

예:

```text
이미지 리사이징
문서 파싱/청킹
암호화
압축
대용량 엑셀 생성
복잡한 계산
```

증상:

```text
CPU 100%
전체 API 응답 지연
요청 수가 많지 않아도 서버가 느림
GC 증가 가능
```

확인할 것:

```text
CPU usage
profiling
thread dump
hot method
GC log
```

해결책:

```text
알고리즘 개선
캐싱
작업 큐 도입
worker 서버 분리
scale-out
batch 처리
```

관련 기술:

```text
Kafka
RabbitMQ
SQS
Redis Queue
Spring Batch
Kubernetes HPA
Profiler
```

주의:

```text
CPU 작업은 WebFlux로 바꿔도 계산 비용이 사라지지 않는다.
```

## 5. Memory and GC Pressure

메모리 사용량이 커지거나 객체 생성이 많아 GC 때문에 지연이 발생하는 상황이다.

원인:

```text
대용량 데이터를 한 번에 로딩
무제한 캐시
큰 response body
메모리 누수
과도한 객체 생성
```

증상:

```text
GC pause 증가
OOM 발생
응답 시간이 주기적으로 튐
서버 재시작
```

확인할 것:

```text
heap usage
GC log
heap dump
object allocation
cache size
```

해결책:

```text
streaming 처리
pagination
cache size 제한
객체 생성 감소
JVM heap 튜닝
메모리 누수 제거
```

## 6. Traffic Spike

짧은 시간에 요청이 폭발적으로 증가하는 상황이다.

예:

```text
이벤트 오픈
푸시 발송 직후
티켓팅
할인 시작
바이럴 유입
```

증상:

```text
갑작스러운 timeout 증가
DB connection 급증
서버 scale-out 전 장애
대기열 증가
```

확인할 것:

```text
RPS 변화
동시 사용자 수
autoscaling 반응 시간
DB connection 수
rate limit 여부
```

해결책:

```text
rate limiting
waiting room
queue
autoscaling
CDN
cache warming
사전 scale-out
```

관련 기술:

```text
Nginx rate limit
Bucket4j
Redis
CloudFront
Cloudflare
Kubernetes HPA
Queue
```

## 7. Retry Storm and Duplicate Requests

실패한 요청을 클라이언트나 서버가 반복 재시도하면서 트래픽이 더 커지는 상황이다.

원인:

```text
짧은 timeout
무제한 retry
여러 계층에서 중복 retry
사용자 중복 클릭
idempotency 없음
```

증상:

```text
실제 사용자 수보다 요청 수가 훨씬 많음
같은 요청이 반복 저장됨
외부 API 호출량 급증
DB write 부하 증가
```

확인할 것:

```text
요청 id
중복 request log
retry policy
client timeout
gateway timeout
```

해결책:

```text
idempotency key
retry 횟수 제한
exponential backoff
jitter
debounce
중복 클릭 방지
```

## 8. Long-Running Jobs

요청 하나가 몇 초에서 몇 분 이상 걸리는 작업이다.

예:

```text
문서 업로드 후 청킹
리포트 생성
엑셀 다운로드
AI 분석
영상 처리
```

문제:

```text
HTTP 요청을 오래 붙잡음
서버 thread 또는 connection 장시간 점유
timeout 가능성 증가
실패/재시도/진행 상태 관리 어려움
```

권장 구조:

```text
POST /jobs
→ 202 Accepted + jobId

GET /jobs/{jobId}
→ 상태 조회

SSE/WebSocket
→ 완료 알림 또는 진행률 전송
```

관련 기술:

```text
Message Queue
Worker
Spring Batch
SSE
WebSocket
Polling
Redis
DB job table
```

## 9. Lock Contention

여러 요청이 같은 자원에 동시에 접근하면서 락 대기 시간이 길어지는 상황이다.

예:

```text
재고 차감
포인트 사용
쿠폰 발급
동일 row update
분산락 과다 사용
```

증상:

```text
특정 쓰기 API만 느림
DB lock wait 증가
deadlock 발생
TPS 저하
```

확인할 것:

```text
DB lock wait
deadlock log
transaction duration
update 대상 row 집중도
```

해결책:

```text
transaction 범위 축소
낙관적 락
큐 기반 직렬화
샤딩/파티셔닝
원자적 update
재시도 정책
```

## 10. Connection Pool Exhaustion

DB나 HTTP client의 connection pool이 모두 사용 중이라 새 요청이 대기하는 상황이다.

원인:

```text
느린 쿼리
느린 외부 API
connection leak
pool size 부족
timeout 미설정
```

증상:

```text
connection acquire timeout
요청 지연
pool active count가 max에 가까움
```

확인할 것:

```text
HikariCP metrics
WebClient/Reactor Netty connection metrics
DB max connections
connection timeout
```

해결책:

```text
pool size 조정
쿼리/외부 API 지연 개선
timeout 설정
connection leak 제거
backpressure/rate limit
```

## 장애 분석 순서

트래픽 장애가 나면 아래 순서로 본다.

```text
1. CPU가 높은가?
2. 메모리/GC 문제가 있는가?
3. DB slow query나 connection pool 문제가 있는가?
4. server thread pool이 고갈됐는가?
5. 외부 API latency가 증가했는가?
6. retry가 트래픽을 증폭시키고 있는가?
7. 특정 API만 느린가, 전체가 느린가?
8. 순간 폭증인가, 지속 부하인가?
```

## WebFlux의 위치

WebFlux는 주로 아래 문제에 도움이 된다.

```text
I/O 대기 많은 요청
server thread 고갈
외부 API 호출 많은 서버
SSE/streaming
고동시성 pending 요청
```

WebFlux가 1차 해결책이 아닌 경우:

```text
느린 SQL
DB index 문제
CPU 100%
메모리 누수
락 경합
무제한 retry
```

정리:

```text
WebFlux는 트래픽 문제 전체의 답이 아니라,
I/O 대기와 thread 효율 문제를 해결하는 선택지 중 하나다.
```

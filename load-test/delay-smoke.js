 import http from 'k6/http';
  import { check, sleep } from 'k6';

  export const options = {
    vus: 1,
    duration: '10s',
  };

  export default function () {
    const res = http.get('http://localhost:8080/delay?ms=100');

    check(res, {
      'status is 200': (r) => r.status === 200,
      'has requestedDelayMs': (r) => r.json('requestedDelayMs') === 100,
      'has actualDelayMs': (r) => typeof r.json('actualDelayMs') === 'number',
      'has threadName': (r) => typeof r.json('threadName') === 'string',
    });

    sleep(1);
  }

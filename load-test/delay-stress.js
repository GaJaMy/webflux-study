 import http from 'k6/http';
  import { check } from 'k6';

  export const options = {
    vus: 50,
    duration: '30s',
    thresholds: {
      http_req_failed: ['rate<0.01'],
      http_req_duration: ['p(95)<1500'],
    },
  };

  export default function () {
    const res = http.get('http://localhost:8080/delay?ms=1000');

    check(res, {
      'status is 200': (r) => r.status === 200,
      'requestedDelayMs is 1000': (r) => r.json('requestedDelayMs') === 1000,
      'actualDelayMs exists': (r) => typeof r.json('actualDelayMs') === 'number',
      'threadName exists': (r) => typeof r.json('threadName') === 'string',
    });
  }

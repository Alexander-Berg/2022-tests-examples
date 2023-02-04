import type { Server } from 'http';

import fetch from 'node-fetch';
import { Gauge } from 'prom-client';

import { createServer } from './index';

let server: Server;
beforeAll(async() => {
    server = createServerWithoutCluster({ listen: 12345 });
    await new Promise((resolve) => setTimeout(resolve, 1000));
});

afterAll(() => {
    server.close();
});

describe('cluster=false', () => {
    describe('/metrics', () => {
        it('should response with 200', async() => {
            return fetch(`http://localhost:12345/metrics`)
                .then((response) => {
                    expect(response.status).toBe(200);
                });
        });

        it('should response with content-type: text/plain', async() => {
            return fetch(`http://localhost:12345/metrics`)
                .then((response) => {
                    expect(response.headers.get('content-type')).toBe('text/plain; version=0.0.4; charset=utf-8');
                });
        });
    });

    describe('/ping', () => {
        it('should response with 200', async() => {
            return fetch(`http://localhost:12345/ping`)
                .then((response) => {
                    expect(response.status).toBe(200);
                });
        });
    });

    describe('/foo', () => {
        it('should response with 404', async() => {
            return fetch(`http://localhost:12345/foo`)
                .then((response) => {
                    expect(response.status).toBe(404);
                });
        });
    });
});

function createServerWithoutCluster({ listen }: { listen: number }) {
    const server = createServer({ cluster: false, listen });

    const testGauge = new Gauge({
        name: 'test',
        help: 'test metric',
    });
    testGauge.set(1);

    return server;
}

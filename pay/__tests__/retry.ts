/* eslint-disable @typescript-eslint/no-explicit-any,@typescript-eslint/ban-ts-ignore */

// @ts-ignore
import { Response } from 'whatwg-fetch';

import { FetchHttpError, FetchTimeoutError } from '../../lib/errors';
import retryDecorator from '../retry';

const wait = (ms: number, value: any): Promise<void> =>
    new Promise((resolve) => setTimeout(() => resolve(value), ms));

const response = (status = 200, headers = {}): any => new Response('', { status, headers });

describe('Decorator Retry', () => {
    describe('Retry Delays', () => {
        test('check default delay', async () => {
            let prevTimer = Date.now();
            let timers: number[] = [];

            const baseFetch = jest.fn((): any => {
                timers.push(Date.now() - prevTimer);
                prevTimer = Date.now();

                return Promise.reject(new FetchHttpError(response(400)));
            });

            const retry = { limit: 2 };
            const fetch = retryDecorator(baseFetch);

            try {
                await fetch('url', { retry });
            } catch (err) {
                timers = timers.map((t) => Math.round(t * 0.01) * 100);
                expect(baseFetch).toBeCalledTimes(3);
                expect(timers).toEqual([0, 500, 1000]);
            }
        });

        test('check custom delay', async () => {
            let prevTimer = Date.now();
            let timers: number[] = [];

            const baseFetch = jest.fn((): any => {
                timers.push(Date.now() - prevTimer);
                prevTimer = Date.now();

                return Promise.reject(new FetchHttpError(response(400)));
            });

            const retry = { limit: 3, delay: (): number => 10 };
            const fetch = retryDecorator(baseFetch);

            try {
                await fetch('url', { retry });
            } catch (err) {
                timers = timers.map((t) => Math.round(t / 10) * 10);
                expect(baseFetch).toBeCalledTimes(4);
                expect(timers).toEqual([0, 10, 10, 10]);
            }
        });

        test('check disable retry by delay', async () => {
            const baseFetch = jest.fn().mockRejectedValue(new FetchHttpError(response(400)));

            const retry = { limit: 3, delay: (): number => -1 };
            const fetch = retryDecorator(baseFetch);

            try {
                await fetch('url', { retry });
            } catch (err) {
                expect(baseFetch).toBeCalledTimes(1);
            }
        });
    });

    describe('Retry by HttpError', () => {
        const delay = (): number => 10;

        test('check retry limit', async () => {
            const baseFetch = jest
                .fn()
                .mockRejectedValue(new FetchHttpError(response(400, { method: 'POST' })));

            const retry = { limit: 5, delay };
            const fetch = retryDecorator(baseFetch);

            try {
                await fetch('url', { retry });
            } catch (err) {
                expect(baseFetch).toBeCalledTimes(6);
                expect(err).toBeInstanceOf(FetchHttpError);
            }
        });

        test('skip retry by status list', async () => {
            const baseFetch = jest.fn().mockRejectedValue(new FetchHttpError(response(400)));

            const retry = { limit: 1, statuses: [500], delay };
            const fetch = retryDecorator(baseFetch);

            try {
                await fetch('url', { retry });
            } catch (err) {
                expect(baseFetch).toBeCalledTimes(1);
                expect(err).toBeInstanceOf(FetchHttpError);
            }
        });

        test('skip retry by status fn', async () => {
            const baseFetch = jest.fn().mockRejectedValue(new FetchHttpError(response(400)));

            const retry = { limit: 1, statuses: (s: number) => s !== 200, delay };
            const fetch = retryDecorator(baseFetch);

            try {
                await fetch('url', { retry });
            } catch (err) {
                expect(baseFetch).toBeCalledTimes(1);
                expect(err).toBeInstanceOf(FetchHttpError);
            }
        });

        test('skip retry by method', async () => {
            const baseFetch = jest
                .fn()
                .mockRejectedValue(new FetchHttpError(response(400, { method: 'POST' })));

            const retry = { limit: 1, methods: ['GET'], delay };
            const fetch = retryDecorator(baseFetch);

            try {
                await fetch('url', { retry });
            } catch (err) {
                expect(baseFetch).toBeCalledTimes(1);
                expect(err).toBeInstanceOf(FetchHttpError);
            }
        });
    });

    describe('Retry by Timeout', () => {
        const delay = (): number => 10;

        test('successful retry', async () => {
            const [res1, res2] = [response(), response()];

            const baseFetch = jest
                .fn()
                .mockReturnValueOnce(wait(300, res1))
                .mockReturnValueOnce(wait(10, res2));

            const retry = { limit: 1, maxTimeout: 50, delay };
            const fetch = retryDecorator(baseFetch);
            const res = await fetch('url', { retry });

            expect(baseFetch).toBeCalledTimes(2);
            expect(res).toBe(res2);
        });

        test('failure retry', async () => {
            const [res1, res2] = [response(), response()];
            const baseFetch = jest
                .fn()
                .mockReturnValue(wait(100, res1))
                .mockReturnValue(wait(100, res2));

            const retry = { limit: 1, maxTimeout: 10, delay };
            const fetch = retryDecorator(baseFetch);

            try {
                await fetch('url', { retry });
            } catch (err) {
                expect(baseFetch).toBeCalledTimes(2);
                expect(err).toBeInstanceOf(FetchTimeoutError);
            }
        });
    });
});

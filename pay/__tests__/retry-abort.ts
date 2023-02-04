/* eslint-disable @typescript-eslint/no-explicit-any,@typescript-eslint/ban-ts-ignore */

import mock, { delay } from 'xhr-mock';

import request from '../../lib/request';
import retryDecorator from '../retry';

describe('Decorator Retry', () => {
    beforeEach(() => mock.setup());

    afterEach(() => mock.teardown());

    describe('Abort requests', () => {
        test('abort first request', async () => {
            const controller = new AbortController();
            const baseFetch = jest.fn(request);

            mock.get('/url', delay({ status: 200 }, 1000));

            const retry = { limit: 5 };
            const fetch = retryDecorator(baseFetch);

            setTimeout(() => controller.abort(), 50);

            try {
                await fetch('/url', { retry, signal: controller.signal });
            } catch (err) {
                expect(err).toBeInstanceOf(DOMException);
                expect(err.name).toBe('AbortError');
                expect(baseFetch).toBeCalledTimes(1);
            }
        });

        /**
         * Мы должны прервать второй запрос.
         * Перый прерывается по таймауту 100ms,
         * затем 0 задержка и шлется второй
         * Через 150ms мы должны быть в "середине" второго запроса.
         */
        test('abort retried request', async () => {
            const controller = new AbortController();
            const baseFetch = jest.fn(request);

            mock.get('/url', delay({ status: 200 }, 1000));

            const retry = { limit: 5, maxTimeout: 100, delay: (): number => 0 };
            const fetch = retryDecorator(baseFetch);

            setTimeout(() => controller.abort(), 150);

            try {
                await fetch('/url', { retry, signal: controller.signal });
            } catch (err) {
                expect(err).toBeInstanceOf(DOMException);
                expect(err.name).toBe('AbortError');
                expect(baseFetch).toBeCalledTimes(2);
            }
        });
    });
});

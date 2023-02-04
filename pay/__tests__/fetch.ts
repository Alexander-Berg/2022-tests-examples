/* eslint-disable @typescript-eslint/no-explicit-any,@typescript-eslint/ban-ts-ignore */

import mock from 'xhr-mock';

import fetch from '../fetch';
import { FetchHttpError } from '../lib/errors';

describe('Fetch', () => {
    beforeEach(() => {
        mock.setup();
        // Убирает ошибки из консоли
        mock.error(() => {});
    });

    afterEach(() => mock.teardown());

    test('successful', async () => {
        mock.get('/url', { status: 200 });

        const res = await fetch('/url');

        expect(res.status).toBe(200);
    });

    test('failure by status', async () => {
        mock.get('/url', { status: 400 });

        try {
            await fetch('/url');
        } catch (err) {
            expect(err).toBeInstanceOf(FetchHttpError);
        }
    });

    test('failure by network', async () => {
        mock.get('/url', () => Promise.reject(new Error('Error message')));

        try {
            await fetch('/url');
        } catch (err) {
            expect(err).toBeInstanceOf(FetchHttpError);
        }
    });

    test('retry fetch', async () => {
        const responseStatus = jest.fn().mockReturnValueOnce(500).mockReturnValue(200);

        mock.get('/url', (req, res) => res.status(responseStatus()));

        const retry = { limit: 1, delay: (): number => 10 };

        const res = await fetch('/url', { retry });

        expect(res.status).toBe(200);
        expect(responseStatus).toBeCalledTimes(2);
    });
});

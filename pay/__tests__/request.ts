import mock from 'xhr-mock';

import request from '../request';

describe('Request', () => {
    beforeEach(() => {
        mock.setup();
        // Убирает ошибки из консоли
        mock.error(() => {});
    });

    afterEach(() => mock.teardown());

    test('successful', async () => {
        mock.get('/url', { status: 200 });

        const res = await request('/url');

        expect(res.status).toBe(200);
    });

    test('failure', async () => {
        mock.error(() => null);

        mock.get('/url', () => Promise.reject(new Error('Error message')));

        const res = await request('/url');

        expect(res).toMatchObject({
            ok: false,
            status: -1,
            statusText: 'TypeError: Network request failed',
        });
    });

    test('send search params', async () => {
        mock.get('/url?keyA=dataA&keyB=dataB', { status: 200 });

        const res = await request('/url', { searchParams: { keyA: 'dataA', keyB: 'dataB' } });

        expect(res.status).toBe(200);
    });

    test('send application/x-www-form-urlencode body', async () => {
        let xhrReq: any;

        mock.post(
            '/url',
            jest.fn((req, res) => {
                xhrReq = req;

                return res.status(200);
            }),
        );

        await request('/url', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencode' },
            body: { keyA: 'dataA', keyB: 'dataB' },
        });

        expect(xhrReq.body()).toBe('keyA=dataA&keyB=dataB');
    });
});

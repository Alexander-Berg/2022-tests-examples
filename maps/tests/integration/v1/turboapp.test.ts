import * as http from 'http';
import got from 'got';
import {app} from 'app/app';
import {URLSearchParams} from 'url';
import {startServer, stopServer} from 'tests/integration/test-server';
import {intHostConfigLoader} from 'app/lib/host-loader';
import * as nock from 'nock';

const client = got.extend({
    throwHttpErrors: false,
    retry: 0,
    timeout: 2000,
    responseType: 'json',
    headers: {Referer: 'https://taxi.tap.yandex.ru/somepage'}
});

describe('/v1/turboapp/meta', () => {
    let server: http.Server;
    let url: string;

    beforeAll(async () => {
        nock.disableNetConnect();
        nock.enableNetConnect(/localhost/);

        [server, url] = await startServer(app);
        const {meta} = await intHostConfigLoader.get();

        // libxmljs not working properly under jest environment
        // so there is no reason to stub server answer
        // https://github.com/facebook/jest/issues/8636
        nock(meta)
            .get('/layers?action=info&l=map&lang=ru_RU')
            .reply(200);
    });

    afterAll(async () => {
        await stopServer(server);
        nock.cleanAll();
        nock.enableNetConnect();
    });

    it('should return 200 for valid request', async () => {
        const response = await client.get(`${url}/v1/turboapp/meta`, {
            searchParams: new URLSearchParams({
                lang: 'ru_RU'
            })
        });

        expect(response.headers['access-control-allow-origin']).toEqual('https://taxi.tap.yandex.ru');
        expect(response.headers.vary).toEqual('origin');
        expect(response.statusCode).toEqual(200);
    });

    it('should return 400 for wrong referer', async () => {
        const response = await client.get(`${url}/v1/turboapp/meta`, {
            searchParams: new URLSearchParams({
                lang: 'ru_RU'
            }),
            headers: {Referer: 'https://somesite.ru'}
        });

        expect(response.statusCode).toEqual(400);
    });

    it('should return 400 for empty referer', async () => {
        const response = await client.get(`${url}/v1/turboapp/meta`, {
            searchParams: new URLSearchParams({
                lang: 'ru_RU'
            }),
            headers: {Referer: undefined}
        });

        expect(response.statusCode).toEqual(400);
    });

    it('should return 400 if lang is not provided', async () => {
        const response = await client.get(`${url}/v1/turboapp/meta`);
        expect(response.statusCode).toEqual(400);
    });

    it('should return 400 if lang is invalid', async () => {
        const response = await client.get(`${url}/v1/turboapp/meta`, {
            searchParams: new URLSearchParams({lang: 'wrong'})
        });

        expect(response.statusCode).toEqual(400);
    });
});

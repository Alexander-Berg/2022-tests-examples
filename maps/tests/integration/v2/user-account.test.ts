import * as http from 'http';
import * as nock from 'nock';
import * as got from 'got';
import {app} from 'app/app';
import {startServer, stopServer} from 'tests/integration/test-server';
import {intHostConfigLoader} from 'app/lib/host-loader';
import {config} from 'app/config';

const defaultHeaders = {
    Authorization: 'OAuth xxxxx'
};

const blackboxValidResponse = {
    error: 'OK',
    status: {
        value: 'VALID',
        uid: '9999'
    },
    uid: {
        value: '9999'
    }
};

const invalidBlackboxReponse = {
    ...blackboxValidResponse,
    status: {
        value: 'INVALID'
    }
};

const defaultBody = {
    meta: {}
};

const URL_PREFIX = '/v2/user_account';

const client = got.default.extend({
    throwHttpErrors: false,
    retry: 0,
    timeout: 2000,
    responseType: 'json'
});

describe(URL_PREFIX, () => {
    let server: http.Server;
    let url: string;
    let userAccountHost: string;
    let blackboxService: nock.Interceptor;

    beforeAll(async () => {
        [server, url] = await startServer(app);
        nock.disableNetConnect();
        nock.enableNetConnect(/localhost/);

        blackboxService = nock(config['blackbox.host'])
            .filteringPath(() => '/blackbox')
            .get('/blackbox')
            .query(true);

        const hosts = await intHostConfigLoader.get();
        userAccountHost = hosts.userAccountInt;
    });

    afterAll(async () => {
        await stopServer(server);
        nock.enableNetConnect();
    });

    describe('GET routes with Authorization', () => {
        const routes = [
            '/impressions'
        ];

        it('should return error if backend is unavailable', async () => {
            for (let i = 0; i < routes.length; i++) {
                blackboxService.reply(200, blackboxValidResponse);
                nock(userAccountHost).get(`/v2${routes[i]}`).query(true).reply(403);

                const response = await client.get(`${url}${URL_PREFIX}${routes[i]}`, {
                    headers: defaultHeaders
                });
                expect(response.statusCode).toEqual(403);
            }
        });

        it('should return error if blackbox is unavailable', async () => {
            for (let i = 0; i < routes.length; i++) {
                blackboxService.reply(500);

                const response = await client.get(`${url}${URL_PREFIX}${routes[i]}`, {
                    headers: defaultHeaders
                });
                expect(response.statusCode).toEqual(401);
            }
        });

        it('should return error if user hasn\'t authorization', async () => {
            for (let i = 0; i < routes.length; i++) {
                blackboxService.reply(200, invalidBlackboxReponse);

                const response = await client.get(`${url}${URL_PREFIX}${routes[i]}`, {
                    headers: defaultHeaders
                });
                expect(response.statusCode).toEqual(401);
            }
        });

        it('should return error if "Authorization" header wasn\'t recivied', async () => {
            for (let i = 0; i < routes.length; i++) {
                blackboxService.reply(200, blackboxValidResponse);

                const response = await client.get(`${url}${URL_PREFIX}${routes[i]}`);
                expect(response.statusCode).toEqual(401);
            }
        });

        it('should return success', async () => {
            for (let i = 0; i < routes.length; i++) {
                blackboxService.reply(200, blackboxValidResponse);
                nock(userAccountHost).get(`/v2${routes[i]}`).query(true).reply(200);

                const response = await client.get(`${url}${URL_PREFIX}${routes[i]}`, {
                    headers: defaultHeaders
                });
                expect(response.statusCode).toEqual(200);
            }
        });
    });

    describe('POST routes with Authorization', () => {
        const routes = [
            '/impressions/skip_poll',
            '/impressions/answer_simple_question',
            '/impressions/answer_sbs_question'
        ];

        it('should return error if backend is unavailable', async () => {
            for (let i = 0; i < routes.length; i++) {
                blackboxService.reply(200, blackboxValidResponse);
                nock(userAccountHost).post(`/v2${routes[i]}`).reply(403);

                const response = await client.post(`${url}${URL_PREFIX}${routes[i]}`, {
                    headers: defaultHeaders,
                    json: defaultBody
                });
                expect(response.statusCode).toEqual(403);
            }
        });

        it('should return error if blackbox is unavailable', async () => {
            for (let i = 0; i < routes.length; i++) {
                blackboxService.reply(500);

                const response = await client.post(`${url}${URL_PREFIX}${routes[i]}`, {
                    headers: defaultHeaders,
                    json: defaultBody
                });
                expect(response.statusCode).toEqual(401);
            }
        });

        it('should return error if user hasn\'t authorization', async () => {
            for (let i = 0; i < routes.length; i++) {
                blackboxService.reply(200, invalidBlackboxReponse);

                const response = await client.post(`${url}${URL_PREFIX}${routes[i]}`, {
                    headers: defaultHeaders,
                    json: defaultBody
                });
                expect(response.statusCode).toEqual(401);
            }
        });

        it('should return error if "Authorization" header wasn\'t recivied', async () => {
            for (let i = 0; i < routes.length; i++) {
                blackboxService.reply(200, blackboxValidResponse);

                const response = await client.post(`${url}${URL_PREFIX}${routes[i]}`, {
                    json: defaultBody
                });
                expect(response.statusCode).toEqual(401);
            }
        });

        it('should return success', async () => {
            for (let i = 0; i < routes.length; i++) {
                blackboxService.reply(200, blackboxValidResponse);
                nock(userAccountHost).post(`/v2${routes[i]}`).reply(200);

                const response = await client.post(`${url}${URL_PREFIX}${routes[i]}`, {
                    headers: defaultHeaders,
                    json: defaultBody
                });
                expect(response.statusCode).toEqual(200);
            }
        });
    });
});

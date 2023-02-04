import * as http from 'http';
import got from 'got';
import {expect} from 'chai';
import * as nock from 'nock';
import {URL} from 'url';
import {app} from 'app/app';
import {startServer, stopServer} from 'tests/integration/test-server';
import {intHostConfigLoader} from 'app/middlewares/hosts';
import {metaResponse} from 'tests/fixtures/meta';
import {initScriptResponse} from 'tests/fixtures/initScript';
import {supportedLangs} from 'app/lib/lang';
import {TvmDaemon} from 'tests/utils/tvm-daemon';
import {config} from 'tests/config';

const client = got.extend({
    throwHttpErrors: false,
    retry: 0,
    timeout: 2000,
    responseType: 'json',
    headers: {
        Referer: 'https://yandex.ru',
        'X-Forwarded-For-Y': 'another_ip',
        'User-Agent': 'test'
    }
});

const requestedLangs = [
    // Lang requests from July 2021. https://yql.yandex-team.ru/Operations/YQpRixJKfaEJa9de5qyp7wAyfwQQSQYQ1F1iehkJROg=
    'ru_RU',
    'ru_BY',
    'ru_UA',
    'ru_KZ',
    'en_RU',
    'tr_TR',
    'en_UA',
    'uz_UZ',
    'uk_UA',
    'en_US',
    'be_BY',
    'en_BY',
    'uk_RU',
    'kk_KZ',
    'kk_RU',
    'be_RU',
    'en_TR',
    'uz_RU',

    // Langs for cartograph: https://st.yandex-team.ru/MAPSAPI-15064#61534446a71b6208e832e8b1
    'he_IL',
    'mul_UA',
    'en_IL',
    'mul-Latn_UA',
    'mul-Latn_TR'
];

describe('/v1/startup', () => {
    let server: http.Server;
    let url: string;

    let tvmDaemon: TvmDaemon;

    before(async () => {
        [server, url] = await startServer(app);
        tvmDaemon = await TvmDaemon.start();

        nock.disableNetConnect();
        nock.enableNetConnect(/(127.0.0.1|localhost)/);

        const {meta, apikeysInt} = await intHostConfigLoader.get();

        nock(meta)
            .get('/layers?action=info&l=map')
            .reply(200, metaResponse);

        nock('https://maps-front-static-int.s3.mds.yandex.net')
            .get('/ymaps3-react/static/bundles/init.template.js')
            .reply(200, initScriptResponse);

        nock(apikeysInt)
            .persist(true)
            .post('/v1/check')
            .reply(200, (_, body: {key: string, useWhitelist: boolean, referrer?: string}) => {
                if (body.useWhitelist) {
                    let origin: string;
                    try {
                        origin = new URL(body.referrer ?? '').origin;
                    } catch {
                        return {ok: false, message: 'Icorrect key'};
                    }

                    // See whitelist in apikeys-int.
                    if (/yandex-team\.\w+|yandex\.ru|yango\.com|meteum\.ai$/.test(origin)) {
                        return {ok: true};
                    }
                } else if (body.key === config.apikey && body.referrer) {
                    return {ok: true};
                }

                return {ok: false, message: 'Incorrect key'};
            });
    });

    after(async () => {
        await stopServer(server);
        await tvmDaemon.stop();
        nock.cleanAll();
        nock.enableNetConnect();
    });

    it('should return data if everything is alright', async () => {
        const response = await client.get(`${url}/v1/startup?lang=ru_RU`);
        expect(response.statusCode).to.equal(200);
        expect(response.headers['access-control-allow-origin']).to.equal('https://yandex.ru');
        expect(response.headers.vary).to.equal('origin');
    });

    it('should return data if referer is meteum.ai', async () => {
        const hostnames = ['meteum.ai', 'l7test.meteum.ai', 'weather-chams.l7test.meteum.ai'];
        for (const hostname of hostnames) {
            const response = await client.get(`${url}/v1/startup?lang=ru_RU`, {
                headers: {Referer: `https://${hostname}/somepage`}
            });
            expect(response.statusCode).to.equal(200);
            expect(response.headers['access-control-allow-origin']).to.equal(`https://${hostname}`);
            expect(response.headers.vary).to.equal('origin');
        }
    });

    it('should return data if referer is yango.com', async () => {
        const hostnames = ['yango.com', 'somedomen.yango.com'];
        for (const hostname of hostnames) {
            const response = await client.get(`${url}/v1/startup?lang=ru_RU`, {
                headers: {Referer: `https://${hostname}/somepage`}
            });
            expect(response.statusCode).to.equal(200);
            expect(response.headers['access-control-allow-origin']).to.equal(`https://${hostname}`);
            expect(response.headers.vary).to.equal('origin');
        }
    });

    it('should return data if referer is *.yandex-team.ru', async () => {
        const hostnames = [
            'eagle.lavka.dev.yandex-team.ru',
            'yandex-team.ru',
            'wiki.yandex-team.ru'
        ];
        for (const hostname of hostnames) {
            const response = await client.get(`${url}/v1/startup?lang=ru_RU`, {
                headers: {Referer: `https://${hostname}/somepage`}
            });
            expect(response.statusCode).to.equal(200);
            expect(response.headers['access-control-allow-origin']).to.equal(`https://${hostname}`);
            expect(response.headers.vary).to.equal('origin');
        }
    });

    it('should return data if ip from yandex net and wrong referer', async () => {
        const response = await client.get(`${url}/v1/startup?lang=ru_RU`, {
            headers: {
                Referer: 'https://somesite.ru',
                // see getIpTraits from src/app/lib/geobase.ts
                'X-Forwarded-For-Y' : 'yandex_ip'
            }
        });
        expect(response.statusCode).to.equal(200);
        expect(response.headers['access-control-allow-origin']).to.equal('https://somesite.ru');
        expect(response.headers.vary).to.equal('origin');
    });

    it('should return 400 for wrong referer', async () => {
        const response = await client.get(`${url}/v1/startup?lang=ru_RU`, {
            headers: {Referer: 'https://somesite.ru'}
        });

        expect(response.statusCode).to.equal(400);
    });

    it('should return 400 for empty referer', async () => {
        const response = await client.get(`${url}/v1/startup?lang=ru_RU`, {
            headers: {Referer: undefined}
        });
        expect(response.statusCode).to.equal(400);
    });

    it('should return 400 if lang is not provided', async () => {
        const response = await client.get(`${url}/v1/startup`);
        expect(response.statusCode).to.equal(400);
    });

    it('should return 400 if lang is invalid', async () => {
        const response = await client.get(`${url}/v1/startup?lang=wrong`);
        expect(response.statusCode).to.equal(400);
    });

    it('should return data if lang is supported', async () => {
        await Promise.all(Object.keys(supportedLangs).map(async (lang) => {
            const response = await client.get(`${url}/v1/startup?lang=${lang}`);
            expect(response.statusCode).to.equal(200);
        }));
    });

    it('should return data if lang is unsupported', async () => {
        const response = await client.get(`${url}/v1/startup?lang=xx_XX`);
        expect(response.statusCode).to.equal(200);
    });

    it('should return data on requested lang', async () => {
        await Promise.all(requestedLangs.map(async (lang) => {
            const response = await client.get(`${url}/v1/startup?lang=${lang}`);
            expect(response.statusCode).to.equal(200);
        }));
    });

    describe('/3.0/', () => {
        describe('apikey', async () => {
            describe('Wrong format', async () => {
                it('should return error', async () => {
                    const response = await client.get<{message: string}>(`${url}/3.0/?lang=ru_RU`);
                    expect(response.statusCode).to.equal(400);
                    expect(response.body.message).contain('"apikey" is required');
                });
            });

            describe('Wrong key', async () => {
                it('should return error', async () => {
                    const response = await client.get<{message: string}>(
                        `${url}/3.0/?lang=ru_RU&apikey=wrong-key`
                    );
                    expect(response.statusCode).to.equal(400);
                    expect(response.body.message).contain('Incorrect key');
                });
            });

            describe('Without referer', async () => {
                it('should return error', async () => {
                    const response = await client.get<{message: string}>(
                        `${url}/3.0/?lang=ru_RU&apikey=${config.apikey}`,
                        {headers: {Referer: ''}}
                    );

                    expect(response.statusCode).to.equal(400);
                    expect(response.body.message).to.be.equal('Incorrect key');
                });
            });

            it('should return javascript code', async () => {
                const response = await client.get(
                    `${url}/3.0/?lang=ru_RU&apikey=${config.apikey}`,
                    {headers: {Referer: 'https://yandex.ru'}, responseType: 'text'}
                );

                expect(response.statusCode).to.equal(200);
                expect(response.headers['content-type']).to.equal('application/javascript; charset=utf-8');
                expect(response.body).satisfies((x: string) => x.startsWith('!function(') && !x.includes('__CFG__'));
            });
        });
    });
});

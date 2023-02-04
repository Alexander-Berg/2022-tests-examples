import assert from 'assert';
import got, {Got} from 'got';
import nock from 'nock';
import {app} from '../../app/app';
import {config} from '../../app/config';
import {TestServer} from './lib/test-server';
import {TvmDaemon} from './lib/tvm-daemon';

const LOCALHOST = /(127.0.0.1|localhost)/;
const TVMDAEMON_URL = 'http://localhost:8881/';
const BLACKBOX_URL = 'http://blackbox/blackbox/';
const PERSONALIZED_POI_RENDERER_URL = 'http://personalized-poi/';

const HOST_CONFIG = {
    'host_config[inthosts][blackbox]': BLACKBOX_URL,
    'host_config[inthosts][personalizedPoi]': PERSONALIZED_POI_RENDERER_URL
};

let baseHttpClient: Got;

interface NockArgs {
    delay?: number;
    url?: string;
    query?: boolean | Record<string, string>;
    requestHeaders?: Record<string, string>;
    responseHeaders?: Record<string, string>;
    responseBody?: string | object;
    responseStatus?: number;
}

function nockBlackbox({delay, query, responseBody, responseStatus}: NockArgs = {}) {
    return nock(BLACKBOX_URL)
        .get('/')
        .query(query ?? true)
        .delay(delay ?? 0)
        .reply(responseStatus ?? 200, responseBody ?? {status: {value: 'VALID'}});
}

function nockPersonalizedPoiRenderer({
    delay,
    url,
    query,
    requestHeaders,
    responseHeaders,
    responseBody,
    responseStatus
}: NockArgs = {}) {
    return nock(PERSONALIZED_POI_RENDERER_URL, {reqheaders: requestHeaders})
        .get(`/${url ?? ''}`)
        .query(query ?? true)
        .delay(delay ?? 0)
        .reply(responseStatus ?? 200, responseBody ?? 'OK', responseHeaders ?? {});
}

function testPersonalizedPoiProxy(args: {
    url: string;
    query?: Record<string, string>;
    mockBlackbox?: boolean;
}) {
    describe('Personalized POI proxy middleware', () => {
        let request: Got;

        before(() => {
            request = baseHttpClient.extend({
                url: args.url,
                searchParams: {
                    ...HOST_CONFIG,
                    ...args.query
                }
            });
        });

        beforeEach(() => {
            if (args.mockBlackbox) {
                nockBlackbox().persist();
            }
        });

        it('should return 500 if backend timed out', async () => {
            const personalizedPoiScope = nockPersonalizedPoiRenderer({
                url: args.url,
                delay: config['http.defaultTimeoutMilliseconds']
            });

            const response = await request({});

            assert.strictEqual(response.statusCode, 500);
            assert.strictEqual(personalizedPoiScope.isDone(), true);
        });

        it('should proxy backend error response to client', async () => {
            const personalizedPoiScope = nockPersonalizedPoiRenderer({
                url: args.url,
                responseStatus: 444,
                responseBody: 'error message'
            });

            const response = await request({});

            assert.strictEqual(response.statusCode, 444);
            assert.strictEqual(response.body, 'error message');
            assert.strictEqual(personalizedPoiScope.isDone(), true);
        });

        it('should send correct request to backend', async () => {
            const personalizedPoiScope = nockPersonalizedPoiRenderer({
                url: args.url,
                query: {
                    ...HOST_CONFIG,
                    ...args.query
                },
                requestHeaders: {
                    // Special case:
                    // Requests to Personalized POI renderer's '/tiles' endpoint must have
                    // 'Accept: application/x-protobuf' header, otherwise it will return '406 Not Acceptable' error.
                    // So the expected behaviour here is to send:
                    //   - 'application/x-protobuf' when proxying to '/tiles' endpoint
                    //   - 'Accept' header from the client request when proxying to other endpoints
                    Accept: args.url === 'tiles' ? 'application/x-protobuf' : 'mock/*'
                },
                responseStatus: 200,
                responseBody: 'backend response body'
            });

            const response = await request({
                headers: {Accept: 'mock/*'}
            });

            assert.strictEqual(response.statusCode, 200);
            assert.strictEqual(response.body, 'backend response body');
            assert.strictEqual(personalizedPoiScope.isDone(), true);
        });
    });
}

function testRefererValidation(url: string) {
    describe('Referer validation and CORS headers', () => {
        let request: Got;

        before(() => {
            request = baseHttpClient.extend({
                headers: {Origin: undefined},
                url: url,
                searchParams: HOST_CONFIG
            });
        });

        beforeEach(() => {
            nockBlackbox().persist();

            nockPersonalizedPoiRenderer({
                url: url,

                // Provide 'Access-Control-Allow-Origin: *' header in the backend response,
                // to make sure that we correctly overwrite it in the proxied response.
                responseHeaders: {'Access-Control-Allow-Origin': '*'}
            }).persist();
        });

        it('should return 403 if both Referer and Origin headers are missing', async () => {
            const response = await request({});

            assert.strictEqual(response.statusCode, 403);
        });

        ['Referer', 'Origin'].forEach((header) => describe(`${header} header`, () => {
            it('should allow different Yandex TLDs', async () => {
                for (const tld of ['ru', 'com.tr', 'ua', 'kz', 'by']) {
                    const response = await request({
                        headers: {[header]: `https://yandex.${tld}`}
                    });

                    assert.strictEqual(response.statusCode, 200);
                    assert.strictEqual(response.body, 'OK');
                    assert.strictEqual(response.headers['access-control-allow-origin'], `https://yandex.${tld}`);
                    assert.strictEqual(response.headers['access-control-allow-credentials'], 'true');
                    assert.strictEqual(response.headers.vary, 'Origin');
                }
            });

            it('should allow Yandex subdomains', async () => {
                const response = await request({
                    headers: {[header]: 'https://foo.bar.baz.yandex.com.tr'}
                });

                assert.strictEqual(response.statusCode, 200);
                assert.strictEqual(response.body, 'OK');
                assert.strictEqual(response.headers['access-control-allow-origin'], 'https://foo.bar.baz.yandex.com.tr');
                assert.strictEqual(response.headers['access-control-allow-credentials'], 'true');
                assert.strictEqual(response.headers.vary, 'Origin');
            });

            it('should return 403 if it is not a Yandex domain', async () => {
                const responses = await Promise.all([
                    request({headers: {[header]: 'https://example.com'}}),
                    request({headers: {[header]: 'https://yandex.ru.example.com'}}),
                    request({headers: {[header]: 'https://example.com/https://yandex.ru'}})
                ]);

                assert.deepStrictEqual(responses.map((response) => response.statusCode), [403, 403, 403]);
            });
        }));
    });
}

describe('/personalized_poi', () => {
    let tvmDaemon: TvmDaemon;
    let testServer: TestServer;

    before(async () => {
        tvmDaemon = await TvmDaemon.start();
        testServer = await TestServer.start(app);
        baseHttpClient = got.extend({
            method: 'GET',
            prefixUrl: `${testServer.url}/personalized_poi`,
            headers: {
                Origin: 'https://yandex.ru',
                Cookie: 'Session_id=mock_session_id',
                'X-Real-IP': '::ffff'
            },
            retry: 0,
            throwHttpErrors: false
        });
    });

    after(async () => {
        await testServer.stop();
        await tvmDaemon.stop();
    });

    beforeEach(() => {
        nock.disableNetConnect();
        nock.enableNetConnect(LOCALHOST);
    });

    afterEach(() => {
        nock.cleanAll();
    });

    describe('/tiles', () => {
        let request: Got;

        before(() => {
            request = baseHttpClient.extend({
                url: 'tiles',
                searchParams: HOST_CONFIG
            });
        });

        describe('Blackbox middleware', () => {
            it('should return 500 if TVM daemon is unavailable', async () => {
                const tvmDaemonScope = nock(TVMDAEMON_URL)
                    .filteringPath(() => '/')
                    .get('/')
                    .reply(500);

                const response = await request({});

                assert.strictEqual(response.statusCode, 500);
                assert.strictEqual(tvmDaemonScope.isDone(), true);
            });

            it('should return 500 if blackbox timed out', async () => {
                const blackboxTimeout = config['blackbox.timeoutMilliseconds'];
                const blackboxRetryCount = config['blackbox.retryCount'];
                const blackboxRetryDelay = config['blackbox.retryDelayMilliseconds'];

                // The formula below calculates the total duration of the request before it fails.
                // Assuming blackboxRetryCount = 2, the request should fail after 3 attempts
                // (1 initial + 2 retries). Before each retry there is a constant delay.
                const totalDuration = blackboxTimeout + (blackboxRetryDelay + blackboxTimeout) * blackboxRetryCount;

                const blackboxScope = nockBlackbox({delay: blackboxTimeout}).persist();

                const response = await request({
                    timeout: totalDuration + 50
                });

                assert.strictEqual(response.statusCode, 500);
                assert.strictEqual(blackboxScope.isDone(), true);
            });

            it('should return 500 if blackbox response has non-200 status code', async () => {
                const blackboxScope = nockBlackbox({responseStatus: 500}).persist();

                const response = await request({});

                assert.strictEqual(response.statusCode, 500);
                assert.strictEqual(blackboxScope.isDone(), true);
            });

            it('should return 500 if blackbox response has exception in body', async () => {
                const blackboxScope = nockBlackbox({
                    responseStatus: 200,
                    responseBody: {exception: {value: 'DB_EXCEPTION'}}
                });

                const response = await request({});

                assert.strictEqual(response.statusCode, 500);
                assert.strictEqual(blackboxScope.isDone(), true);
            });

            it('should return 403 if Cookie header is missing', async () => {
                const response = await request({
                    headers: {Cookie: undefined}
                });

                assert.strictEqual(response.statusCode, 403);
            });

            it('should send correct request to blackbox', async () => {
                const blackboxScope = nockBlackbox({
                    query: {
                        method: 'sessionid',
                        sessionid: 'my_session_id',
                        host: 'yandex.ru',
                        userip: '::ffff',
                        get_user_ticket: 'yes', // eslint-disable-line camelcase
                        format: 'json'
                    }
                });
                const personalizedPoiScope = nockPersonalizedPoiRenderer({url: 'tiles'});

                const response = await request({
                    headers: {Cookie: 'Session_id=my_session_id'}
                });

                assert.strictEqual(response.statusCode, 200);
                assert.strictEqual(blackboxScope.isDone(), true);
                assert.strictEqual(personalizedPoiScope.isDone(), true);
            });

            it('should send obtained user ticket to backend', async () => {
                const blackboxScope = nockBlackbox({
                    responseBody: {
                        status: {value: 'VALID'},
                        user_ticket: 'my_user_ticket' // eslint-disable-line camelcase
                    }
                });
                const personalizedPoiScope = nockPersonalizedPoiRenderer({
                    url: 'tiles',
                    requestHeaders: {'X-Ya-User-Ticket': 'my_user_ticket'}
                });

                const response = await request({});

                assert.strictEqual(response.statusCode, 200);
                assert.strictEqual(blackboxScope.isDone(), true);
                assert.strictEqual(personalizedPoiScope.isDone(), true);
            });
        });

        testPersonalizedPoiProxy({
            url: 'tiles',
            query: {
                l: 'personalized_poi',
                lang: 'ru_RU',
                scale: '2',
                x: '158482',
                y: '81973',
                z: '18',
                v: '1'
            },
            mockBlackbox: true
        });

        testRefererValidation('tiles');
    });

    [
        {
            url: 'glyphs',
            query: {
                font_id: '9f08a647bf335ac646f57a03dbb3a12bd8cab4d0', // eslint-disable-line camelcase
                range: '0,128'
            } as Record<string, string>
        },
        {
            url: 'icons',
            query: {
                id: '3WBOU27IRS7G',
                scale: '2.5'
            } as Record<string, string>
        },
        {
            url: 'version'
        }
    ].forEach(({url, query}) => {
        describe(`/${url}`, () => {
            testPersonalizedPoiProxy({
                url: url,
                query: query,
                mockBlackbox: false
            });

            testRefererValidation(url);
        });
    });

    describe('unknown endpoints', () => {
        it('should return 404 for unknown endpoints', async () => {
            const blackboxScope = nockBlackbox();
            const personalizedPoiScope = nockPersonalizedPoiRenderer({url: 'foo'});

            const response = await baseHttpClient({
                url: 'foo',
                searchParams: {
                    ...HOST_CONFIG,
                    bar: 'baz'
                }
            });

            assert.strictEqual(response.statusCode, 404);
            assert.strictEqual(blackboxScope.isDone(), false);
            assert.strictEqual(personalizedPoiScope.isDone(), false);
        });
    });
});

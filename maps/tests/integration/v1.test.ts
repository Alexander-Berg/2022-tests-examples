import assert from 'assert';
import got, {Got} from 'got';
import nock, {Interceptor} from 'nock';
import {app} from '../../app/app';
import {config} from '../../app/config';
import {TestServer} from './lib/test-server';
import {TvmDaemon} from './lib/tvm-daemon';

const LOCALHOST = /(127.0.0.1|localhost)/;
const TVMDAEMON_URL = 'http://localhost:8881';
const APIKEYSINT_URL = 'http://apikeysint';
const MAPTILES_URL = 'http://maptiles';

const DEFAULT_PARAMS = {
    l: 'map',
    x: 1,
    y: 1,
    z: 1,
    lang: 'ru_RU',
    apikey: 123,
    'host_config[inthosts][apikeysInt]': `${APIKEYSINT_URL}/`,
    'host_config[inthosts][mapTiles]': `${MAPTILES_URL}/tiles?l=map&%c&%l`
};

describe('v1', () => {
    let tvmDaemon: TvmDaemon;
    let testServer: TestServer;
    let request: Got;

    before(async () => {
        tvmDaemon = await TvmDaemon.start();
        testServer = await TestServer.start(app);
        request = got.extend({
            method: 'GET',
            prefixUrl: `${testServer.url}/v1`,
            url: 'tiles',
            retry: 0,
            throwHttpErrors: false
        });
    });

    after(async () => {
        await testServer.stop();
        await tvmDaemon.stop();
    });

    let apikeysIntService: Interceptor;
    let mapTilesService: Interceptor;

    beforeEach(() => {
        nock.disableNetConnect();
        nock.enableNetConnect(LOCALHOST);

        apikeysIntService = nock(APIKEYSINT_URL)
            .post('/v1/check');

        mapTilesService = nock(MAPTILES_URL)
            .get('/tiles')
            .query(true);
    });

    afterEach(() => {
        nock.cleanAll();
    });

    describe('with whitelisted referrer', () => {
        it('should proxy backend response without a valid apikey', async () => {
            const apikeysIntServiceScope = nock(APIKEYSINT_URL)
                .post('/v1/check', (body) => body.referrer === 'https://yandex.ru')
                .reply(200, {ok: true});
            const mapTilesScope = mapTilesService.reply(200, 'tile');

            const response = await request({
                headers: {Referer: 'https://yandex.ru'},
                searchParams: {
                    ...DEFAULT_PARAMS,
                    apikey: undefined
                }
            });

            assert.strictEqual(response.statusCode, 200);
            assert.strictEqual(response.body, 'tile');
            assert.strictEqual(mapTilesScope.isDone(), true);
            assert.strictEqual(apikeysIntServiceScope.isDone(), true);
        });
    });

    describe('without whitelisted referrer', () => {
        describe('when apikey is valid', () => {
            it('should proxy backend response', async () => {
                const apikeysIntServiceScope = apikeysIntService.reply(200, {ok: true});
                const mapTilesScope = mapTilesService.reply(200, 'tile');

                const response = await request({searchParams: DEFAULT_PARAMS});

                assert.strictEqual(response.statusCode, 200);
                assert.strictEqual(response.body, 'tile');
                assert.strictEqual(mapTilesScope.isDone(), true);
                assert.strictEqual(apikeysIntServiceScope.isDone(), true);
            });
        });

        describe('when apikey is banned', () => {
            it('should respond with an error returned by apikeysInt', async () => {
                const apikeysIntServiceScope = apikeysIntService.reply(200, {ok: false, statusCode: 403});

                const response = await request({searchParams: DEFAULT_PARAMS});

                assert.strictEqual(response.statusCode, 403);
                assert.strictEqual(apikeysIntServiceScope.isDone(), true);
            });
        });

        describe('when apikey parameter is missed', () => {
            it('should respond with an error returned by apikeysInt', async () => {
                const apikeysIntServiceScope = apikeysIntService.reply(200, {ok: false, statusCode: 403});

                const response = await request({
                    searchParams: {
                        ...DEFAULT_PARAMS,
                        apikey: undefined
                    }
                });

                assert.strictEqual(response.statusCode, 403);
                assert.strictEqual(apikeysIntServiceScope.isDone(), true);
            });
        });
    });

    describe('when required parameters are missing', () => {
        it('should respond with a validation error', async () => {
            const apikeysIntServiceScope = apikeysIntService.reply(200, {ok: true});
            const mapTilesScope = nock(MAPTILES_URL)
                .get('/tiles')
                .query(true)
                .reply(200, 'tile');

            for (const param of ['l', 'x', 'y', 'z']) {
                const response = await request({
                    searchParams: {
                        ...DEFAULT_PARAMS,
                        [param]: undefined
                    }
                });

                assert.strictEqual(response.statusCode, 400);
                assert.strictEqual(response.body, `"${param}" is required`);
            }

            assert.strictEqual(mapTilesScope.isDone(), false);
            assert.strictEqual(apikeysIntServiceScope.isDone(), false);
        });
    });

    describe('when x, y parameters are invalid', () => {
        it('should respond with request error', async () => {
            // x, y are invalid, if x >= 2**z or y >= 2**z
            const query = {
                ...DEFAULT_PARAMS,
                x: 2,
                y: 2,
                z: 1
            };
            const apikeysIntServiceScope = apikeysIntService.reply(200, {ok: true});

            const response = await request({searchParams: query});

            assert.strictEqual(response.statusCode, 400);
            assert.strictEqual(apikeysIntServiceScope.isDone(), true);
        });
    });

    describe('when TVM daemon is unavailable', () => {
        describe('in apikey middleware', () => {
            it('should skip apikey checking and proxy backend response', async () => {
                const tvmDaemonScope = nock(TVMDAEMON_URL, {allowUnmocked: true})
                    .filteringPath(() => '/')
                    .get('/')
                    .reply(500);
                const apikeysIntServiceScope = apikeysIntService.reply(200);
                const mapTilesScope = mapTilesService.reply(200, 'tile');

                const response = await request({searchParams: DEFAULT_PARAMS});

                assert.strictEqual(response.statusCode, 200);
                assert.strictEqual(response.body, 'tile');
                assert.strictEqual(mapTilesScope.isDone(), true);
                assert.strictEqual(apikeysIntServiceScope.isDone(), false);
                assert.strictEqual(tvmDaemonScope.isDone(), true);
            });
        });

        describe('in tiles middleware', () => {
            it('should return error response', async () => {
                const tvmDaemonScope = nock(TVMDAEMON_URL)
                    .filteringPath(() => '/')
                    .get('/')
                    .twice()
                    .reply(500);
                const apikeysIntServiceScope = apikeysIntService.reply(200);
                const mapTilesScope = mapTilesService.reply(200, 'tile');

                const response = await request({searchParams: DEFAULT_PARAMS});

                assert.strictEqual(response.statusCode, 500);
                assert.strictEqual(mapTilesScope.isDone(), false);
                assert.strictEqual(apikeysIntServiceScope.isDone(), false);
                assert.strictEqual(tvmDaemonScope.isDone(), true);
            });
        });
    });

    describe('when apikeysInt service is unavailable', () => {
        it('should skip apikey checking and proxy backend response', async () => {
            const apikeysIntServiceScope = apikeysIntService.reply(500);
            const mapTilesScope = mapTilesService.reply(200, 'tile');

            const response = await request({searchParams: DEFAULT_PARAMS});

            assert.strictEqual(response.statusCode, 200);
            assert.strictEqual(response.body, 'tile');
            assert.strictEqual(mapTilesScope.isDone(), true);
            assert.strictEqual(apikeysIntServiceScope.isDone(), true);
        });
    });

    describe('when apikeysInt service is timed out', () => {
        it('should skip apikey checking and proxy backend response', async () => {
            const apikeysIntServiceScope = apikeysIntService
                .delay(config['apikeysInt.timeoutMilliseconds'])
                .reply(200, {ok: true});
            const mapTilesScope = mapTilesService.reply(200, 'tile');

            const response = await request({searchParams: DEFAULT_PARAMS});

            assert.strictEqual(response.statusCode, 200);
            assert.strictEqual(response.body, 'tile');
            assert.strictEqual(mapTilesScope.isDone(), true);
            assert.strictEqual(apikeysIntServiceScope.isDone(), true);
        });
    });

    describe('when tiles backend is unavailable', () => {
        it('should return error response', async () => {
            const apikeysIntServiceScope = apikeysIntService.reply(200, {ok: true});
            const mapTilesScope = mapTilesService.reply(500);

            const response = await request({searchParams: DEFAULT_PARAMS});

            assert.strictEqual(response.statusCode, 500);
            assert.strictEqual(mapTilesScope.isDone(), true);
            assert.strictEqual(apikeysIntServiceScope.isDone(), true);
        });
    });

    describe('when tiles backend is timed out', () => {
        it('should return error response', async () => {
            const apikeysIntServiceScope = apikeysIntService.reply(200, {ok: true});
            const mapTilesScope = mapTilesService.delay(config['http.defaultTimeoutMilliseconds']).reply(200);

            const response = await request({searchParams: DEFAULT_PARAMS});

            assert.strictEqual(response.statusCode, 500);
            assert.strictEqual(mapTilesScope.isDone(), true);
            assert.strictEqual(apikeysIntServiceScope.isDone(), true);
        });
    });
});

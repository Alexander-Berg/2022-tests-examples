import * as http from 'http';
import * as nock from 'nock';
import * as got from 'got';
import {app} from 'app/app';
import {config} from 'app/config';
import {startServer, stopServer} from 'tests/integration/test-server';
import {intHostConfigLoader} from 'app/lib/host-loader';
import {readFileSync} from 'fs';
import {URLSearchParams} from 'url';
import * as path from 'path';

const defaultHeaders = {
    Authorization: 'OAuth xxxxx'
};

const UID = '9999';
const blackboxValidResponse = {
    error: 'OK',
    status: {
        value: 'VALID',
        uid: UID
    },
    uid: {
        value: UID
    }
};

const client = got.default.extend({
    throwHttpErrors: false,
    retry: 0,
    timeout: 2000,
    responseType: 'json'
});

export const mockDate = (expected: Date) => {
    const _Date = Date;

    function MockDate(mockOverride?: Date | number) {
        return new _Date(mockOverride || expected);
    }

    MockDate.UTC = _Date.UTC;
    MockDate.parse = _Date.parse;
    MockDate.now = () => expected.getTime();
    MockDate.prototype = _Date.prototype;

    global.Date = MockDate as any;

    return () => {
        global.Date = _Date;
    };
};

const MOCK_DATE_VALUE = 1607108400;

const CURRENT_DAY_RESPONSE = {
    2: {
        timestamp: MOCK_DATE_VALUE,
        jams: {
            22: {from: '7'},
            23: {from: '7'}
        }
    },
    213: {
        timestamp: MOCK_DATE_VALUE,
        jams: {
            22: {from: '9'},
            23: {from: '8'}
        }
    }
};

const FULL_RESPONSE = {
    2: {
        timestamp: MOCK_DATE_VALUE,
        jams: {
            22: {from: '7'},
            23: {from: '7'},
            0: {from: '5'}
        }
    },
    213: {
        timestamp: MOCK_DATE_VALUE,
        jams: {
            22: {from: '9'},
            23: {from: '8'},
            0: {from: '6'}
        }
    }
};

const ONE_REGION_RESPONSE = {
    213: {
        timestamp: MOCK_DATE_VALUE,
        jams: {
            22: {from: '9'},
            23: {from: '8'}
        }
    }
};

const URL_PREFIX = '/v1/traffic/forecast';

describe(URL_PREFIX, () => {
    let server: http.Server;
    let url: string;
    let blackboxService: nock.Interceptor;
    let trafficLevelService: nock.Interceptor;

    beforeAll(async () => {
        [server, url] = await startServer(app);
        nock.disableNetConnect();
        nock.enableNetConnect(/localhost/);

        const hosts = await intHostConfigLoader.get();

        blackboxService = nock(config['blackbox.host'])
            .filteringPath(() => '/blackbox')
            .get('/blackbox')
            .query(true);

        trafficLevelService = nock(hosts.trafficInfo)
            .get('/levels_prediction')
            .query(true);
    });

    afterAll(async () => {
        await stopServer(server);
        nock.enableNetConnect();
    });

    it('should return 502 error, when backend is unavailable', async () => {
        blackboxService.reply(200, blackboxValidResponse);
        trafficLevelService.reply(502);

        const response = await client.get(
            `${url}${URL_PREFIX}`,
            {
                headers: defaultHeaders
            }
        );
        expect(response.statusCode).toEqual(502);
    });

    it('should return proxy response with filtered entries, when no query parameters provided', async () => {
        blackboxService.reply(200, blackboxValidResponse);
        trafficLevelService.reply(200, readFileSync(
            path.resolve('./src/tests/integration/v1/fixtures/traffic/traffic-forecast.json')
        ));

        const mockDateReset = mockDate(new Date(MOCK_DATE_VALUE * 1000));

        const response = await client.get(
            `${url}${URL_PREFIX}`,
            {
                headers: defaultHeaders
            }
        );

        mockDateReset();

        expect(response.statusCode).toEqual(200);
        expect(response.body).toEqual(CURRENT_DAY_RESPONSE);
    });

    it('should return proxy response with all entries, when `only_current_day=false` parameter provided', async () => {
        blackboxService.reply(200, blackboxValidResponse);
        trafficLevelService.reply(200, readFileSync(
            path.resolve('./src/tests/integration/v1/fixtures/traffic/traffic-forecast.json')
        ));

        const mockDateReset = mockDate(new Date(MOCK_DATE_VALUE * 1000));

        const response = await client.get(
            `${url}${URL_PREFIX}`,
            {
                searchParams: new URLSearchParams({only_current_day: 'false'}),
                headers: defaultHeaders
            }
        );

        mockDateReset();

        expect(response.statusCode).toEqual(200);
        expect(response.body).toEqual(FULL_RESPONSE);
    });

    it('should return proxy response for one region entries, when `region_id` parameter provided', async () => {
        blackboxService.reply(200, blackboxValidResponse);
        trafficLevelService.reply(200, readFileSync(
            path.resolve('./src/tests/integration/v1/fixtures/traffic/traffic-forecast.json')
        ));

        const mockDateReset = mockDate(new Date(MOCK_DATE_VALUE * 1000));

        const response = await client.get(
            `${url}${URL_PREFIX}`,
            {
                searchParams: new URLSearchParams({region_id: '213'}),
                headers: defaultHeaders
            }
        );

        mockDateReset();

        expect(response.statusCode).toEqual(200);
        expect(response.body).toEqual(ONE_REGION_RESPONSE);
    });

    it('should return proxy response for one regions entries, when `lat` and `lon` parameters provided', async () => {
        blackboxService.reply(200, blackboxValidResponse);
        trafficLevelService.reply(200, readFileSync(
            path.resolve('./src/tests/integration/v1/fixtures/traffic/traffic-forecast.json')
        ));

        const mockDateReset = mockDate(new Date(MOCK_DATE_VALUE * 1000));

        const response = await client.get(
            `${url}${URL_PREFIX}`,
            {
                searchParams: new URLSearchParams({lat: '0.0', lon: '0.0'}),
                headers: defaultHeaders
            }
        );

        mockDateReset();

        expect(response.statusCode).toEqual(200);
        expect(response.body).toEqual(ONE_REGION_RESPONSE);
    });

    it('should return 400 error, when both `region_id` and `lat`/`lon` parameters provided', async () => {
        blackboxService.reply(200, blackboxValidResponse);
        trafficLevelService.reply(200, readFileSync(
            path.resolve('./src/tests/integration/v1/fixtures/traffic/traffic-forecast.json')
        ));

        const mockDateReset = mockDate(new Date(MOCK_DATE_VALUE * 1000));

        const response = await client.get(
            `${url}${URL_PREFIX}`,
            {
                searchParams: new URLSearchParams({region_id: '213', lat: '0.0', lon: '0.0'}),
                headers: defaultHeaders
            }
        );

        mockDateReset();

        expect(response.statusCode).toEqual(400);
    });
});

import * as http from 'http';
import * as nock from 'nock';
import * as got from 'got';
import {URLSearchParams} from 'url';
import {app} from 'app/app';
import {config} from 'app/config';
import {startServer, stopServer} from 'tests/integration/test-server';
import {intHostConfigLoader} from 'app/lib/host-loader';
import {readFileSync} from 'fs';
import * as path from 'path';

const defaultHeaders = {
    Authorization: 'OAuth xxxxx'
};

const defaultQuery: { region_id: string } = {
    region_id: '213'
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

const RESPONSE = {
    regionId: '213',
    level: '5',
    style: 'yellow'
};

const URL_PREFIX = '/v1/traffic/level';

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
            .get('/info')
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
                searchParams: new URLSearchParams(defaultQuery),
                headers: defaultHeaders
            }
        );
        expect(response.statusCode).toEqual(502);
    });

    it('should return 400 error, when bad query is passed', async () => {
        blackboxService.reply(200, blackboxValidResponse);

        const response = await client.get(
            `${url}${URL_PREFIX}`,
            {
                headers: defaultHeaders
            }
        );
        expect(response.statusCode).toEqual(400);
    });

    it('should proxy response, when authorization is valid', async () => {
        blackboxService.reply(200, blackboxValidResponse);
        trafficLevelService.reply(200, readFileSync(
            path.resolve('./src/tests/integration/v1/fixtures/traffic/traffic-level.protobuf')
        ));

        const response = await client.get(
            `${url}${URL_PREFIX}`,
            {
                searchParams: new URLSearchParams(defaultQuery),
                headers: defaultHeaders
            }
        );
        expect(response.statusCode).toEqual(200);
        expect(response.body).toEqual(RESPONSE);
    });
});

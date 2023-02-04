import * as http from 'http';
import * as nock from 'nock';
import * as got from 'got';
import {app} from 'app/app';
import {config} from 'app/config';
import {startServer, stopServer} from 'tests/integration/test-server';
import {intHostConfigLoader} from 'app/lib/host-loader';

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

const URL_PREFIX = '/v1/geoadv/userinfo/isowner';

describe(URL_PREFIX, () => {
    let server: http.Server;
    let url: string;
    let blackboxService: nock.Interceptor;
    let geoAdvService: nock.Interceptor;

    beforeAll(async () => {
        [server, url] = await startServer(app);
        nock.disableNetConnect();
        nock.enableNetConnect(/localhost/);

        const hosts = await intHostConfigLoader.get();

        blackboxService = nock(config['blackbox.host'])
            .filteringPath(() => '/blackbox')
            .get('/blackbox')
            .query(true);

        geoAdvService = nock(hosts.spravApi)
            .get(`/v3/users/${UID}`)
            .query(true);
    });

    afterAll(async () => {
        await stopServer(server);
        nock.enableNetConnect();
    });

    it('should return 401 error, when disabled account is passed', async () => {
        blackboxService.reply(200, {error: 'OK', status: {value: 'DISABLED'}});

        const response = await client.get(
            `${url}${URL_PREFIX}`,
            {
                headers: defaultHeaders
            }
        );
        expect(response.statusCode).toEqual(401);
    });

    it('should return 401 error, when bad authorization is passed', async () => {
        blackboxService.reply(200, {error: 'OK', status: {value: 'INVALID'}});

        const response = await client.get(
            `${url}${URL_PREFIX}`,
            {
                headers: defaultHeaders
            }
        );
        expect(response.statusCode).toEqual(401);
    });

    it('should proxy response, when authorization is valid', async () => {
        blackboxService.reply(200, blackboxValidResponse);
        geoAdvService.reply(200);

        const response = await client.get(
            `${url}${URL_PREFIX}`,
            {
                headers: defaultHeaders
            }
        );
        expect(response.statusCode).toEqual(200);
    });
});

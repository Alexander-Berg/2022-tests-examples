import * as http from 'http';
import * as nock from 'nock';
import * as got from 'got';
import {app} from 'app/app';
import {URLSearchParams} from 'url';
import {config} from 'app/config';
import {startServer, stopServer} from 'tests/integration/test-server';
import {intHostConfigLoader} from 'app/lib/host-loader';

const defaultHeaders = {
    'Accept-Language': 'en',
    Authorization: 'OAuth xxxxx'
};

const defaultQuery = {
    id: '1234'
};

const client = got.default.extend({
    throwHttpErrors: false,
    retry: 0,
    timeout: 2000,
    responseType: 'json',
    headers: defaultHeaders
});

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

const URL_PREFIX = '/v1/sprav/companies/posts';

describe(URL_PREFIX, () => {
    let server: http.Server;
    let url: string;
    let blackboxService: nock.Interceptor;
    let spravCompaniesPostsService: nock.Interceptor;

    beforeAll(async () => {
        [server, url] = await startServer(app);
        nock.disableNetConnect();
        nock.enableNetConnect(/localhost/);

        const hosts = await intHostConfigLoader.get();

        blackboxService = nock(config['blackbox.host'])
            .filteringPath(() => '/blackbox')
            .get('/blackbox')
            .query(true);

        spravCompaniesPostsService = nock(hosts.spravApi)
            .get(`/v1/companies/${defaultQuery.id}/posts`);
    });

    afterAll(async () => {
        await stopServer(server);
        nock.enableNetConnect();
    });

    it('should return error, when bad authorization is passed', async () => {
        blackboxService.reply(200, {error: 'OK', status: {value: 'INVALID'}});
        spravCompaniesPostsService.reply(200);

        const response = await client.get(
            `${url}${URL_PREFIX}`,
            {
                searchParams: new URLSearchParams(defaultQuery)
            });
        expect(response.statusCode).toEqual(401);
    });

    it('should return error, when authorization is disabled', async () => {
        blackboxService.reply(200, {error: 'OK', status: {value: 'DISABLED'}});
        spravCompaniesPostsService.reply(200);

        const response = await client.get(
            `${url}${URL_PREFIX}`,
            {
                searchParams: new URLSearchParams(defaultQuery)
            });
        expect(response.statusCode).toEqual(403);
    });

    it('should send response, when authorization is valid', async () => {
        blackboxService.reply(200, blackboxValidResponse);
        spravCompaniesPostsService.reply(200);

        const response = await client.get(
            `${url}${URL_PREFIX}`,
            {
                searchParams: new URLSearchParams(defaultQuery)
            });
        expect(response.statusCode).toEqual(200);
    });

    it('should return error, when backend is unavailable', async () => {
        blackboxService.reply(200, blackboxValidResponse);
        spravCompaniesPostsService.reply(502);

        const response = await client.get(
            `${url}${URL_PREFIX}`,
            {
                searchParams: new URLSearchParams(defaultQuery)
            });
        expect(response.statusCode).toEqual(502);
    });

    it('should return error, when query is incorrect', async () => {
        blackboxService.reply(200, blackboxValidResponse);
        spravCompaniesPostsService.reply(200);

        const response = await client.get(`${url}${URL_PREFIX}`);
        expect(response.statusCode).toEqual(400);
    });
});

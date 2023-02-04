import * as http from 'http';
import * as nock from 'nock';
import * as got from 'got';
import {app} from 'app/app';
import {config} from 'app/config';
import {startServer, stopServer} from 'tests/integration/test-server';

const defaultHeaders = {
    Authorization: 'OAuth xxxxx'
};

const client = got.default.extend({
    throwHttpErrors: false,
    retry: 0,
    timeout: 2000,
    responseType: 'json'
});

describe('/v1/tiles_pois', () => {
    let server: http.Server;
    let url: string;
    let blackboxService: nock.Interceptor;

    beforeAll(async () => {
        [server, url] = await startServer(app);
        nock.disableNetConnect();
        nock.enableNetConnect(/localhost/);

        blackboxService = nock(config['blackbox.host'])
            .filteringPath(() => '/blackbox')
            .get('/blackbox')
            .query(true);
    });

    afterAll(async () => {
        await stopServer(server);
        nock.enableNetConnect();
    });

    it('should return error, when blackbox is unavailable', async () => {
        blackboxService.reply(500);

        const response = await client.get(`${url}/v1/tiles_pois`, {
            headers: defaultHeaders
        });
        expect(response.statusCode).toEqual(401);
    });

    it('should return error, when bad authorization is passed', async () => {
        blackboxService.reply(200, {error: 'OK', status: {value: 'INVALID'}});

        const response = await client.get(`${url}/v1/tiles_pois`, {
            headers: defaultHeaders
        });
        expect(response.statusCode).toEqual(401);
    });

    it('should proxy pois, when authorization is valid', async () => {
        blackboxService.reply(200, {error: 'OK', uid: {value: 123}, status: {value: 'VALID'}});

        const response = await client.get(`${url}/v1/tiles_pois`, {
            headers: defaultHeaders,
            responseType: 'json'
        });
        expect(response.statusCode).toEqual(200);
        expect(response.headers['content-type']).toEqual('application/json; charset=utf-8');
        expect(response.body).toEqual([]);
    });
});

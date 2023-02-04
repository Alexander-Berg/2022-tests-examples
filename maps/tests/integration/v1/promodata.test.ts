import * as http from 'http';
import * as nock from 'nock';
import {readFileSync} from 'fs';
import * as path from 'path';
import * as got from 'got';
import {app} from 'app/app';
import {config} from 'app/config';
import {startServer, stopServer} from 'tests/integration/test-server';

const projectsResponse = JSON.parse(
    readFileSync(
        path.resolve('./src/tests/integration/v1/fixtures/promodata-response.json'),
        'utf-8'
    )
);

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

const blackboxInvalidResponse = {
    ...blackboxValidResponse,
    status: {
        valid: 'INVALID'
    }
};

const client = got.default.extend({
    throwHttpErrors: false,
    retry: 0,
    timeout: 2000,
    responseType: 'json'
});

describe('/v1/promodata', () => {
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

    it('should return a valid entry data, when correct blackbox authorization', async () => {
        blackboxService.reply(200, blackboxValidResponse);

        const response = await client.get(`${url}/v1/promodata`, {
            headers: defaultHeaders,
            responseType: 'json'
        });
        expect(response.statusCode).toEqual(200);
        expect(response.body).toEqual(projectsResponse);
    });

    it('should return a valid entry data, when correct uuid', async () => {
        blackboxService.reply(200, blackboxValidResponse);

        const response = await client.get(`${url}/v1/promodata`, {
            headers: {
                ...defaultHeaders,
                'x-user-uuid': '1234567'
            },
            responseType: 'json'
        });
        expect(response.statusCode).toEqual(200);
        expect(response.body).toEqual(projectsResponse);
    });

    it('should return a valid entry data, when blackbox authorization is invalid', async () => {
        blackboxService.reply(200, blackboxInvalidResponse);

        const response = await client.get(`${url}/v1/promodata`, {
            headers: defaultHeaders,
            responseType: 'json'
        });
        expect(response.statusCode).toEqual(401);
    });

    it('should return error, when blackbox authorization is invalid', async () => {
        blackboxService.reply(200, blackboxInvalidResponse);

        const response = await client.get(`${url}/v1/promodata`, {
            headers: defaultHeaders,
            responseType: 'json'
        });
        expect(response.statusCode).toEqual(401);
    });

    it('should return error, when uuid is missed', async () => {
        blackboxService.reply(200, blackboxValidResponse);

        const response = await client.get(`${url}/v1/promodata`);
        expect(response.statusCode).toEqual(400);
    });
});

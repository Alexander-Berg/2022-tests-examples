import * as http from 'http';
import * as nock from 'nock';
import * as got from 'got';
import {app} from 'app/app';
import {config} from 'app/config';
import {startServer, stopServer} from 'tests/integration/test-server';
import {intHostConfigLoader} from 'app/lib/host-loader';
import * as fakeResponse from 'tests/integration/v1/fixtures/feedback-response.json';

const UID = 9999;
const fakeResponseWithUid = JSON.parse(JSON.stringify(fakeResponse));
fakeResponseWithUid.original_task.metadata.uid = UID;

const blackboxValidResponse = {
    error: 'OK',
    status: {
        value: 'VALID',
        uid: UID.toString()
    },
    uid: {
        value: UID.toString()
    }
};

const defaultRequest = {
    metadata: {}
};

const defaultHeaders = {
    Authorization: 'OAuth xxxxx'
};

const client = got.default.extend({
    throwHttpErrors: false,
    retry: 0,
    timeout: 2000,
    responseType: 'json'
});

describe('/v1/feedback/post', () => {
    let server: http.Server;
    let url: string;
    let blackboxService: nock.Interceptor;
    let feedbackService: nock.Interceptor;

    beforeAll(async () => {
        [server, url] = await startServer(app);
        nock.disableNetConnect();
        nock.enableNetConnect(/localhost/);

        const hosts = await intHostConfigLoader.get();

        blackboxService = nock(config['blackbox.host'])
            .filteringPath(() => '/blackbox')
            .get('/blackbox')
            .query(true);

        feedbackService = nock(hosts.feedbackInt).post('/v1/tasks');
    });

    afterAll(async () => {
        await stopServer(server);
        nock.enableNetConnect();
    });

    it('should return feedback response', async () => {
        feedbackService.reply(201, fakeResponse);

        const response = await client.post(`${url}/v1/feedback/post`);
        expect(response.statusCode).toEqual(201);
        expect(response.body).toEqual(fakeResponse);
    });

    it('should return feedback response with uid', async () => {
        blackboxService.reply(200, blackboxValidResponse);

        const hosts = await intHostConfigLoader.get();
        nock(hosts.feedbackInt)
            .post('/v1/tasks', (body) => body.metadata && !isNaN(body.metadata.uid))
            .reply(201, fakeResponseWithUid);

        const response = await client.post(`${url}/v1/feedback/post`, {
            headers: defaultHeaders,
            json: defaultRequest
        });
        expect(response.statusCode).toEqual(201);
        expect(response.body).toEqual(fakeResponseWithUid);
    });

    it('should throw X-Forwarded-For header to feedback service', async () => {
        blackboxService.reply(200, blackboxValidResponse);

        const hosts = await intHostConfigLoader.get();
        const reqHeaders = {
            'X-Forwarded-For': 'xxxxx'
        };

        nock(hosts.feedbackInt, {
            reqheaders: reqHeaders
        })
            .post('/v1/tasks')
            .reply(201, fakeResponseWithUid);

        const response = await client.post(`${url}/v1/feedback/post`, {
            headers: {
                ...defaultHeaders,
                ...reqHeaders
            },
            json: defaultRequest
        });
        expect(response.statusCode).toEqual(201);
        expect(response.body).toEqual(fakeResponseWithUid);
    });

    it('should return error, when backend is unavailable', async () => {
        blackboxService.reply(200, blackboxValidResponse);
        feedbackService.reply(400);

        const response = await client.post(`${url}/v1/feedback/post`);
        expect(response.statusCode).toEqual(400);
    });

    it('should send feedback anyway, when blackbox is unavailable', async () => {
        blackboxService.reply(502);
        feedbackService.reply(200, fakeResponse);

        const response = await client.post(`${url}/v1/feedback/post`, {
            headers: defaultHeaders
        });
        expect(response.statusCode).toEqual(200);
        expect(response.body).toEqual(fakeResponse);
    });

    it('should return error, when bad authorization is passed', async () => {
        const invalidBlackboxReponse = {
            ...blackboxValidResponse,
            status: {
                value: 'INVALID'
            }
        };

        blackboxService.reply(200, invalidBlackboxReponse);
        feedbackService.reply(200, fakeResponse);

        const response = await client.post(`${url}/v1/feedback/post`, {
            headers: defaultHeaders
        });
        expect(response.statusCode).toEqual(401);
    });
});

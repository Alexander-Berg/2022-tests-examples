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

const defaultResponse = {unique_id: '123'};

const client = got.default.extend({
    throwHttpErrors: false,
    retry: 0,
    timeout: 2000,
    responseType: 'json',
    headers: defaultHeaders
});

describe('/v1/business/feedback', () => {
    let server: http.Server;
    let url: string;
    let blackboxService: nock.Interceptor;
    let businessFeedbackService: nock.Interceptor;

    beforeAll(async () => {
        [server, url] = await startServer(app);
        nock.disableNetConnect();
        nock.enableNetConnect(/localhost/);

        const hosts = await intHostConfigLoader.get();

        blackboxService = nock(config['blackbox.host'])
            .filteringPath(() => '/blackbox')
            .get('/blackbox')
            .query(true);

        businessFeedbackService = nock(hosts.businessFeedback)
            .post('/v1/feedback');
    });

    afterAll(async () => {
        await stopServer(server);
        nock.enableNetConnect();
    });

    it('should send feedback anyway, when blackbox is unavailable', async () => {
        blackboxService.reply(502);
        businessFeedbackService.reply(200, defaultResponse);

        const response = await client.post(`${url}/v1/business/feedback`);
        expect(response.statusCode).toEqual(200);
    });

    it('should return error, when bad authorization is passed', async () => {
        blackboxService.reply(200, {error: 'OK', status: {value: 'INVALID'}});
        businessFeedbackService.reply(200, defaultResponse);

        const response = await client.post(`${url}/v1/business/feedback`);
        expect(response.statusCode).toEqual(401);
    });

    it('should send feedback, when authorization is valid', async () => {
        blackboxService.reply(200, {error: 'OK', status: {value: 'VALID'}});
        businessFeedbackService.reply(200, defaultResponse);

        const response = await client.post(`${url}/v1/business/feedback`);
        expect(response.body).toEqual(defaultResponse);
        expect(response.statusCode).toEqual(200);
    });

    it('should return error, when backend is unavailable', async () => {
        blackboxService.reply(502);
        businessFeedbackService.reply(502);

        const response = await client.post(`${url}/v1/business/feedback`);
        expect(response.statusCode).toEqual(502);
    });
});

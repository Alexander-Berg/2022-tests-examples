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

const client = got.default.extend({
    throwHttpErrors: false,
    retry: 0,
    timeout: 2000,
    responseType: 'json'
});

describe('/v1/save_user_answer', () => {
    let server: http.Server;
    let url: string;
    let blackboxService: nock.Interceptor;
    let ugcService: nock.Scope;

    beforeAll(async () => {
        [server, url] = await startServer(app);
        nock.disableNetConnect();
        nock.enableNetConnect(/localhost/);

        const hosts = await intHostConfigLoader.get();

        blackboxService = nock(config['blackbox.host'])
            .filteringPath(() => '/blackbox')
            .get('/blackbox')
            .query(true);

        ugcService = nock(hosts.ugcSearch);
    });

    afterAll(async () => {
        await stopServer(server);
        nock.enableNetConnect();
    });

    it('should save user answer, when blackbox is unavailable', async () => {
        blackboxService.reply(502);
        ugcService.post('/saveuseranswer').query(true).reply(200);

        const response = await client.post(`${url}/v1/save_user_answer`, {
            headers: defaultHeaders
        });
        expect(response.statusCode).toEqual(200);
    });

    it('should save user answer, when bad authorization is passed', async () => {
        blackboxService.reply(200, {error: 'OK', status: {value: 'INVALID'}});
        ugcService.post('/saveuseranswer').query(true).reply(200);

        const response = await client.post(`${url}/v1/save_user_answer`, {
            headers: defaultHeaders
        });
        expect(response.statusCode).toEqual(200);
    });

    it('should send feedback with uid, when authorization is valid', async () => {
        blackboxService.reply(200, {error: 'OK', uid: {value: 123}, status: {value: 'VALID'}});
        ugcService.post('/saveuseranswer').query({
            passport_uid: 123,
            app_id: 'undefined',
            permalink: 'undefined',
            mobapp: 'undefined',
            mobdevice: 'undefined'
        }).reply(200);

        const response = await client.post(`${url}/v1/save_user_answer`, {
            headers: defaultHeaders
        });
        expect(response.statusCode).toEqual(200);
    });

    it('should return error, when backend is unavailable', async () => {
        blackboxService.reply(502);
        ugcService.post('/saveuseranswer').query(true).reply(502);

        const response = await client.post(`${url}/v1/save_user_answer`, {
            headers: defaultHeaders
        });
        expect(response.statusCode).toEqual(502);
    });
});

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

describe('/v1/photos/report', () => {
    let server: http.Server;
    let url: string;
    let blackboxService: nock.Interceptor;
    let fotkiService: nock.Interceptor;

    beforeAll(async () => {
        [server, url] = await startServer(app);
        nock.disableNetConnect();
        nock.enableNetConnect(/localhost/);

        const hosts = await intHostConfigLoader.get();

        blackboxService = nock(config['blackbox.host'])
            .filteringPath(() => '/blackbox')
            .get('/blackbox')
            .query(true);

        fotkiService = nock(hosts.spravPhotoApi)
            .get('/old/addOrganizationComplaintsByUrn')
            .query(true);
    });

    afterAll(async () => {
        await stopServer(server);
        nock.enableNetConnect();
    });

    it('should return error, when blackbox is unavailable', async () => {
        blackboxService.reply(502);

        const response = await client.get(`${url}/v1/photos/report`, {
            headers: defaultHeaders
        });
        expect(response.statusCode).toEqual(403);
    });

    it('should return error, when backend is unavailable', async () => {
        blackboxService.reply(200, blackboxValidResponse);
        fotkiService.reply(502);

        const response = await client.get(
            `${url}/v1/photos/report` +
            '?path=addOrganizationComplaints&company=1&image_urns=2&complaint_type=ORGANIZATION_BAD_QUALITY',
            {
                headers: defaultHeaders
            }
        );
        expect(response.statusCode).toEqual(502);
    });

    it('should return error, when bad authorization is passed', async () => {
        blackboxService.reply(200, {error: 'OK', status: {value: 'INVALID'}});

        const response = await client.get(`${url}/v1/photos/report`, {
            headers: defaultHeaders
        });
        expect(response.statusCode).toEqual(403);
    });

    it('should return error, when bad path is passed', async () => {
        blackboxService.reply(200, blackboxValidResponse);

        const response = await client.get(`${url}/v1/photos/report?path=badPath`, {
            headers: defaultHeaders
        });
        expect(response.statusCode).toEqual(400);
    });

    it('should proxy response, when authorization is valid', async () => {
        blackboxService.reply(200, blackboxValidResponse);
        fotkiService.reply(200);

        const response = await client.get(
            `${url}/v1/photos/report` +
            '?path=addOrganizationComplaints&company=1&image_urns=2&complaint_type=ORGANIZATION_BAD_QUALITY',
            {
                headers: defaultHeaders
            }
        );
        expect(response.statusCode).toEqual(200);
    });
});

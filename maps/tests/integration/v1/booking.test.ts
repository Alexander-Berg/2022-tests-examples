import * as http from 'http';
import * as nock from 'nock';
import * as got from 'got';
import {app} from 'app/app';
import {config} from 'app/config';
import {startServer, stopServer} from 'tests/integration/test-server';
import {intHostConfigLoader} from 'app/lib/host-loader';

const UID = 9999;
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

const client = got.default.extend({
    throwHttpErrors: false,
    retry: 0,
    timeout: 2000,
    responseType: 'json'
});

const defaultHeaders = {
    Authorization: 'OAuth xxxxx',
    'content-type': 'application/json'
};

const MOCK_RESPONSE = {
    notifications: [],
    totalCount: 1
};

describe('/v1/booking/notifications', () => {
    let server: http.Server;
    let url: string;
    let bookingHost: string;

    beforeAll(async () => {
        [server, url] = await startServer(app);
        nock.disableNetConnect();
        nock.enableNetConnect(/localhost/);

        const hosts = await intHostConfigLoader.get();
        bookingHost = hosts.bookingInt;
    });

    afterAll(async () => {
        await stopServer(server);
        nock.enableNetConnect();
    });

    beforeEach(() => {
        nock(config['blackbox.host'])
            .filteringPath(() => '/blackbox')
            .get('/blackbox')
            .query(true)
            .reply(200, blackboxValidResponse);

        nock(bookingHost)
            .get('/v1/bookings/notifications')
            .reply(200, MOCK_RESPONSE);
    });

    afterEach(() => {
        nock.cleanAll();
    });

    it('should respond with notification', async () => {
        const response = await client.get(`${url}/v1/booking/notifications`, {headers: defaultHeaders});
        expect(response.statusCode).toEqual(200);
        expect(response.body).toEqual(MOCK_RESPONSE);
    });
});

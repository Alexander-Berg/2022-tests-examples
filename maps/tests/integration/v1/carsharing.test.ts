import * as http from 'http';
import * as nock from 'nock';
import * as got from 'got';
import {app} from 'app/app';
import {startServer, stopServer} from 'tests/integration/test-server';
import {intHostConfigLoader} from 'app/lib/host-loader';
import {encodedEmptyResponse} from 'app/v1/routers/carsharing';

const client = got.default.extend({
    throwHttpErrors: false,
    retry: 0,
    timeout: 2000,
    responseType: 'buffer'
});

describe('/v1/carsharing/fixpoint', () => {
    let server: http.Server;
    let url: string;
    let carsharingHost: string;

    beforeAll(async () => {
        [server, url] = await startServer(app);
        nock.disableNetConnect();
        nock.enableNetConnect(/localhost/);

        const hosts = await intHostConfigLoader.get();
        carsharingHost = hosts.carsharing;
    });

    afterAll(async () => {
        await stopServer(server);
        nock.enableNetConnect();
    });

    beforeEach(() => {
        nock(carsharingHost)
            .get('/api/maps/offers/fixpoint')
            .reply(200, {someValue: 123});
    });

    afterEach(() => {
        nock.cleanAll();
    });

    it('should return empty responses every time', async () => {
        const response = await client.get(`${url}/v1/carsharing/fixpoint`);
        expect(response.statusCode).toEqual(200);
        expect(response.body).toEqual(encodedEmptyResponse);
    });
});

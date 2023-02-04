import * as http from 'http';
import * as nock from 'nock';
import * as got from 'got';
import {app} from 'app/app';
import {startServer, stopServer} from 'tests/integration/test-server';
import {intHostConfigLoader} from 'app/lib/host-loader';

const client = got.default.extend({
    throwHttpErrors: false,
    retry: 0,
    timeout: 2000
});

describe('/v1/discovery', () => {
    let server: http.Server;
    let url: string;
    let discoveryHost: string;

    beforeAll(async () => {
        [server, url] = await startServer(app);
        nock.disableNetConnect();
        nock.enableNetConnect(/localhost/);

        const hosts = await intHostConfigLoader.get();
        discoveryHost = hosts.discoveryInt;
    });

    afterAll(async () => {
        await stopServer(server);
        nock.enableNetConnect();
    });

    beforeEach(() => {
        nock(discoveryHost)
            .get('/v1/page')
            .query({
                alias: 'gde-pit-vino-sankt-peterburg'
            })
            .reply(200)
            .get('/v1/event/search_by_point')
            .query({
                lon: 37.5,
                lat: 55.7
            })
            .reply(200);
    });

    afterEach(() => {
        nock.cleanAll();
    });

    it('should return page by alias', async () => {
        const response = await client.get(`${url}/v1/discovery/page`, {
            searchParams: {
                alias: 'gde-pit-vino-sankt-peterburg'
            }
        });
        expect(response.statusCode).toEqual(200);
    });

    it('should find event by coordinates', async () => {
        const response = await client.get(`${url}/v1/discovery/event/search_by_point`, {
            searchParams: {
                lon: 37.5,
                lat: 55.7
            }
        });
        expect(response.statusCode).toEqual(200);
    });
});

import * as http from 'http';
import * as nock from 'nock';
import * as got from 'got';
import {URLSearchParams} from 'url';
import {app} from 'app/app';
import {config} from 'app/config';
import {startServer, stopServer} from 'tests/integration/test-server';
import {intHostConfigLoader} from 'app/lib/host-loader';

const defaultQuery: any = {
    lon: 37.670714,
    lat: 55.789731,
    zoom: 12
};

const client = got.default.extend({
    throwHttpErrors: false,
    retry: 0,
    timeout: 2000,
    responseType: 'json'
});

describe('/v1/ar_objects', () => {
    let server: http.Server;
    let url: string;
    let s3apiHost: string;

    beforeAll(async () => {
        [server, url] = await startServer(app);
        nock.disableNetConnect();
        nock.enableNetConnect(/localhost/);

        const {s3api} = await intHostConfigLoader.get();
        s3apiHost = s3api;
    });

    afterAll(async () => {
        await stopServer(server);
        nock.enableNetConnect();
    });

    beforeEach(() => {
        nock(s3apiHost)
            .get(`/${config['s3.arObjectsIos']}`)
            .reply(200);

        nock(s3apiHost)
            .get(`/${config['s3.arObjectsAndroid']}`)
            .reply(200);
    });

    afterEach(() => {
        nock.cleanAll();
    });

    it('should return ios config', async () => {
        const response = await client.get(`${url}/v1/ar_objects`, {
            searchParams: new URLSearchParams({
                ...defaultQuery,
                client: 'ios'
            })
        });
        expect(response.statusCode).toEqual(200);
    });

    it('should return ios config if query param in missing', async () => {
        const response = await client.get(`${url}/v1/ar_objects`, {
            searchParams: new URLSearchParams(defaultQuery)
        });
        expect(response.statusCode).toEqual(200);
    });

    it('should return android config', async () => {
        const response = await client.get(`${url}/v1/ar_objects`, {
            searchParams: new URLSearchParams({
                ...defaultQuery,
                client: 'android'
            })
        });
        expect(response.statusCode).toEqual(200);
    });

    it('should return error when backend is unavailable', async () => {
        nock(s3apiHost)
            .get(`/${config['s3.arObjectsIos']}`)
            .reply(400);

        const response = await client.get(`${url}/v1/ar_objects`, {
            searchParams: new URLSearchParams(defaultQuery)
        });
        expect(response.statusCode).toEqual(200);
    });

    describe('query params is missing', () => {
        it('should return error if "lon" param is missing', async () => {
            const queryParams = {...defaultQuery};
            delete queryParams.lon;

            const response = await client.get(`${url}/v1/ar_objects`, {
                searchParams: new URLSearchParams(queryParams)
            });
            expect(response.statusCode).toEqual(400);
        });

        it('should return error if "lat" param is missing', async () => {
            const queryParams = {...defaultQuery};
            delete queryParams.lat;

            const response = await client.get(`${url}/v1/ar_objects`, {
                searchParams: new URLSearchParams(queryParams)
            });
            expect(response.statusCode).toEqual(400);
        });

        it('should return error if "zoom" param is missing', async () => {
            const queryParams = {...defaultQuery};
            delete queryParams.zoom;

            const response = await client.get(`${url}/v1/ar_objects`, {
                searchParams: new URLSearchParams(queryParams)
            });
            expect(response.statusCode).toEqual(400);
        });

        it('should return error if client param is incorrect', async () => {
            const response = await client.get(`${url}/v1/ar_objects`, {
                searchParams: new URLSearchParams({
                    ...defaultQuery,
                    client: 'blackberry'
                })
            });
            expect(response.statusCode).toEqual(400);
        });
    });
});

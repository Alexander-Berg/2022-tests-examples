import * as http from 'http';
import * as path from 'path';
import * as fs from 'fs';
import * as nock from 'nock';
import {extend, Response} from 'got';
import {OpenApiValidator} from 'express-openapi-validate';
import {app} from 'app/app';
import {intHostConfigLoader} from 'app/lib/host-loader';
import {startServer, stopServer} from 'tests/integration/test-server';
import ugcMock from 'tests/integration/v1/data/ugc-reviews';
import geosearchMock from 'tests/integration/v1/data/geosearch';
import {
    UID123_USER_TICKET,
    CORRECT_SRC_SERVICE_TICKET,
    WRONG_SRC_SERVICE_TICKET
} from 'tests/integration/tvm-tickets';

const specDoc = JSON.parse(fs.readFileSync(path.resolve('./out/generated/spec.json'), 'utf-8'));
const validator = new OpenApiValidator(specDoc);

const defaultUid = 123;
const defaultHeaders = {
    'X-Ya-User-Ticket': UID123_USER_TICKET,
    'X-Ya-Service-Ticket': CORRECT_SRC_SERVICE_TICKET
};

const defaultQuery: any = {
    lang: 'ru_RU',
    origin: 'test'
};

const client = extend({
    throwHttpErrors: false,
    retry: 0,
    timeout: 1500,
    json: true,
    query: defaultQuery,
    headers: defaultHeaders
});

describe('GET /v1/reviews', () => {
    let server: http.Server;
    let url: string;
    let ugcSearch: string;
    let searchHost: string;

    beforeAll(async () => {
        const hosts = await intHostConfigLoader.get();
        ugcSearch = hosts.ugcSearch;
        searchHost = hosts.search;

        [server, url] = await startServer(app);
        nock.disableNetConnect();
        nock.enableNetConnect(/localhost/);
    });

    afterAll(async () => {
        await stopServer(server);
        nock.enableNetConnect();
    });

    beforeEach(() => {
        // all
        nock(ugcSearch)
            .get(`/api/v1/users/${defaultUid}/orgs/reviews`)
            .query({
                limit: 5,
                offset: 0
            })
            .reply(200, ugcMock.all);

        nock(searchHost)
            .get([
                '/yandsearch?lang=ru_RU',
                'results=3',
                'snippets=photos%2F1.x%2Ccluster_permalinks',
                'ms=pb',
                'origin=personal-account-oid',
                'business_oid=1095958628&business_oid=105068524205&business_oid=148038830577'
            ].join('&'))
            .reply(200, geosearchMock.reviews);
    });

    it('should return 200 if only "uid" parameters is correct', async () => {
        const response = await client.get(`${url}/v1/reviews`);
        expect(response.statusCode).toEqual(200);
        validateResponseBySchema(response);
    });

    describe('parameters checking', () => {
        it('should return 401 if "X-Ya-User-Ticket" header is missed', async () => {
            const response = await client.get(`${url}/v1/reviews`, {
                headers: {
                    ...defaultHeaders,
                    'X-Ya-User-Ticket': ''
                }
            });
            expect(response.statusCode).toEqual(401);
            validateResponseBySchema(response);
        });

        it('should return 401 if "X-Ya-Service-Ticket" is missed', async () => {
            const response = await client.get(`${url}/v1/reviews`, {
                headers: {
                    ...defaultHeaders,
                    'X-Ya-Service-Ticket': ''
                }
            });
            expect(response.statusCode).toEqual(401);
            validateResponseBySchema(response);
        });

        it('should return 403 if "X-Ya-Service-Ticket" is wrong', async () => {
            const response = await client.get(`${url}/v1/reviews`, {
                headers: {
                    ...defaultHeaders,
                    'X-Ya-Service-Ticket': WRONG_SRC_SERVICE_TICKET
                }
            });
            expect(response.statusCode).toEqual(403);
            validateResponseBySchema(response);
        });

        it('should return 400 if "origin" parameter was missed', async () => {
            const response = await client.get(`${url}/v1/reviews`, {
                query: {
                    ...defaultQuery,
                    origin: ''
                }
            });
            expect(response.statusCode).toEqual(400);
            validateResponseBySchema(response);
        });

        it('should return 400 if "offset" parameter is invalid', async () => {
            const response = await client.get(`${url}/v1/reviews`, {
                query: {
                    ...defaultQuery,
                    offset: -10
                }
            });
            expect(response.statusCode).toEqual(400);
            validateResponseBySchema(response);
        });

        it('should return 400 if "limit" parameter is invalid', async () => {
            const response = await client.get(`${url}/v1/reviews`, {
                query: {
                    ...defaultQuery,
                    limit: -10
                }
            });
            expect(response.statusCode).toEqual(400);
            validateResponseBySchema(response);
        });
    });
});

function validateResponseBySchema(response: Response<any>) {
    const validateResponse = validator.validateResponse('get', '/v1/reviews');
    validateResponse(response);
}

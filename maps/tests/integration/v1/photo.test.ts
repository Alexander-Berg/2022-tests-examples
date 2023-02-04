import * as http from 'http';
import * as path from 'path';
import * as fs from 'fs';
import * as nock from 'nock';
import {extend, Response} from 'got';
import {OpenApiValidator} from 'express-openapi-validate';
import {app} from 'app/app';
import {intHostConfigLoader} from 'app/lib/host-loader';
import {startServer, stopServer} from 'tests/integration/test-server';
import {
    UID123_USER_TICKET,
    WRONG_SRC_SERVICE_TICKET,
    CORRECT_SRC_SERVICE_TICKET
} from 'tests/integration/tvm-tickets';

import * as mock from 'tests/integration/v1/data/photo';

const specDoc = JSON.parse(fs.readFileSync(path.resolve('./out/generated/spec.json'), 'utf-8'));
const validator = new OpenApiValidator(specDoc);

const defaultQuery = mock.defaultRequest;
const defaultHeaders = {
    'X-Ya-User-Ticket': UID123_USER_TICKET,
    'X-Ya-Service-Ticket': CORRECT_SRC_SERVICE_TICKET
};

const client = extend({
    throwHttpErrors: false,
    retry: 0,
    timeout: 1500,
    json: true,
    headers: defaultHeaders,
    query: defaultQuery
});

describe('GET /v1/photo', () => {
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
            .get(`/api/v1/users/${mock.defaultUid}/orgs/photos`)
            .query({
                aggr: true,
                limit: defaultQuery.limit,
                offset: defaultQuery.offset
            })
            .reply(200, mock.ugcResponse);

        nock(searchHost)
            .get('/yandsearch')
            .query({
                lang: 'ru_RU',
                results: 2,
                snippets: 'photos/1.x,cluster_permalinks',
                business_oid: ['123', '132002434175'],
                ms: 'pb',
                origin: 'personal-account-oid'
            })
            .reply(200, mock.geoSearchResponse);
    });

    it('should return 200', async () => {
        const response = await client.get(`${url}/v1/photo`);
        expect(response.statusCode).toEqual(200);
        expect(response.body).toEqual(mock.defaultResponse);
        validateResponseBySchema(response);
    });

    describe('parameters checking', () => {
        it('should return 401 if "X-Ya-User-Ticket" header is missed', async () => {
            const response = await client.get(`${url}/v1/photo`, {
                headers: {
                    ...defaultHeaders,
                    'X-Ya-User-Ticket': ''
                }
            });
            expect(response.statusCode).toEqual(401);
            validateResponseBySchema(response);
        });

        it('should return 403 if "X-Ya-Service-Ticket" header is wrong', async () => {
            const response = await client.get(`${url}/v1/photo`, {
                headers: {
                    ...defaultHeaders,
                    'X-Ya-Service-Ticket': WRONG_SRC_SERVICE_TICKET
                }
            });
            expect(response.statusCode).toEqual(403);
            validateResponseBySchema(response);
        });

        it('should return 401 if "X-Ya-Service-Ticket" header is missed', async () => {
            const response = await client.get(`${url}/v1/photo`, {
                headers: {
                    ...defaultHeaders,
                    'X-Ya-Service-Ticket': ''
                }
            });
            expect(response.statusCode).toEqual(401);
            validateResponseBySchema(response);
        });

        it('should return 400 if "origin" parameter is missed', async () => {
            const response = await client.get(`${url}/v1/photo`, {
                query: {
                    ...defaultQuery,
                    origin: ''
                }
            });
            expect(response.statusCode).toEqual(400);
            validateResponseBySchema(response);
        });

        it('should return 400 if "offset" parameter is invalid', async () => {
            const response = await client.get(`${url}/v1/photo`, {
                query: {
                    ...defaultQuery,
                    offset: -10
                }
            });
            expect(response.statusCode).toEqual(400);
            validateResponseBySchema(response);
        });

        it('should return 400 if "limit" parameter is invalid', async () => {
            const response = await client.get(`${url}/v1/photo`, {
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
    const validateResponse = validator.validateResponse('get', '/v1/photo');
    validateResponse(response);
}

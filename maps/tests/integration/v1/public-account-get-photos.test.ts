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
    WRONG_SRC_SERVICE_TICKET,
    CORRECT_SRC_SERVICE_TICKET
} from 'tests/integration/tvm-tickets';
import * as mock from 'tests/integration/v1/data/photo';

const specDoc = JSON.parse(fs.readFileSync(path.resolve('./out/generated/spec.json'), 'utf-8'));
const validator = new OpenApiValidator(specDoc);

const defaultQuery: Readonly<Record<string, any>> = {
    lang: mock.defaultRequest.lang,
    limit: mock.defaultRequest.limit,
    offset: mock.defaultRequest.offset,
    origin: mock.defaultRequest.origin,
    publicId: 'AAAZ_Q_Q_'
};

const defaultHeaders = {
    'X-Ya-Service-Ticket': CORRECT_SRC_SERVICE_TICKET,
    'X-Forwarded-For-Y': '::ffff:127.0.0.1'
};

const client = extend({
    throwHttpErrors: false,
    retry: 0,
    timeout: 1500,
    json: true,
    query: defaultQuery,
    headers: defaultHeaders
});

const blackboxResponse = {
    users: [
        {
            uid: {
                value: mock.defaultUid
            }
        }
    ]
};

describe('GET /v1/public_account/photos', () => {
    let server: http.Server;
    let url: string;
    let ugcSearch: string;
    let blackboxHost: string;
    let searchHost: string;

    beforeAll(async () => {
        const hosts = await intHostConfigLoader.get();
        ugcSearch = hosts.ugcSearch;
        searchHost = hosts.search;
        blackboxHost = hosts.blackbox;

        [server, url] = await startServer(app);
        nock.disableNetConnect();
        nock.enableNetConnect(/localhost/);
    });

    afterAll(async () => {
        await stopServer(server);
        nock.enableNetConnect();
    });

    beforeEach(() => {
        nock(blackboxHost)
            .get('/')
            .query({
                userip: '::ffff:127.0.0.1',
                method: 'userinfo',
                public_id: defaultQuery.publicId,
                format: 'json'
            })
            .reply(200, blackboxResponse);

        nock(ugcSearch)
            .get(`/api/v1/users/${mock.defaultUid}/orgs/photos`)
            .query({
                aggr: true,
                limit: mock.defaultRequest.limit,
                offset: mock.defaultRequest.offset,
                public: '1'
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

    afterEach(() => {
        nock.cleanAll();
    });

    describe('response validation', () => {
        it('should validate success response', async () => {
            const response = await client.get(`${url}/v1/public_account/photos`);

            expect(response.statusCode).toEqual(200);
            expect(response.body).toEqual(mock.defaultResponse);
            validateResponseBySchema(response);
        });
    });

    describe('parameters checking', () => {
        it('should return 403 if "X-Ya-Service-Ticket" header is wrong', async () => {
            const response = await client.get(`${url}/v1/public_account/photos`, {
                headers: {
                    ...defaultHeaders,
                    'X-Ya-Service-Ticket': WRONG_SRC_SERVICE_TICKET
                }
            });
            expect(response.statusCode).toEqual(403);
            validateResponseBySchema(response);
        });

        it('should return 401 if "X-Ya-Service-Ticket" header is missed', async () => {
            const response = await client.get(`${url}/v1/public_account/photos`, {
                headers: {
                    ...defaultHeaders,
                    'X-Ya-Service-Ticket': ''
                }
            });
            expect(response.statusCode).toEqual(401);
            validateResponseBySchema(response);
        });

        it('should return 400 if parameters were missed', async () => {
            const response = await client.get(`${url}/v1/public_account/photos`, {
                query: {
                    origin: ''
                }
            });

            expect(response.statusCode).toEqual(400);
            validateResponseBySchema(response);
        });

        it('should return 400 if "offset" parameter is invalid', async () => {
            const response = await client.get(`${url}/v1/public_account/photos`, {
                query: {
                    ...mock.defaultRequest,
                    offset: -10
                }
            });
            expect(response.statusCode).toEqual(400);
            validateResponseBySchema(response);
        });

        it('should return 400 if "limit" parameter is invalid', async () => {
            const response = await client.get(`${url}/v1/public_account/photos`, {
                query: {
                    ...mock.defaultRequest,
                    limit: -10
                }
            });
            expect(response.statusCode).toEqual(400);
            validateResponseBySchema(response);
        });
    });
});

function validateResponseBySchema(response: Response<any>) {
    const validateResponse = validator.validateResponse('get', '/v1/public_account/photos');
    validateResponse(response);
}

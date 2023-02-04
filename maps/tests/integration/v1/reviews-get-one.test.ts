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
import {
    UID123_USER_TICKET,
    CORRECT_SRC_SERVICE_TICKET,
    WRONG_SRC_SERVICE_TICKET
} from 'tests/integration/tvm-tickets';

const specDoc = JSON.parse(fs.readFileSync(path.resolve('./out/generated/spec.json'), 'utf-8'));
const validator = new OpenApiValidator(specDoc);

const defaultOrgId = 123;
const defaultReviewId = 123456;
const defaultHeaders = {
    'X-Ya-User-Ticket': UID123_USER_TICKET,
    'X-Ya-Service-Ticket': CORRECT_SRC_SERVICE_TICKET
};

const defaultQuery: any = {
    org_id: defaultOrgId
};

const client = extend({
    throwHttpErrors: false,
    retry: 0,
    timeout: 1500,
    json: true,
    query: defaultQuery,
    headers: defaultHeaders
});

describe('GET /v1/reviews/:review_id', () => {
    let server: http.Server;
    let url: string;
    let ugcSearch: string;

    beforeAll(async () => {
        const hosts = await intHostConfigLoader.get();
        ugcSearch = hosts.ugcSearch;

        [server, url] = await startServer(app);
        nock.disableNetConnect();
        nock.enableNetConnect(/localhost/);
    });

    afterAll(async () => {
        await stopServer(server);
        nock.enableNetConnect();
    });

    beforeEach(() => {
        nock(ugcSearch)
            .get(`/v1/orgs/${defaultOrgId}/get-review`)
            .query({review_id: defaultReviewId})
            .reply(200, ugcMock.onlyFirst.Reviews[0]);
    });

    it('should return 200 if parameters is correct', async () => {
        const response = await client.get(`${url}/v1/reviews/${defaultReviewId}`);
        expect(response.statusCode).toEqual(200);
        validateResponseBySchema(response);
    });

    describe('parameters checking', () => {
        it('should return 401 if "X-Ya-User-Ticket" header is missed', async () => {
            const response = await client.get(`${url}/v1/reviews/${defaultReviewId}`, {
                headers: {
                    ...defaultHeaders,
                    'X-Ya-User-Ticket': ''
                }
            });
            expect(response.statusCode).toEqual(401);
            validateResponseBySchema(response);
        });

        it('should return 401 if "X-Ya-Service-Ticket" is missed', async () => {
            const response = await client.get(`${url}/v1/reviews/${defaultReviewId}`, {
                headers: {
                    ...defaultHeaders,
                    'X-Ya-Service-Ticket': ''
                }
            });
            expect(response.statusCode).toEqual(401);
            validateResponseBySchema(response);
        });

        it('should return 403 if "X-Ya-Service-Ticket" is wrong', async () => {
            const response = await client.get(`${url}/v1/reviews/${defaultReviewId}`, {
                headers: {
                    ...defaultHeaders,
                    'X-Ya-Service-Ticket': WRONG_SRC_SERVICE_TICKET
                }
            });
            expect(response.statusCode).toEqual(403);
            validateResponseBySchema(response);
        });

        it('should return 400 if "org_id" parameter is invalid', async () => {
            const response = await client.get(`${url}/v1/reviews/${defaultReviewId}`, {
                query: {
                    ...defaultQuery,
                    org_id: ''
                }
            });
            expect(response.statusCode).toEqual(400);
            validateResponseBySchema(response);
        });
    });
});

function validateResponseBySchema(response: Response<any>) {
    const validateResponse = validator.validateResponse('get', `/v1/reviews/{review_id}`);
    validateResponse(response);
}

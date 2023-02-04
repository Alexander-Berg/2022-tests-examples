import * as http from 'http';
import * as path from 'path';
import * as fs from 'fs';
import * as nock from 'nock';
import {extend, Response} from 'got';
import {OpenApiValidator} from 'express-openapi-validate';
import {app} from 'app/app';
import {intHostConfigLoader} from 'app/lib/host-loader';
import {startServer, stopServer} from 'tests/integration/test-server';
import {editAccessData} from 'tests/integration/v1/data/public-account';
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

const defaultBody = {
    meta: {},
    data: {
        is_enabled: true
    }
};

const client = extend({
    throwHttpErrors: false,
    retry: 0,
    timeout: 1500,
    json: true,
    body: defaultBody,
    headers: defaultHeaders
});

describe('POST /v1/public_account/edit_access', () => {
    let server: http.Server;
    let url: string;
    let ugcSearch: string;

    beforeAll(async () => {
        const intHosts = await intHostConfigLoader.get();
        ugcSearch = intHosts.ugcSearch;

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
            .patch(`/v1/users/${defaultUid}/user-settings/1`)
            .reply(200, {});
    });

    afterEach(() => {
        nock.cleanAll();
    });

    describe('response validation', () => {
        it('should validate success response', async () => {
            const response = await client.post(`${url}/v1/public_account/edit_access`);

            expect(response.statusCode).toEqual(200);
            expect(response.body).toEqual(editAccessData.answer);
            validateResponseBySchema(response);
        });
    });

    describe('parameters checking', () => {
        it('should return 401 if "X-Ya-User-Ticket" header is missed', async () => {
            const response = await client.post(`${url}/v1/public_account/edit_access`, {
                headers: {
                    ...defaultHeaders,
                    'X-Ya-User-Ticket': ''
                }
            });
            expect(response.statusCode).toEqual(401);
            validateResponseBySchema(response);
        });

        it('should return 403 if "X-Ya-Service-Ticket" header is wrong', async () => {
            const response = await client.post(`${url}/v1/public_account/edit_access`, {
                headers: {
                    ...defaultHeaders,
                    'X-Ya-Service-Ticket': WRONG_SRC_SERVICE_TICKET
                }
            });
            expect(response.statusCode).toEqual(403);
            validateResponseBySchema(response);
        });

        it('should return 401 if "X-Ya-Service-Ticket" header is missed', async () => {
            const response = await client.post(`${url}/v1/public_account/edit_access`, {
                headers: {
                    ...defaultHeaders,
                    'X-Ya-Service-Ticket': ''
                }
            });
            expect(response.statusCode).toEqual(401);
            validateResponseBySchema(response);
        });

        it('should return 400 if "is_enabled" parameter is missed', async () => {
            const response = await client.post(`${url}/v1/public_account/edit_access`, {
                body: {
                    ...defaultBody,
                    data: {
                        is_enabled: ''
                    }
                }
            });
            expect(response.statusCode).toEqual(400);
            validateResponseBySchema(response);
        });
    });
});

function validateResponseBySchema(response: Response<any>) {
    const validateResponse = validator.validateResponse('post', '/v1/public_account/edit_access');
    validateResponse(response);
}

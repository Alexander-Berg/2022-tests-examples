import * as http from 'http';
import * as path from 'path';
import * as fs from 'fs';
import * as nock from 'nock';
import {extend} from 'got';
import {OpenApiValidator} from 'express-openapi-validate';
import {app} from 'app/app';
import {intHostConfigLoader} from 'app/lib/host-loader';
import {startServer, stopServer} from 'tests/integration/test-server';
import {
    UID123_USER_TICKET,
    WRONG_SRC_SERVICE_TICKET,
    CORRECT_SRC_SERVICE_TICKET
} from 'tests/integration/tvm-tickets';

const specDoc = JSON.parse(fs.readFileSync(path.resolve('./out/generated/spec.json'), 'utf-8'));
const validator = new OpenApiValidator(specDoc);

const defaultUid = 123;
const defaultHeaders = {
    'X-Ya-User-Ticket': UID123_USER_TICKET,
    'X-Ya-Service-Ticket': CORRECT_SRC_SERVICE_TICKET
};

const org_id = {
    right: 123456,
    wrong: 666
};

const defaultBody = {
    meta: {},
    data: {
        org_id: org_id.right,
        photo_id: 'test_photo_id_1'
    }
};

const client = extend({
    throwHttpErrors: false,
    retry: 0,
    timeout: 1500,
    json: true,
    headers: defaultHeaders,
    body: defaultBody
});

describe('POST /v1/photo/delete', () => {
    let server: http.Server;
    let url: string;
    let ugcSearch: string;
    let altayPhotoApi: string;

    beforeAll(async () => {
        const hosts = await intHostConfigLoader.get();
        ugcSearch = hosts.ugcSearch;
        altayPhotoApi = hosts.altayPhotoApi;

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
            .delete(`/v1/users/${defaultUid}/orgs/${org_id.right}/photos/${defaultBody.data.photo_id}`)
            .reply(200);

        nock(altayPhotoApi)
            .post('/photo/delete')
            .query({
                permalink: org_id.right,
                uid: defaultUid,
                photo_id: defaultBody.data.photo_id
            })
            .reply(200);

        nock(ugcSearch)
            .delete(`/v1/users/${defaultUid}/orgs/${org_id.wrong}/photos/${defaultBody.data.photo_id}`)
            .reply(404);
    });

    afterEach(() => {
        nock.cleanAll();
    });

    it('should pass success response', async () => {
        const response = await client.post(`${url}/v1/photo/delete`);

        expect(response.statusCode).toEqual(200);
        validator.validateResponse('post', '/v1/photo/delete')(response);
    });

    it('should return 401 if "X-Ya-User-Ticket" header is missed', async () => {
        const response = await client.post(`${url}/v1/photo/delete`, {
            body: defaultBody,
            headers: {
                ...defaultHeaders,
                'X-Ya-User-Ticket': ''
            }
        });
        expect(response.statusCode).toEqual(401);
    });

    it('should return 403 if "X-Ya-Service-Ticket" header is wrong', async () => {
        const response = await client.post(`${url}/v1/photo/delete`, {
            headers: {
                ...defaultHeaders,
                'X-Ya-Service-Ticket': WRONG_SRC_SERVICE_TICKET
            }
        });
        expect(response.statusCode).toEqual(403);
    });

    it('should return 401 if "X-Ya-Service-Ticket" header is missed', async () => {
        const response = await client.post(`${url}/v1/photo/delete`, {
            headers: {
                ...defaultHeaders,
                'X-Ya-Service-Ticket': ''
            }
        });
        expect(response.statusCode).toEqual(401);
    });

    it('should validate error response', async () => {
        const response = await client.post(`${url}/v1/photo/delete`, {
            body: {
                meta: {},
                data: {
                    org_id: ''
                }
            }
        });

        expect(response.statusCode).toEqual(400);
        validator.validateResponse('post', '/v1/photo/delete')(response);
    });

    it('should handle wrong org_id', async () => {
        const response = await client.post(`${url}/v1/photo/delete`, {
            body: {
                ...defaultBody,
                meta: {
                    ...defaultBody.meta,
                    org_id: org_id.wrong
                }
            }
        });

        expect(response.statusCode).toEqual(400);
        validator.validateResponse('post', '/v1/photo/delete')(response);
    });
});

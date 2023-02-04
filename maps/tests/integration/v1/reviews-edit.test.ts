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
    CORRECT_SRC_SERVICE_TICKET,
    WRONG_SRC_SERVICE_TICKET
} from 'tests/integration/tvm-tickets';

const specDoc = JSON.parse(fs.readFileSync(path.resolve('./out/generated/spec.json'), 'utf-8'));
const validator = new OpenApiValidator(specDoc);

const org_id = {
    right: 123456,
    wrong: 666
};

const defaultBody = {
    meta: {},
    data: {
        org_id: org_id.right,
        review_id: 'test_review_id_1',
        is_anonymous: true,
        text: 'test_review_text',
        rating: 3,
        photos: []
    }
};

const defaultHeaders = {
    'X-Ya-User-Ticket': UID123_USER_TICKET,
    'X-Ya-Service-Ticket': CORRECT_SRC_SERVICE_TICKET
};

const client = extend({
    throwHttpErrors: false,
    retry: 0,
    timeout: 1500,
    json: true,
    body: defaultBody,
    headers: defaultHeaders
});

describe('POST /v1/reviews/edit', () => {
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
            .post(`/v1/orgs/${org_id.right}/edit-my-review?review_id=${defaultBody.data.review_id}`)
            .reply(200, {
                Type: 'YANDEX',
                ReviewId: 'test_review_id_2',
                IsAnonymous: true,
                Text: defaultBody.data.text,
                Snippet: '',
                Moderation: {
                    Status: 'IN_PROGRESS',
                    DeclineReason: ''
                },
                Rating: defaultBody.data.rating,
                LikeCount: 0,
                DislikeCount: 0,
                SkipCount: 0,
                UserReaction: 'NONE',
                Photos: defaultBody.data.photos.map(
                    (photo: any) => ({
                        ...photo,
                        Moderation: {
                            Status: 'IN_PROGRESS',
                            DeclineReason: ''
                        }
                    })
                ),
                OrgId: defaultBody.data.org_id,
                UpdatedTime: '2019-01-29T17:25:24.837Z'
            });

        nock(ugcSearch)
            .post(`/v1/orgs/${org_id.wrong}/edit-my-review?review_id=${defaultBody.data.review_id}`)
            .reply(404);
    });

    afterEach(() => {
        nock.cleanAll();
    });

    it('should pass success response', async () => {
        const response = await client.post(`${url}/v1/reviews/edit`);
        expect(response.statusCode).toEqual(200);
        validator.validateResponse('post', '/v1/reviews/edit')(response);
    });

    it('should validate error response', async () => {
        const response = await client.post(`${url}/v1/reviews/edit`, {
            body: {
                data: {
                    org_id: ''
                }
            }
        });
        expect(response.statusCode).toEqual(400);
        validator.validateResponse('post', '/v1/reviews/edit')(response);
    });

    it('should return 401 if "X-Ya-User-Ticket" header is missed', async () => {
        const response = await client.post(`${url}/v1/reviews/edit`, {
            headers: {
                ...defaultHeaders,
                'X-Ya-User-Ticket': ''
            }
        });
        expect(response.statusCode).toEqual(401);
    });

    it('should return 401 if "X-Ya-Service-Ticket" is missed', async () => {
        const response = await client.post(`${url}/v1/reviews/edit`, {
            headers: {
                ...defaultHeaders,
                'X-Ya-Service-Ticket': ''
            }
        });
        expect(response.statusCode).toEqual(401);
    });

    it('should return 403 if "X-Ya-Service-Ticket" is wrong', async () => {
        const response = await client.post(`${url}/v1/reviews/edit`, {
            headers: {
                ...defaultHeaders,
                'X-Ya-Service-Ticket': WRONG_SRC_SERVICE_TICKET
            }
        });
        expect(response.statusCode).toEqual(403);
    });

    it('should handle wrong org_id', async () => {
        const response = await client.post(`${url}/v1/reviews/edit`, {
            body: {
                ...defaultBody,
                meta: {
                    ...defaultBody.meta,
                    org_id: org_id.wrong
                }
            }
        });
        expect(response.statusCode).toEqual(400);
        validator.validateResponse('post', '/v1/reviews/edit')(response);
    });
});

import * as http from 'http';
import * as path from 'path';
import * as fs from 'fs';
import * as nock from 'nock';
import {extend, Response} from 'got';
import {OpenApiValidator} from 'express-openapi-validate';
import {app} from 'app/app';
import {intHostConfigLoader} from 'app/lib/host-loader';
import {config} from 'app/config';
import {startServer, stopServer} from 'tests/integration/test-server';
import impressionsPollsMock from 'tests/integration/v3/data/impressions-polls';
import impressionsPollsGeosearchMock from 'tests/integration/v2/data/impressions-polls-geosearch';
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

const defaultBodyMeta = {
    uuid: 'b1c614bbbf88e74c2fd01da372af6863',
    device_id: '38c285c4777ea5d67cafa169df3bce6a'
};

const params = {
    orgId: '321',
    otype: config.ugcPullParams.otype,
    appId: config.ugcPullParams.appId,
    simpleQuestionId: 'feature_123'
};

const defaultQuery = {
    origin: 'origin',
    lang: 'ru_RU'
};

const client = extend({
    throwHttpErrors: false,
    retry: 0,
    timeout: 1500,
    json: true,
    query: defaultQuery,
    headers: defaultHeaders
});

describe('/v3/impressions', () => {
    let server: http.Server;
    let url: string;
    let ugcSearchHost: string;
    let searchHost: string;

    beforeAll(async () => {
        const hosts = await intHostConfigLoader.get();
        ugcSearchHost = hosts.ugcSearch;
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
        nock(ugcSearchHost)
            .get(`/api/users/${defaultUid}/polls`)
            .query({
                app_id: params.appId,
                otype: params.otype,
                lang: defaultQuery.lang,
                poll_type: ['OBJECT_QUESTIONS', 'OBJECT_REVIEW', 'OBJECT_SBS']
            })
            .reply(200, impressionsPollsMock.raw);

        nock(ugcSearchHost)
            .put(`/v1/users/${defaultUid}/orgs/${params.orgId}/answers/${params.simpleQuestionId}`, {
                QuestionId: params.simpleQuestionId,
                Answers: ['no']
            })
            .reply(200);

        nock(searchHost)
            .get('/yandsearch')
            .query({
                lang: defaultQuery.lang,
                origin: 'personal-account-oid',
                ms: 'pb',
                snippets: 'photos/1.x,cluster_permalinks',
                results: '5',
                business_oid: [1095559046, 1080404160, 66666, 1091716672, 1664687615]
            })
            .reply(200, impressionsPollsGeosearchMock);
    });

    describe('GET /', () => {
        function validateResponseBySchema(response: Response<any>) {
            const validateResponse = validator.validateResponse('get', '/v3/impressions');
            validateResponse(response);
        }

        it('should return 200', async () => {
            const response = await client.get(`${url}/v3/impressions`);

            expect(response.body).toEqual({
                meta: {
                    lang: defaultQuery.lang
                },
                data: impressionsPollsMock.response
            });
            expect(response.statusCode).toEqual(200);
            validateResponseBySchema(response);
        });

        it('should return 400 if required query params doesn\'t exist', async () => {
            const response = await client.get(`${url}/v3/impressions`, {
                query: {
                    lang: defaultQuery.lang,
                    origin: '',
                    orgId: ''
                }
            });

            expect(response.statusCode).toEqual(400);
            validateResponseBySchema(response);
        });

        it('should return 401 if "X-Ya-User-Ticket" header is missed', async () => {
            const response = await client.get(`${url}/v3/impressions`, {
                headers: {
                    ...defaultHeaders,
                    'X-Ya-User-Ticket': ''
                }
            });
            expect(response.statusCode).toEqual(401);
            validateResponseBySchema(response);
        });

        it('should return 403 if "X-Ya-Service-Ticket" header is wrong', async () => {
            const response = await client.get(`${url}/v3/impressions`, {
                headers: {
                    ...defaultHeaders,
                    'X-Ya-Service-Ticket': WRONG_SRC_SERVICE_TICKET
                }
            });
            expect(response.statusCode).toEqual(403);
            validateResponseBySchema(response);
        });

        it('should return 401 if "X-Ya-Service-Ticket" header is missed', async () => {
            const response = await client.get(`${url}/v3/impressions`, {
                headers: {
                    ...defaultHeaders,
                    'X-Ya-Service-Ticket': ''
                }
            });
            expect(response.statusCode).toEqual(401);
            validateResponseBySchema(response);
        });
    });

    describe('POST /answer_simple_question', () => {
        function validateResponseBySchema(response: Response<any>) {
            const validateResponse = validator.validateResponse('post', '/v3/impressions/answer_simple_question');
            validateResponse(response);
        }

        it('should return 200 with "answer" field', async () => {
            const response = await client.post(`${url}/v3/impressions/answer_simple_question`, {
                body: {
                    meta: defaultBodyMeta,
                    data: {
                        org_id: params.orgId,
                        question_id: params.simpleQuestionId,
                        answer: 'no'
                    }
                }
            });

            validateResponseBySchema(response);
            expect(response.statusCode).toEqual(200);
        });

        it('should return 401 if "X-Ya-User-Ticket" header is missed', async () => {
            const response = await client.post(`${url}/v3/impressions/answer_simple_question`, {
                headers: {
                    ...defaultHeaders,
                    'X-Ya-User-Ticket': ''
                }
            });
            expect(response.statusCode).toEqual(401);
            validateResponseBySchema(response);
        });

        it('should return 403 if "X-Ya-Service-Ticket" header is wrong', async () => {
            const response = await client.post(`${url}/v3/impressions/answer_simple_question`, {
                body: {
                    meta: defaultBodyMeta,
                    data: {
                        org_id: params.orgId,
                        question_id: params.simpleQuestionId,
                        answer: false
                    }
                },
                headers: {
                    ...defaultHeaders,
                    'X-Ya-Service-Ticket': WRONG_SRC_SERVICE_TICKET
                }
            });
            expect(response.statusCode).toEqual(403);
            validateResponseBySchema(response);
        });

        it('should return 401 if "X-Ya-Service-Ticket" header is missed', async () => {
            const response = await client.post(`${url}/v3/impressions/answer_simple_question`, {
                body: {
                    meta: defaultBodyMeta,
                    data: {
                        org_id: params.orgId,
                        question_id: params.simpleQuestionId,
                        answer: false
                    }
                },
                headers: {
                    ...defaultHeaders,
                    'X-Ya-Service-Ticket': ''
                }
            });
            expect(response.statusCode).toEqual(401);
            validateResponseBySchema(response);
        });

        it('should return 400 if required params doesn\'t exist', async () => {
            const response = await client.post(`${url}/v3/impressions/answer_simple_question`, {
                body: {
                    meta: defaultBodyMeta,
                    data: {
                        org_id: params.orgId
                    }
                }
            });

            expect(response.statusCode).toEqual(400);
            validateResponseBySchema(response);
        });

        it('should return 400 if in body exist "skip" & "answer" in same time', async () => {
            const response = await client.post(`${url}/v3/impressions/answer_simple_question`, {
                body: {
                    meta: defaultBodyMeta,
                    data: {
                        org_id: params.orgId,
                        skip: true,
                        answer: 'no'
                    }
                }
            });

            expect(response.statusCode).toEqual(400);
            validateResponseBySchema(response);
        });
    });
});

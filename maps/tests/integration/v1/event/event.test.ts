import * as http from 'http';
import nock from 'nock';
import {promises} from 'fs';
import * as got from 'got';
import {app} from 'app/app';
import {TestDb} from 'tests/test-db';
import {fixtures} from 'tests/fixtures/db';
import {startServer, stopServer} from 'tests/test-server';
import {createResponseSchemaValidator} from 'tests/response-schema-validator';
import {ResponseBody} from 'tests/types';
import {intHostConfigLoader} from 'app/lib/host-loader';
import {createEventJson} from 'tests/fixtures/integration/v1/event/create-event';

const client = got.default.extend({
    throwHttpErrors: false,
    retry: 0,
    timeout: 2000,
    responseType: 'json'
});

const ORG_ID = 1094008369;

describe('public api', () => {
    const testDb = new TestDb();
    let server: http.Server;
    let url: string;

    beforeAll(async () => {
        [server, url] = await startServer(app);
        nock.disableNetConnect();
        nock.enableNetConnect(/localhost/);
    });

    afterAll(async () => {
        await stopServer(server);
        nock.enableNetConnect();
    });

    beforeEach(async () => {
        await testDb.clean();
        await testDb.loadFixtures(fixtures);
    });

    afterEach(() => {
        nock.cleanAll();
    });

    describe('/v1/event', () => {
        const validateResponseSchema = createResponseSchemaValidator({
            path: '/v1/event',
            method: 'get'
        });

        it('should return correct response', async () => {
            const id = 'onlayn-pokaz-spektaklyagamlet';
            const response = await client.get<ResponseBody>(`${url}/v1/event`, {
                searchParams: {
                    id,
                    branch: 'draft'
                }
            });
            expect(response.statusCode).toEqual(200);
            expect(response.body.id).toEqual(id);
            validateResponseSchema(response);
        });

        it('should return correct 404 response', async () => {
            const response = await client.get<ResponseBody>(`${url}/v1/event`, {
                searchParams: {
                    id: 'not-exist'
                }
            });

            expect(response.statusCode).toEqual(404);
        });

        it('should return bad request', async () => {
            const response = await client.get<ResponseBody>(`${url}/v1/event`, {
                searchParams: {
                    id: '',
                    branch: 'invalid'
                }
            });

            expect(response.statusCode).toEqual(400);
            validateResponseSchema(response);
        });
    });

    describe('/v1/event/search_by_point', () => {
        const validateResponseSchema = createResponseSchemaValidator({
            path: '/v1/event/search_by_point',
            method: 'get'
        });

        it('should return correct response', async () => {
            const {search} = await intHostConfigLoader.get();
            nock(search)
                .get('/yandsearch')
                .query({
                    origin: 'discovery',
                    lang: 'ru_RU',
                    type: 'biz',
                    format: 'json',
                    snippets: [
                        'businessrating/1.x',
                        'masstransit/1.x',
                        'photos/2.x',
                        'cluster_permalinks',
                        'promo_mastercard/1.x:mastercardoffers'
                    ].join(','),
                    ms: 'pb',
                    business_oid: ORG_ID
                })
                .reply(200, await promises.readFile(`src/tests/fixtures/backend/organizations/${ORG_ID}.protobuf`));

            await Promise.all(
                [
                    createEventJson.withUrlMinReqField,
                    createEventJson.withImagesAndFeatures,
                    createEventJson.withButtons,
                    createEventJson.withOidMinReqField
                ].map((json) =>
                    client
                        .post<ResponseBody>(`${url}/v1/edit/event/create`, {
                            json
                        })
                        .catch()
                )
            );

            const response = await client.get<ResponseBody>(`${url}/v1/event/search_by_point`, {
                searchParams: {
                    lon: 37,
                    lat: 55,
                    branch: 'draft'
                }
            });

            expect(response.statusCode).toEqual(200);
            expect(response.body.events.length).toBeGreaterThan(0);
            validateResponseSchema(response);
        });

        it('should return correct empty response', async () => {
            const response = await client.get<ResponseBody>(`${url}/v1/event/search_by_point`, {
                searchParams: {
                    lon: 0,
                    lat: 0
                }
            });

            expect(response.statusCode).toEqual(200);
            expect(response.body.events.length).toEqual(0);
            validateResponseSchema(response);
        });

        it('should return bad request', async () => {
            const response = await client.get<ResponseBody>(`${url}/v1/event/search_by_point`, {
                searchParams: {
                    lon: 37,
                    lat: 555555
                }
            });

            expect(response.statusCode).toEqual(400);
            validateResponseSchema(response);
        });
    });
});

import * as http from 'http';
import * as got from 'got';
import nock from 'nock';
import {app} from 'app/app';
import {TestDb} from 'tests/test-db';
import {fixtures} from 'tests/fixtures/db';
import {startServer, stopServer} from 'tests/test-server';
import {createResponseSchemaValidator} from 'tests/response-schema-validator';
import {ResponseBody} from 'tests/types';

const client = got.default.extend({
    throwHttpErrors: false,
    retry: 0,
    timeout: 2000,
    responseType: 'json'
});

describe('/v3/story', () => {
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

    describe('/v3/story', () => {
        const validateResponseSchema = createResponseSchemaValidator({path: '/v3/story'});

        it('should return story', async () => {
            const response = await client.get<ResponseBody>(`${url}/v3/story`, {
                searchParams: {
                    id: '671c0cdf-1283-4e15-9f68-bad87c174070',
                    origin: 'test',
                    lang: 'ru_RU'
                }
            });

            expect(response.statusCode).toEqual(200);
            expect(response.body.screens.length).toBeGreaterThan(0);

            validateResponseSchema(response);
        });

        it('should not return story with unresolved video', async () => {
            const response = await client.get<ResponseBody>(`${url}/v3/story`, {
                searchParams: {
                    // Story with this ID has unprepeared video
                    id: '6f37895b-ecc9-4347-aa27-d08a7b6c9c8d',
                    origin: 'test',
                    lang: 'ru_RU'
                }
            });

            expect(response.statusCode).toEqual(404);
        });
    });
});

import * as http from 'http';
import * as got from 'got';
import nock from 'nock';
import {app} from 'app/app';
import {TestDb} from 'tests/test-db';
import {fixtures} from 'tests/fixtures/db';
import {startServer, stopServer} from 'tests/test-server';
import {createResponseSchemaValidator} from 'tests/response-schema-validator';

const client = got.default.extend({
    throwHttpErrors: false,
    retry: 0,
    timeout: 2000,
    responseType: 'json'
});

describe('/v1/page', () => {
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

    describe('/v1/page', () => {
        const validateResponseSchema = createResponseSchemaValidator({path: '/v1/page'});

        it('should return page', async () => {
            const response = await client.get(`${url}/v1/page`, {
                searchParams: {
                    alias: 'alias-for-page-with-id-1',
                    origin: 'test'
                }
            });

            expect(response.statusCode).toEqual(200);
            validateResponseSchema(response);
        });
    });

    describe('/v1/page/aliases', () => {
        const validateResponseSchema = createResponseSchemaValidator({path: '/v1/page/aliases'});

        it('should return list of pages alieases', async () => {
            const response = await client.get(`${url}/v1/page/aliases`);
            expect(response.statusCode).toEqual(200);
            expect(response.body.length).toBeGreaterThan(0);
            validateResponseSchema(response);
        });
    });
});

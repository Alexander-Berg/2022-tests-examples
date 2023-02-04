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

describe('/v2/search', () => {
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

    describe('/v2/search/by_point', () => {
        const validateResponseSchema = createResponseSchemaValidator({path: '/v2/search/by_point'});

        it('should return empty data', async () => {
            const response = await client.get<ResponseBody>(`${url}/v2/search/by_point`, {
                searchParams: {
                    lon: 0,
                    lat: 0,
                    zoom: 11,
                    origin: 'mobile',
                    lang: 'ru_RU'
                }
            });

            expect(response.statusCode).toEqual(200);

            expect(response.body.rubrics.primary.length).toEqual(0);
            expect(response.body.rubrics.rest.length).toEqual(0);

            validateResponseSchema(response);
        });
    });
});

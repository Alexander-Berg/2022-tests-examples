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

describe('/v3/search', () => {
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

    describe('/v3/search/intro', () => {
        const validateResponseSchema = createResponseSchemaValidator({path: '/v3/search/intro'});

        it('should return rich data', async () => {
            const response = await client.get<ResponseBody>(`${url}/v3/search/intro`, {
                searchParams: {
                    lon: 37.71245562474962,
                    lat: 55.64792533760475,
                    origin: 'mobile'
                }
            });

            expect(response.statusCode).toEqual(200);
            expect(response.body.items.length).toBeGreaterThan(0);
            expect(response.body.items[0].id).toEqual('c8892b3b-dcbe-444e-b933-a6575d8bcbc4');
            validateResponseSchema(response);
        });

        it('should return rich data when segments in query', async () => {
            const response = await client.get<ResponseBody>(`${url}/v3/search/intro`, {
                searchParams: {
                    lon: 37.71245562474962,
                    lat: 55.64792533760475,
                    'segments[]': 'segment-1',
                    origin: 'mobile'
                }
            });

            expect(response.statusCode).toEqual(200);
            expect(response.body.items.length).toEqual(2);
            validateResponseSchema(response);
        });

        it('should return empty data', async () => {
            const response = await client.get<ResponseBody>(`${url}/v3/search/intro`, {
                searchParams: {
                    lon: 0,
                    lat: 0,
                    origin: 'mobile'
                }
            });

            expect(response.statusCode).toEqual(200);
            expect(response.body.items.length).toEqual(0);
            validateResponseSchema(response);
        });
    });
});

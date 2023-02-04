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

    describe('/v3/search/by_point/notifications', () => {
        const validateResponseSchema = createResponseSchemaValidator({path: '/v3/search/by_point/notifications'});

        it('should return rich data', async () => {
            const response = await client.get<ResponseBody>(`${url}/v3/search/by_point/notifications`, {
                searchParams: {
                    lon: 37.71245562474962,
                    lat: 55.64792533760475,
                    zoom: 11,
                    origin: 'mobile',
                    lang: 'ru_RU'
                }
            });

            expect(response.statusCode).toEqual(200);
            expect(response.body.notifications.length).toBeGreaterThan(0);

            validateResponseSchema(response);
        });

        it('should return rich data when segments in query', async () => {
            const response = await client.get<ResponseBody>(`${url}/v3/search/by_point/notifications`, {
                searchParams: {
                    lon: 37.71245562474962,
                    lat: 55.64792533760475,
                    zoom: 11,
                    'segments[]': 'segment-1',
                    origin: 'mobile',
                    lang: 'ru_RU'
                }
            });

            expect(response.statusCode).toEqual(200);
            expect(response.body.notifications.length).toEqual(2);
            expect(response.body.notifications[0].id).toEqual('f2beead85f8c2k88irgt2');

            validateResponseSchema(response);
        });

        it('should return rich data when x-device-id in headers', async () => {
            const response = await client.get<ResponseBody>(`${url}/v3/search/by_point/notifications`, {
                searchParams: {
                    lon: 37.71245562474962,
                    lat: 55.64792533760475,
                    zoom: 11,
                    origin: 'mobile',
                    lang: 'ru_RU'
                },
                headers: {
                    'x-device-id': 'device-hash-1'
                }
            });

            expect(response.statusCode).toEqual(200);
            expect(response.body.notifications.length).toEqual(2);
            expect(response.body.notifications[0].id).toEqual('f2beead85f8c2k88irgt2');

            validateResponseSchema(response);
        });

        it('should return rich data when deviceId in headers', async () => {
            const response = await client.get<ResponseBody>(`${url}/v3/search/by_point/notifications`, {
                searchParams: {
                    lon: 37.71245562474962,
                    lat: 55.64792533760475,
                    zoom: 11,
                    origin: 'mobile',
                    lang: 'ru_RU'
                },
                headers: {
                    deviceId: 'device-hash-1'
                }
            });

            expect(response.statusCode).toEqual(200);
            expect(response.body.notifications.length).toEqual(2);
            expect(response.body.notifications[0].id).toEqual('f2beead85f8c2k88irgt2');

            validateResponseSchema(response);
        });

        it('should return empty data', async () => {
            const response = await client.get<ResponseBody>(`${url}/v3/search/by_point/notifications`, {
                searchParams: {
                    lon: 0,
                    lat: 0,
                    zoom: 11,
                    origin: 'mobile',
                    lang: 'ru_RU'
                }
            });

            expect(response.statusCode).toEqual(200);
            expect(response.body.notifications.length).toEqual(0);

            validateResponseSchema(response);
        });
    });
});

import * as http from 'http';
import * as got from 'got';
import nock from 'nock';
import {app} from 'app/app';
import {TestDb} from 'tests/test-db';
import {fixtures} from 'tests/fixtures/db';
import {startServer, stopServer} from 'tests/test-server';
import {createResponseSchemaValidator} from 'tests/response-schema-validator';
import {FeedCardType} from 'app/types/consts';
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

    describe('/v3/search/by_point', () => {
        const validateResponseSchema = createResponseSchemaValidator({path: '/v3/search/by_point'});

        it('should return rich data', async () => {
            const response = await client.get<ResponseBody>(`${url}/v3/search/by_point`, {
                searchParams: {
                    lon: 37.71245562474962,
                    lat: 55.64792533760475,
                    zoom: 11,
                    origin: 'mobile',
                    lang: 'ru_RU'
                }
            });

            expect(response.statusCode).toEqual(200);

            expect(
                response.body.selectedStories.find(
                    (story: {id: string}) => story.id === '6f37895b-ecc9-4347-aa27-d08a7b6c9c8d'
                )
            ).toBeUndefined();
            expect(response.body.selectedStories.length).toBeGreaterThan(0);
            expect(response.body.rubrics.length).toBeGreaterThan(0);
            expect(response.body.feedFilters.length).toBeGreaterThan(0);
            expect(response.body.feed.length).toBeGreaterThan(0);
            expect(
                response.body.feed.find((item: {type: FeedCardType}) => item.type === FeedCardType.COLLECTION)
            ).not.toBeUndefined();
            expect(
                response.body.feed.find((item: {type: FeedCardType}) => item.type === FeedCardType.STORY)
            ).not.toBeUndefined();
            // Temporary solution MAPSHTTPAPI-1779, uncomment
            // expect(response.body.categories.length).toBeGreaterThan(0);

            validateResponseSchema(response);
        });

        it('should return empty data', async () => {
            const response = await client.get<ResponseBody>(`${url}/v3/search/by_point`, {
                searchParams: {
                    lon: 0,
                    lat: 0,
                    zoom: 11,
                    origin: 'mobile',
                    lang: 'ru_RU'
                }
            });

            expect(response.statusCode).toEqual(200);

            expect(
                response.body.selectedStories.find(
                    (story: {id: string}) => story.id === '6f37895b-ecc9-4347-aa27-d08a7b6c9c8d'
                )
            ).toBeUndefined();
            expect(response.body.selectedStories.length).toBeGreaterThan(0);
            expect(response.body.rubrics.length).toBeGreaterThan(0);
            // Temporary solution MAPSHTTPAPI-1779, uncomment
            // expect(response.body.categories.length).toBeGreaterThan(0);

            validateResponseSchema(response);
        });
    });
});

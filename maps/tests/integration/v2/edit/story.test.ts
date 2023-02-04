import * as http from 'http';
import Ajv from 'ajv';
import * as got from 'got';
import nock from 'nock';
import {app} from 'app/app';
import {TestDb} from 'tests/test-db';
import {fixtures} from 'tests/fixtures/db';
import {startServer, stopServer} from 'tests/test-server';
import {checkListBodyShape} from 'tests/response-validator';
import {logger} from 'app/lib/logger';
import {Branch} from 'app/types/consts';
import getStory from 'tests/fixtures/integration/v2/story/get-story.json';
import createStory from 'tests/fixtures/integration/v2/story/create-story.json';
import createdStory from 'tests/fixtures/integration/v2/story/created-story.json';
import updateStory from 'tests/fixtures/integration/v2/story/update-story.json';
import updatedStory from 'tests/fixtures/integration/v2/story/updated-story.json';
import GET_STORY_LIST_SCHEMA from 'tests/integration/v2/ajv-schemas/get-story-list.json';
import GET_STORY_LIST_CLIENT_SCHEMA from 'tests/integration/v2/ajv-schemas/get-story-list-client.json';
import {ResponseBody} from 'tests/types';

const ajv = new Ajv({
    nullable: true
});

const client = got.default.extend({
    throwHttpErrors: false,
    retry: 0,
    timeout: 2000,
    responseType: 'json'
});

describe('/v2/edit', () => {
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

    describe('/v2/edit/story', () => {
        it('should return story', async () => {
            const {statusCode, body} = await client.get(`${url}/v2/edit/story`, {
                searchParams: {
                    id: '671c0cdf-1283-4e15-9f68-bad87c174070'
                }
            });

            expect(statusCode).toEqual(200);
            expect(body).toEqual(getStory);
        });

        it('should return 404 if story was not found', async () => {
            const {statusCode} = await client.get(`${url}/v2/edit/story`, {
                searchParams: {
                    id: '7f76ec70-7ff8-41ae-9a5d-aaf4b032224d'
                }
            });

            expect(statusCode).toEqual(404);
        });
    });

    describe('/v2/edit/story/list', () => {
        it('should return stories', async () => {
            const {statusCode, body} = await client.get(`${url}/v2/edit/story/list`);
            expect(statusCode).toEqual(200);
            checkListBodyShape(body);
        });

        it('should return one story', async () => {
            const {statusCode, body} = await client.get<ResponseBody>(`${url}/v2/edit/story/list`, {
                searchParams: {
                    limit: 1
                }
            });

            expect(statusCode).toEqual(200);
            checkListBodyShape(body);

            expect(body.data.length).toEqual(1);
        });

        it('should return one story with offset', async () => {
            const {
                body: {
                    data: [firstItem]
                }
            } = await client.get<ResponseBody>(`${url}/v2/edit/story/list`, {
                searchParams: {
                    limit: 1
                }
            });

            const {
                body,
                body: {
                    data: [secondItem]
                }
            } = await client.get<ResponseBody>(`${url}/v2/edit/story/list`, {
                searchParams: {
                    limit: 1,
                    offset: 1
                }
            });

            checkListBodyShape(body);
            expect(firstItem).not.toBeUndefined();
            expect(secondItem).not.toBeUndefined();
            expect(firstItem).not.toEqual(secondItem);
        });

        it('should return stories with correct schema', async () => {
            const {statusCode, body} = await client.get<ResponseBody>(`${url}/v2/edit/story/list`, {
                searchParams: {
                    filters: '{"geoRegionIds": [2]}'
                }
            });
            expect(statusCode).toEqual(200);
            checkListBodyShape(body);

            expect(body.data.length).toBeGreaterThan(0);

            const valid = ajv.validate(GET_STORY_LIST_SCHEMA, body.data);
            if (!valid) {
                logger.error(ajv.errorsText());
            }

            expect(valid).toBeTruthy();
        });
    });

    describe('/v2/edit/story/list_client', () => {
        it('should return stories', async () => {
            const {statusCode, body} = await client.get<ResponseBody>(`${url}/v2/edit/story/list_client`, {
                searchParams: {
                    geoRegionId: 2,
                    platform: 'ios'
                }
            });
            expect(statusCode).toEqual(200);
            expect(body.data.length).toBeGreaterThan(0);

            const valid = ajv.validate(GET_STORY_LIST_CLIENT_SCHEMA, body.data);
            if (!valid) {
                logger.error(ajv.errorsText());
            }

            expect(valid).toBeTruthy();
        });
    });

    describe('/v2/edit/story/list_empty_client', () => {
        it('should return stories', async () => {
            const {statusCode, body} = await client.get<ResponseBody>(`${url}/v2/edit/story/list_empty_client`, {
                searchParams: {
                    platform: 'ios'
                }
            });
            expect(statusCode).toEqual(200);
            expect(body.data.length).toBeGreaterThan(0);

            const valid = ajv.validate(GET_STORY_LIST_CLIENT_SCHEMA, body.data);
            if (!valid) {
                logger.error(ajv.errorsText());
            }

            expect(valid).toBeTruthy();
        });
    });

    describe('/v2/edit/story/create', () => {
        it('should create story', async () => {
            const {statusCode, body} = await client.post<ResponseBody>(`${url}/v2/edit/story/create`, {
                json: createStory
            });

            const id = body.data.id;

            expect(statusCode).toEqual(201);
            expect(body).toEqual({
                success: true,
                data: {id}
            });

            const {body: createdStoryBody} = await client.get(`${url}/v2/edit/story`, {
                searchParams: {
                    id,
                    branch: Branch.DRAFT
                }
            });

            createdStory.data.story.id = id;
            expect(createdStoryBody).toEqual(createdStory);
        });
    });

    describe('/v2/edit/story/update', () => {
        it('should update story', async () => {
            const createStoryResponse = await client.post<ResponseBody>(`${url}/v2/edit/story/create`, {
                json: createStory
            });

            const id = createStoryResponse.body.data.id;

            const updateStoryResponse = await client.post<ResponseBody>(`${url}/v2/edit/story/update`, {
                json: updateStory,
                searchParams: {id}
            });

            expect(updateStoryResponse.statusCode).toEqual(204);

            const {body: updatedStoryBody} = await client.get(`${url}/v2/edit/story`, {
                searchParams: {
                    id,
                    branch: Branch.DRAFT
                }
            });

            updatedStory.data.story.id = id;
            expect(updatedStoryBody).toEqual(updatedStory);
        });
    });

    describe('/v2/edit/story/delete', () => {
        it('should delete story', async () => {
            const searchParams = {
                id: '671c0cdf-1283-4e15-9f68-bad87c174070'
            };

            const existedStory = await client.get(`${url}/v2/edit/story`, {searchParams});

            expect(existedStory.statusCode).toEqual(200);
            expect(existedStory.body).toEqual(getStory);

            const {statusCode} = await client.post<ResponseBody>(`${url}/v2/edit/story/delete`, {searchParams});
            expect(statusCode).toEqual(204);

            const deletedStory = await client.get(`${url}/v2/edit/story`, {searchParams});
            expect(deletedStory.statusCode).toEqual(404);
        });
    });

    describe('/v2/edit/story/publish', () => {
        it('should publish story', async () => {
            const createResult = await client.post<ResponseBody>(`${url}/v2/edit/story/create`, {
                json: createStory
            });

            expect(createResult.statusCode).toEqual(201);

            const id = createResult.body.data.id;
            const checkResult = await client.get<ResponseBody>(`${url}/v2/edit/story`, {
                searchParams: {
                    id,
                    branch: 'public'
                }
            });
            expect(checkResult.statusCode).toEqual(404);

            const publishResult = await client.post(`${url}/v2/edit/story/publish`, {
                searchParams: {id}
            });
            expect(publishResult.statusCode).toBe(204);

            const {statusCode, body: publishStory} = await client.get<ResponseBody>(`${url}/v2/edit/story`, {
                responseType: 'json',
                searchParams: {
                    id,
                    branch: 'public'
                }
            });

            createdStory.data.story.id = id;
            createdStory.data.story.isPublished = true;

            expect(statusCode).toEqual(200);
            expect(publishStory).toEqual(createdStory);
        });
    });

    describe('/v2/edit/story/unpublish', () => {
        it('should unpublish story', async () => {
            const searchParams = {
                id: '671c0cdf-1283-4e15-9f68-bad87c174070'
            };

            const checkBeforeUnpublish = await client.get<ResponseBody>(`${url}/v2/edit/story`, {searchParams});
            expect(checkBeforeUnpublish.statusCode).toEqual(200);

            const unpublishResult = await client.post(`${url}/v2/edit/story/unpublish`, {searchParams});
            expect(unpublishResult.statusCode).toBe(204);

            const checkAfterUnpublish = await client.get<ResponseBody>(`${url}/v2/edit/story`, {
                searchParams: {
                    ...searchParams,
                    branch: 'public'
                }
            });

            expect(checkAfterUnpublish.statusCode).toEqual(404);
        });
    });
});

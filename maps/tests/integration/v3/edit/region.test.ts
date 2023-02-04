import Ajv from 'ajv';
import * as http from 'http';
import * as got from 'got';
import nock from 'nock';
import {app} from 'app/app';
import {TestDb} from 'tests/test-db';
import {fixtures} from 'tests/fixtures/db';
import {startServer, stopServer} from 'tests/test-server';
import {Branch, FeedType, StoryDisplayLocation} from 'app/types/consts';
import {logger} from 'app/lib/logger';
import createPage from 'tests/fixtures/integration/v1/page/create-page.json';
import createStory from 'tests/fixtures/integration/v2/story/create-story.json';
import getRegion from 'tests/fixtures/integration/v3/region/get-region.json';
import createRegion from 'tests/fixtures/integration/v3/region/create-region.json';
import createdRegion from 'tests/fixtures/integration/v3/region/created-region.json';
import updateRegion from 'tests/fixtures/integration/v3/region/update-region.json';
import updatedRegion from 'tests/fixtures/integration/v3/region/updated-region.json';
import REGION_LIST_SCHEMA from 'tests/integration/v3/ajv-schemas/get-region-list.json';
import {FeedItem} from 'app/types/db/regions-info';
import {ResponseBody} from 'tests/types';

const client = got.default.extend({
    throwHttpErrors: false,
    retry: 0,
    timeout: 2000,
    responseType: 'json'
});

const ajv = new Ajv({
    nullable: true
});

describe('/v3/edit', () => {
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

    describe('/v3/edit/region', () => {
        it('should return region', async () => {
            const {statusCode, body} = await client.get(`${url}/v3/edit/region`, {
                searchParams: {
                    id: 1
                }
            });

            expect(statusCode).toEqual(200);
            expect(body).toEqual(getRegion);
        });

        it('should return 404 if region was not found', async () => {
            const {statusCode} = await client.get(`${url}/v3/edit/region`, {
                searchParams: {
                    id: 9999
                }
            });

            expect(statusCode).toEqual(404);
        });
    });

    describe('/v3/edit/region/list', () => {
        it('should return regions list', async () => {
            const {statusCode, body} = await client.get<ResponseBody>(`${url}/v3/edit/region/list`, {
                searchParams: {
                    limit: 2
                }
            });

            expect(statusCode).toEqual(200);
            expect(body.data.length).toBeGreaterThan(0);

            const valid = ajv.validate(REGION_LIST_SCHEMA, body.data);
            if (!valid) {
                logger.error(ajv.errorsText());
            }

            expect(valid).toBeTruthy();
        });
    });

    describe('/v3/edit/region/create', () => {
        it('should create region', async () => {
            const {statusCode, body} = await client.post<ResponseBody>(`${url}/v3/edit/region/create`, {
                json: createRegion
            });

            const id = body.data.id;

            expect(statusCode).toEqual(201);
            expect(body).toEqual({
                success: true,
                data: {id}
            });

            const {body: createdRegionBody} = await client.get(`${url}/v3/edit/region`, {
                searchParams: {
                    id,
                    branch: Branch.DRAFT
                }
            });

            expect(createdRegionBody).toEqual(createdRegion);
        });
    });

    describe('/v3/edit/region/update', () => {
        it('should update region', async () => {
            const searchParams = {
                id: 1
            };

            const {statusCode} = await client.post(`${url}/v3/edit/region/update`, {
                json: updateRegion,
                searchParams
            });

            expect(statusCode).toEqual(204);

            const {body} = await client.get(`${url}/v3/edit/region`, {
                searchParams: {
                    ...searchParams,
                    branch: Branch.DRAFT
                }
            });

            expect(body).toEqual(updatedRegion);
        });
    });

    describe('/v3/edit/region/delete', () => {
        it('should delete region', async () => {
            const searchParams = {
                id: 1
            };

            const existedRegion = await client.get(`${url}/v3/edit/region`, {searchParams});

            expect(existedRegion.statusCode).toEqual(200);
            expect(existedRegion.body).toEqual(getRegion);

            const {statusCode} = await client.post<ResponseBody>(`${url}/v3/edit/region/delete`, {searchParams});
            expect(statusCode).toEqual(204);

            const deletedRegion = await client.get(`${url}/v3/edit/region`, {searchParams});
            expect(deletedRegion.statusCode).toEqual(404);
        });
    });

    describe('/v3/edit/region/publish', () => {
        it('should publish region', async () => {
            const createResult = await client.post<ResponseBody>(`${url}/v3/edit/region/create`, {
                json: createRegion
            });

            expect(createResult.statusCode).toEqual(201);

            const id = createResult.body.data.id;
            const checkResult = await client.get<ResponseBody>(`${url}/v3/edit/region`, {
                searchParams: {
                    id,
                    branch: 'public'
                }
            });
            expect(checkResult.statusCode).toEqual(404);

            await client.post(`${url}/v3/edit/region/publish`, {
                searchParams: {id}
            });

            const {statusCode, body: publishRegion} = await client.get<ResponseBody>(`${url}/v3/edit/region`, {
                responseType: 'json',
                searchParams: {
                    id,
                    branch: 'public'
                }
            });
            expect(statusCode).toEqual(200);

            createdRegion.data.isPublished = true;
            expect(publishRegion).toEqual(createdRegion);
            createdRegion.data.isPublished = false;
        });
    });

    describe('/v3/edit/region/unpublish', () => {
        it('should unpublish region', async () => {
            const searchParams = {
                id: 1,
                branch: 'public'
            };

            const checkBeforeUnpublish = await client.get<ResponseBody>(`${url}/v3/edit/region`, {searchParams});
            expect(checkBeforeUnpublish.statusCode).toEqual(200);

            await client.post(`${url}/v3/edit/region/unpublish`, {
                searchParams: {
                    id: searchParams.id
                }
            });

            const checkAfterUnpublish = await client.get<ResponseBody>(`${url}/v3/edit/region`, {searchParams});
            expect(checkAfterUnpublish.statusCode).toEqual(404);
        });
    });

    describe('/v3/edit/region/link_page', () => {
        it('should link page to region', async () => {
            const createPageResponse = await client.post<ResponseBody>(`${url}/v1/edit/create_page`, {
                json: createPage
            });

            const pageId = createPageResponse.body.data.id;

            const linkPageResponse = await client.post<ResponseBody>(`${url}/v3/edit/region/link_page`, {
                json: {
                    pageId,
                    date: new Date(),
                    geoRegionId: 213
                }
            });
            expect(linkPageResponse.statusCode).toEqual(204);

            const checkResponse = await client.get<ResponseBody>(`${url}/v3/edit/region`, {
                searchParams: {
                    id: 1
                }
            });

            const feedPage = checkResponse.body.data.feed.find(
                (item: Omit<FeedItem, 'ids'> & {ids: {id: string; title: string}[]}) =>
                    item.type === FeedType.PAGES && item.ids.find(({id}) => id === pageId)
            );
            expect(feedPage).not.toBeUndefined();
        });
    });

    describe('/v3/edit/region/link_story', () => {
        it('should link story to region', async () => {
            const createStoryResponse = await client.post<ResponseBody>(`${url}/v2/edit/story/create`, {
                json: createStory
            });

            const storyId = createStoryResponse.body.data.id;

            const linkStoryResponse = await client.post<ResponseBody>(`${url}/v3/edit/region/link_story`, {
                json: {
                    storyId,
                    date: new Date(),
                    displayLocations: [StoryDisplayLocation.CONTENT, StoryDisplayLocation.HEADER],
                    geoRegionIds: [213]
                }
            });
            expect(linkStoryResponse.statusCode).toEqual(204);

            const checkResponse = await client.get<ResponseBody>(`${url}/v3/edit/region`, {
                searchParams: {
                    id: 1
                }
            });

            const feedStory = checkResponse.body.data.feed.find(
                (item: Omit<FeedItem, 'ids'> & {ids: {id: string; title: string}[]}) =>
                    item.type === FeedType.STORIES && item.ids.find(({id}) => id === storyId)
            );
            expect(feedStory).not.toBeUndefined();
        });
    });
});

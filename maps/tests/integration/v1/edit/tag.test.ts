import * as http from 'http';
import * as got from 'got';
import {app} from 'app/app';
import {Branch} from 'app/types/consts';
import {TestDb} from 'tests/test-db';
import {fixtures} from 'tests/fixtures/db';
import {startServer, stopServer} from 'tests/test-server';
import editTag from 'tests/fixtures/integration/v1/tag/edit-tag.json';
import createTag from 'tests/fixtures/integration/v1/tag/create-tag.json';
import createdTag from 'tests/fixtures/integration/v1/tag/created-tag.json';
import updateTag from 'tests/fixtures/integration/v1/tag/update-tag.json';
import updatedTag from 'tests/fixtures/integration/v1/tag/updated-tag.json';
import tagList from 'tests/fixtures/integration/v1/tag/tag-list.json';
import {ResponseBody} from 'tests/types';

const client = got.default.extend({
    throwHttpErrors: false,
    retry: 0,
    timeout: 2000,
    responseType: 'json'
});

describe('/v1/edit', () => {
    const testDb = new TestDb();
    let server: http.Server;
    let url: string;

    beforeAll(async () => {
        [server, url] = await startServer(app);
    });

    afterAll(async () => {
        await stopServer(server);
    });

    beforeEach(async () => {
        await testDb.clean();
        await testDb.loadFixtures(fixtures);
    });

    describe('/v1/edit/tag', () => {
        it('should return tag', async () => {
            const {statusCode, body} = await client.get(`${url}/v1/edit/tag`, {
                searchParams: {
                    id: 1
                }
            });

            expect(statusCode).toEqual(200);
            expect(body).toEqual(editTag);
        });

        it('should return 400', async () => {
            const {statusCode} = await client.get(`${url}/v1/edit/tag`);
            expect(statusCode).toEqual(400);
        });

        it('should return 404', async () => {
            const {statusCode} = await client.get(`${url}/v1/edit/tag`, {
                searchParams: {
                    id: 100000
                }
            });
            expect(statusCode).toEqual(404);
        });
    });

    describe('/v1/edit/create_tag', () => {
        it('should create tag', async () => {
            const {statusCode, body} = await client.post<ResponseBody>(`${url}/v1/edit/create_tag`, {
                json: createTag
            });
            expect(statusCode).toEqual(201);

            const {id} = body.data;
            const {body: newTag} = await client.get(`${url}/v1/edit/tag`, {
                searchParams: {id}
            });
            expect(newTag).toEqual(createdTag);
        });

        it('should return 400', async () => {
            const {statusCode} = await client.post(`${url}/v1/edit/create_tag`);
            expect(statusCode).toEqual(400);
        });
    });

    describe('/v1/edit/update_tag', () => {
        it('should update tag', async () => {
            const id = 1;

            const {statusCode} = await client.post(`${url}/v1/edit/update_tag`, {
                json: updateTag,
                searchParams: {id}
            });
            expect(statusCode).toEqual(204);

            const {body} = await client.get(`${url}/v1/edit/tag`, {
                searchParams: {id}
            });
            expect(body).toEqual(updatedTag);
        });

        it('should return 400', async () => {
            const {statusCode} = await client.post(`${url}/v1/edit/update_tag`);
            expect(statusCode).toEqual(400);
        });
    });

    describe('/v1/edit/tag_list', () => {
        it('should return tags', async () => {
            const {statusCode, body} = await client.get(`${url}/v1/edit/tag_list`);
            expect(statusCode).toEqual(200);
            expect(body).toEqual(tagList);
        });
    });

    describe('/v1/edit/delete_tag', () => {
        it('should delete tag', async () => {
            const id = 1;

            const {statusCode: statusCodeBefore} = await client.get(`${url}/v1/edit/tag`, {
                searchParams: {id}
            });
            expect(statusCodeBefore).toEqual(200);

            const {statusCode} = await client.post(`${url}/v1/edit/delete_tag`, {
                searchParams: {id}
            });
            expect(statusCode).toEqual(204);

            const {statusCode: statusCodeAfter} = await client.get(`${url}/v1/edit/tag`, {
                searchParams: {id}
            });
            expect(statusCodeAfter).toEqual(404);
        });

        it('should return 400', async () => {
            const {statusCode} = await client.post(`${url}/v1/edit/delete_tag`);
            expect(statusCode).toEqual(400);
        });
    });

    describe('/v1/edit/update_tag_branch', () => {
        const id = 3;

        it('should archive tag', async () => {
            const filters = JSON.stringify({tagIds: [id]});

            const pagesBefore = await client.get<ResponseBody>(`${url}/v1/edit/page_list`, {
                searchParams: {
                    filters,
                    branch: Branch.PUBLIC
                }
            });
            expect(pagesBefore.body.data.length).toBeGreaterThan(0);

            const result = await client.post(`${url}/v1/edit/update_tag_branch`, {
                searchParams: {
                    id,
                    branch: 'archive'
                }
            });
            expect(result.statusCode).toEqual(202);

            const tagResult = await client.get(`${url}/v1/edit/tag`, {
                searchParams: {
                    id,
                    branch: 'public'
                }
            });
            expect(tagResult.statusCode).toEqual(404);

            const pagesAfter = await client.get<ResponseBody>(`${url}/v1/edit/page_list`, {
                searchParams: {
                    filters,
                    branch: Branch.PUBLIC
                }
            });
            expect(pagesAfter.body.data.length).toEqual(0);
        });

        it('should unarchive tag', async () => {
            const tagsBefore = await client.get<ResponseBody>(`${url}/v1/edit/tag_list`);
            expect(tagsBefore.body.data.length).toEqual(3);

            await client.post(`${url}/v1/edit/update_tag_branch`, {
                searchParams: {
                    id,
                    branch: 'archive'
                }
            });

            const tagsAfter = await client.get<ResponseBody>(`${url}/v1/edit/tag_list`);
            expect(tagsAfter.body.data.length).toEqual(2);

            await client.post(`${url}/v1/edit/update_tag_branch`, {
                searchParams: {
                    id,
                    branch: 'public'
                }
            });

            const tagsRevert = await client.get<ResponseBody>(`${url}/v1/edit/tag_list`);
            expect(tagsRevert.body.data.length).toEqual(3);
        });

        it('should return 400', async () => {
            const {statusCode} = await client.post(`${url}/v1/edit/update_tag_branch`);
            expect(statusCode).toEqual(400);
        });

        it('should return 404', async () => {
            const {statusCode} = await client.post(`${url}/v1/edit/update_tag_branch`, {
                searchParams: {
                    id: 100000,
                    branch: 'archive'
                }
            });
            expect(statusCode).toEqual(404);
        });
    });
});

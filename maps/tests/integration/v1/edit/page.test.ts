import Ajv from 'ajv';
import * as http from 'http';
import * as got from 'got';
import {omit, cloneDeep} from 'lodash';
import {app} from 'app/app';
import {TestDb} from 'tests/test-db';
import {fixtures} from 'tests/fixtures/db';
import {startServer, stopServer} from 'tests/test-server';
import {checkListBodyShape} from 'tests/response-validator';
import {logger} from 'app/lib/logger';
import editPage from 'tests/fixtures/integration/v1/page/edit-page.json';
import createPage from 'tests/fixtures/integration/v1/page/create-page.json';
import createdPage from 'tests/fixtures/integration/v1/page/created-page.json';
import updatePage from 'tests/fixtures/integration/v1/page/update-page.json';
import updatedPage from 'tests/fixtures/integration/v1/page/updated-page.json';
import publisedPage from 'tests/fixtures/integration/v1/page/published-page.json';
import editPageList from 'tests/fixtures/integration/v1/page/edit-page-list.json';
import PAGE_INFO_BY_IDS_SCHEMA from 'tests/integration/v1/ajv-schemas/page-info-by-ids.json';
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

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function omitUpdatedTime(body: any): any {
    return cloneDeep({
        ...body,
        data: omit(body.data, ['updatedTime', 'date'])
    });
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function omitTime(body: any): any {
    return cloneDeep({
        ...body,
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        data: body.data.map((item: any) => omit(item, ['createdTime', 'updatedTime']))
    });
}

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

    describe('/v1/edit/page', () => {
        it('should return page', async () => {
            const {statusCode, body} = await client.get(`${url}/v1/edit/page`, {
                searchParams: {
                    id: 1
                }
            });

            expect(statusCode).toEqual(200);
            expect(omitUpdatedTime(body)).toEqual(editPage);
        });
    });

    describe('/v1/edit/create_page', () => {
        it('should create page', async () => {
            const {statusCode, body} = await client.post<ResponseBody>(`${url}/v1/edit/create_page`, {
                json: createPage
            });

            const id = body.data.id;

            expect(statusCode).toEqual(201);
            expect(body).toEqual({
                success: true,
                data: {id}
            });

            const {body: createdPageBody} = await client.get(`${url}/v1/edit/page`, {
                searchParams: {id}
            });
            expect(omitUpdatedTime(createdPageBody)).toEqual(createdPage);
        });
    });

    describe('/v1/edit/update_page', () => {
        it('should update page', async () => {
            const {statusCode} = await client.post(`${url}/v1/edit/update_page`, {
                json: updatePage,
                searchParams: {
                    id: 1
                }
            });

            expect(statusCode).toEqual(202);

            const {body} = await client.get(`${url}/v1/edit/page`, {
                searchParams: {
                    id: 1
                }
            });

            expect(omitUpdatedTime(body)).toEqual(updatedPage);
        });
    });

    describe('/v1/edit/delete_page', () => {
        it('should delete page', async () => {
            const getResult = await client.get(`${url}/v1/edit/page`, {
                searchParams: {
                    id: 1
                }
            });
            expect(getResult.statusCode).toEqual(200);

            const deleteResult = await client.post(`${url}/v1/edit/delete_page`, {
                searchParams: {
                    id: 1
                }
            });
            expect(deleteResult.statusCode).toEqual(204);

            const checkResult = await client.get(`${url}/v1/edit/page`, {
                searchParams: {
                    id: 1
                }
            });
            expect(checkResult.statusCode).toEqual(404);
        });
    });

    describe('/v1/edit/publish_page', () => {
        it('should publish page', async () => {
            const createResult = await client.post<ResponseBody>(`${url}/v1/edit/create_page`, {
                json: createPage
            });

            expect(createResult.statusCode).toEqual(201);

            const id = createResult.body.data.id;
            const checkResult = await client.get<ResponseBody>(`${url}/v1/edit/page`, {
                searchParams: {
                    id,
                    branch: 'public'
                }
            });
            expect(checkResult.statusCode).toEqual(404);

            await client.post(`${url}/v1/edit/publish_page`, {
                searchParams: {id}
            });

            const {statusCode, body: publishPage} = await client.get<ResponseBody>(`${url}/v1/edit/page`, {
                responseType: 'json',
                searchParams: {
                    id,
                    branch: 'public'
                }
            });
            expect(statusCode).toEqual(200);
            expect(omitUpdatedTime(publishPage)).toEqual(publisedPage);

            const {body: draftPage} = await client.get<ResponseBody>(`${url}/v1/edit/page`, {
                responseType: 'json',
                searchParams: {
                    id,
                    branch: 'draft'
                }
            });
            expect(draftPage.data.tagIds).toEqual(publishPage.data.tagIds);
        });
    });

    describe('/v1/edit/unpublish_page', () => {
        it('should unpublish page', async () => {
            const {statusCode} = await client.post(`${url}/v1/edit/unpublish_page`, {
                searchParams: {id: 4}
            });

            expect(statusCode).toEqual(204);
        });

        it('should return 400', async () => {
            const {statusCode} = await client.post(`${url}/v1/edit/unpublish_page`, {
                searchParams: {id: 1000}
            });

            expect(statusCode).toEqual(400);
        });
    });

    describe('/v1/edit/page_list', () => {
        it('should return pages', async () => {
            const {statusCode, body} = await client.get(`${url}/v1/edit/page_list`);
            expect(statusCode).toEqual(200);
            checkListBodyShape(body);
        });

        it('should return one page', async () => {
            const {statusCode, body} = await client.get<ResponseBody>(`${url}/v1/edit/page_list`, {
                searchParams: {
                    limit: 1
                }
            });

            expect(statusCode).toEqual(200);
            checkListBodyShape(body);

            expect(body.data.length).toEqual(1);
        });

        it('should return one page with offset', async () => {
            const {
                body: {
                    data: [firstItem]
                }
            } = await client.get<ResponseBody>(`${url}/v1/edit/page_list`, {
                searchParams: {
                    limit: 1
                }
            });

            const {
                body,
                body: {
                    data: [secondItem]
                }
            } = await client.get<ResponseBody>(`${url}/v1/edit/page_list`, {
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

        describe('should return correct response by "filters"', () => {
            it('should return pages with "tagIds" = [4, 5]', async () => {
                const {statusCode, body} = await client.get(`${url}/v1/edit/page_list`, {
                    searchParams: {
                        filters: JSON.stringify({tagIds: [4, 5]})
                    }
                });

                expect(statusCode).toEqual(200);
                checkListBodyShape(body);
                expect(omitTime(body)).toEqual(editPageList);
            });

            it('should return all pages', async () => {
                const {statusCode, body} = await client.get(`${url}/v1/edit/page_list`, {
                    searchParams: {
                        filters: JSON.stringify({tagIds: [true]})
                    }
                });

                expect(statusCode).toEqual(200);
                checkListBodyShape(body);
                expect(omitTime(body)).toEqual(editPageList);
            });

            it('should not return any pages', async () => {
                const {statusCode, body} = await client.get<ResponseBody>(`${url}/v1/edit/page_list`, {
                    searchParams: {
                        filters: JSON.stringify({tagIds: [false]})
                    }
                });

                expect(statusCode).toEqual(200);
                checkListBodyShape(body);
                expect(body.data.length).toEqual(0);
            });
        });
    });

    describe('/v1/edit/page_info_by_ids', () => {
        it('should return pages info', async () => {
            const {statusCode, body} = await client.get<ResponseBody>(
                `${url}/v1/edit/page_info_by_ids?ids[0]=1&ids[1]=2`
            );

            expect(statusCode).toEqual(200);
            expect(body.data.length).toBeGreaterThan(0);

            const valid = ajv.validate(PAGE_INFO_BY_IDS_SCHEMA, body.data);
            if (!valid) {
                logger.error(ajv.errorsText());
            }

            expect(valid).toBeTruthy();
        });
    });

    describe('/v1/edit/archive_page', () => {
        const id = 4;

        it('should archive drafted page', async () => {
            await client.post(`${url}/v1/edit/unpublish_page`, {
                searchParams: {id}
            });

            const {statusCode} = await client.post(`${url}/v1/edit/archive_page`, {
                searchParams: {id}
            });

            expect(statusCode).toEqual(202);

            const {
                body: {data}
            } = await client.get<ResponseBody>(`${url}/v1/edit/page_list`);
            expect(data.find((item: {id: number}) => item.id === id)).toBeUndefined();
        });

        it('should archive public page', async () => {
            const {statusCode} = await client.post(`${url}/v1/edit/archive_page`, {
                searchParams: {id}
            });

            expect(statusCode).toEqual(202);

            const {
                body: {data}
            } = await client.get<ResponseBody>(`${url}/v1/edit/page_list`);
            expect(data.find((item: {id: number}) => item.id === id)).toBeUndefined();
        });

        it('should return 400 on already archived page', async () => {
            const successResult = await client.post(`${url}/v1/edit/archive_page`, {
                searchParams: {id}
            });
            expect(successResult.statusCode).toEqual(202);

            const errorResult = await client.post(`${url}/v1/edit/archive_page`, {
                searchParams: {id}
            });
            expect(errorResult.statusCode).toEqual(400);
        });
    });

    describe('/v1/edit/unarchive_page', () => {
        const id = 4;

        it('should unarchive page', async () => {
            await client.post(`${url}/v1/edit/archive_page`, {
                searchParams: {id}
            });

            const {
                body: {data: pagesBefore}
            } = await client.get<ResponseBody>(`${url}/v1/edit/page_list`);
            expect(pagesBefore.find((item: {id: number}) => item.id === id)).toBeUndefined();

            const unarchiveResult = await client.post(`${url}/v1/edit/unarchive_page`, {
                searchParams: {id}
            });
            expect(unarchiveResult.statusCode).toEqual(202);

            const {
                body: {data: pagesAfter}
            } = await client.get<ResponseBody>(`${url}/v1/edit/page_list`, {
                searchParams: {
                    branch: 'draft'
                }
            });
            const page = pagesAfter.find((item: {id: number}) => item.id === id);
            expect(page).not.toBeUndefined();
            expect(page.publish).toEqual(false);
        });

        it('should return 400 on not archived page', async () => {
            const {statusCode} = await client.post(`${url}/v1/edit/unarchive_page`, {
                searchParams: {id}
            });
            expect(statusCode).toEqual(400);
        });
    });
});

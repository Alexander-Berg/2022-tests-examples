import Ajv from 'ajv';
import * as http from 'http';
import * as got from 'got';
import nock from 'nock';
import {app} from 'app/app';
import {TestDb} from 'tests/test-db';
import {fixtures} from 'tests/fixtures/db';
import {startServer, stopServer} from 'tests/test-server';
import {logger} from 'app/lib/logger';
import getIntro from 'tests/fixtures/integration/v3/intro/get-intro.json';
import createIntroWithRequiredFields from 'tests/fixtures/integration/v3/intro/create-intro-with-required-fields.json';
import createdIntroWithRequiredFields from 'tests/fixtures/integration/v3/intro/created-intro-with-required-fields.json';
import createIntroWithFullFields from 'tests/fixtures/integration/v3/intro/create-intro-with-full-fields.json';
import createdIntroWithFullFields from 'tests/fixtures/integration/v3/intro/created-intro-with-full-fields.json';
import updateIntro from 'tests/fixtures/integration/v3/intro/update-intro.json';
import updatedIntro from 'tests/fixtures/integration/v3/intro/updated-intro.json';
import publishedIntro from 'tests/fixtures/integration/v3/intro/published-intro.json';
import INTRO_LIST_SCHEMA from 'tests/integration/v3/ajv-schemas/get-intro-list.json';
import {ResponseBody} from 'tests/types';
import {Intro} from 'app/v3/api/intro';
import {Branch} from 'app/types/consts';

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

    describe('/v3/edit/intro', () => {
        it('should return intro', async () => {
            const response = await client.get(`${url}/v3/edit/intro`, {
                searchParams: {
                    id: 2
                }
            });

            expect(response.statusCode).toEqual(200);
            expect(response.body).toEqual(getIntro);
        });

        it('should return 404 if intro was not found', async () => {
            const {statusCode} = await client.get(`${url}/v3/edit/intro`, {
                searchParams: {
                    id: 9999
                }
            });

            expect(statusCode).toEqual(404);
        });
    });

    describe('/v3/edit/intro/list', () => {
        it('should return intros list', async () => {
            const {statusCode, body} = await client.get<ResponseBody>(`${url}/v3/edit/intro/list`, {
                searchParams: {
                    limit: 2
                }
            });
            expect(statusCode).toEqual(200);
            expect(body.data.length).toBeGreaterThan(0);

            const valid = ajv.validate(INTRO_LIST_SCHEMA, body.data);
            if (!valid) {
                logger.error(ajv.errorsText());
            }

            expect(valid).toBeTruthy();
        });
    });

    describe('/v3/edit/intro/create', () => {
        it('should create intro, with required fields', async () => {
            const {statusCode, body} = await client.post<ResponseBody>(`${url}/v3/edit/intro/create`, {
                json: createIntroWithRequiredFields
            });
            expect(statusCode).toEqual(201);
            const id = body.data.id;

            const {body: introBody} = await client.get<ResponseBody>(`${url}/v3/edit/intro`, {
                searchParams: {
                    id
                }
            });

            // random uid every time
            delete introBody.data.uid;
            expect(introBody).toEqual(createdIntroWithRequiredFields);
        });

        it('should create intro, with full fields', async () => {
            const {statusCode, body} = await client.post<ResponseBody>(`${url}/v3/edit/intro/create`, {
                json: createIntroWithFullFields
            });
            expect(statusCode).toEqual(201);
            const id = body.data.id;

            const {body: introBody} = await client.get<ResponseBody>(`${url}/v3/edit/intro`, {
                searchParams: {
                    id
                }
            });
            expect(introBody).toEqual(createdIntroWithFullFields);
        });
    });

    describe('/v3/edit/intro/update', () => {
        it('should update intro', async () => {
            const searchParams = {
                id: 2
            };

            const {statusCode} = await client.post(`${url}/v3/edit/intro/update`, {
                json: updateIntro,
                searchParams
            });
            expect(statusCode).toEqual(204);

            const {body} = await client.get(`${url}/v3/edit/intro`, {
                searchParams
            });
            expect(body).toEqual(updatedIntro);
        });
    });

    describe('/v3/edit/intro/publish', () => {
        it('should publish intro', async () => {
            const createIntro = await client.post<ResponseBody>(`${url}/v3/edit/intro/create`, {
                json: createIntroWithFullFields
            });
            expect(createIntro.statusCode).toEqual(201);
            const searchParams = {
                id: createIntro.body.data.id
            };

            const {statusCode} = await client.post(`${url}/v3/edit/intro/publish`, {
                searchParams
            });
            expect(statusCode).toEqual(204);

            const {body} = await client.get(`${url}/v3/edit/intro`, {
                searchParams: {id: createIntro.body.data.id + 1}
            });
            expect(body).toEqual(publishedIntro);

            const publishedIntroResponse = await client.get<{data: Intro}>(`${url}/v3/edit/intro`, {searchParams});
            expect(publishedIntroResponse.body.data.isPublished).toBeTruthy();
        });
    });

    describe('/v3/edit/intro/unpublish', () => {
        it('should unpublish intro', async () => {
            const createIntro = await client.post<ResponseBody>(`${url}/v3/edit/intro/create`, {
                json: createIntroWithFullFields
            });
            expect(createIntro.statusCode).toEqual(201);
            const searchParams = {
                id: createIntro.body.data.id
            };

            const publishResponse = await client.post(`${url}/v3/edit/intro/publish`, {searchParams});
            expect(publishResponse.statusCode).toEqual(204);

            const publishedIntroResponse = await client.get<{data: Intro}>(`${url}/v3/edit/intro`, {searchParams});
            expect(publishedIntroResponse.body.data.isPublished).toBeTruthy();

            const unpublishResponse = await client.post(`${url}/v3/edit/intro/unpublish`, {searchParams});
            expect(unpublishResponse.statusCode).toEqual(204);

            const deletedPublishedIntroResponse = await client.get(`${url}/v3/edit/intro`, {
                searchParams: {id: createIntro.body.data.id + 1}
            });
            expect(deletedPublishedIntroResponse.statusCode).toEqual(404);

            const unpublishedIntroResponse = await client.get<{data: Intro}>(`${url}/v3/edit/intro`, {searchParams});
            expect(unpublishedIntroResponse.body.data.isPublished).toBeFalsy();
        });
    });

    describe('/v3/edit/intro/delete', () => {
        it('should delete draft intro and archive public', async () => {
            const createIntro = await client.post<ResponseBody>(`${url}/v3/edit/intro/create`, {
                json: createIntroWithFullFields
            });
            expect(createIntro.statusCode).toEqual(201);
            const searchParams = {
                id: createIntro.body.data.id
            };

            const publishResponse = await client.post(`${url}/v3/edit/intro/publish`, {searchParams});
            expect(publishResponse.statusCode).toEqual(204);

            const publishedIntroResponse = await client.get<{data: Intro}>(`${url}/v3/edit/intro`, {searchParams});
            expect(publishedIntroResponse.body.data.isPublished).toBeTruthy();

            const deleteResponse = await client.post<ResponseBody>(`${url}/v3/edit/intro/delete`, {searchParams});
            expect(deleteResponse.statusCode).toEqual(204);

            const deletedIntroResponse = await client.get(`${url}/v3/edit/intro`, {searchParams});
            expect(deletedIntroResponse.statusCode).toEqual(404);

            const {body} = await client.get<ResponseBody>(`${url}/v3/edit/intro`, {
                searchParams: {id: createIntro.body.data.id + 1}
            });
            expect(body.data.branch).toEqual(Branch.ARCHIVE);
        });
    });
});

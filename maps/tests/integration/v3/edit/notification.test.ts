import Ajv from 'ajv';
import * as http from 'http';
import * as got from 'got';
import nock from 'nock';
import {app} from 'app/app';
import {TestDb} from 'tests/test-db';
import {fixtures} from 'tests/fixtures/db';
import {startServer, stopServer} from 'tests/test-server';
import getNotification from 'tests/fixtures/integration/v3/notification/get-notification.json';
import createNotification from 'tests/fixtures/integration/v3/notification/create-notification.json';
import createdNotification from 'tests/fixtures/integration/v3/notification/created-notification.json';
import updateNotification from 'tests/fixtures/integration/v3/notification/update-notification.json';
import updatedNotification from 'tests/fixtures/integration/v3/notification/updated-notification.json';
import updatedPublishedNotification from 'tests/fixtures/integration/v3/notification/updated-published-notification.json';
import publishedNotification from 'tests/fixtures/integration/v3/notification/published-notification.json';
import archivedNotification from 'tests/fixtures/integration/v3/notification/archived-notification.json';
import NOTIFICATION_LIST_SCHEMA from 'tests/integration/v3/ajv-schemas/get-notification-list.json';
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

    describe('/v3/edit/notification', () => {
        it('should return notification', async () => {
            const response = await client.get(`${url}/v3/edit/notification`, {
                searchParams: {
                    id: 2
                }
            });

            expect(response.statusCode).toEqual(200);
            expect(response.body).toEqual(getNotification);
        });

        it('should return 404 if notification was not found', async () => {
            const {statusCode} = await client.get(`${url}/v3/edit/notification`, {
                searchParams: {
                    id: 9999
                }
            });

            expect(statusCode).toEqual(404);
        });
    });

    describe('/v3/edit/notification/list', () => {
        it('should return notifications list', async () => {
            const {statusCode, body} = await client.get<ResponseBody>(`${url}/v3/edit/notification/list`, {
                searchParams: {
                    limit: 2
                }
            });

            expect(statusCode).toEqual(200);
            expect(body.data.length).toBeGreaterThan(0);

            const valid = ajv.validate(NOTIFICATION_LIST_SCHEMA, body.data);
            if (!valid) {
                console.log(ajv.errorsText());
            }

            expect(valid).toBeTruthy();
        });
    });

    describe('/v3/edit/notification/create', () => {
        it('should create notification', async () => {
            const {statusCode, body} = await client.post<ResponseBody>(`${url}/v3/edit/notification/create`, {
                json: createNotification
            });

            expect(statusCode).toEqual(201);

            const id = body.data.id;

            const {body: notificationBody} = await client.get<ResponseBody>(`${url}/v3/edit/notification`, {
                searchParams: {
                    id
                }
            });

            // random uid every time
            delete notificationBody.data.uid;

            expect(notificationBody).toEqual({data: {...createdNotification, id}});
        });
    });

    describe('/v3/edit/notification/update', () => {
        it('should update notification', async () => {
            const searchParams = {
                id: 2
            };

            const {statusCode} = await client.post(`${url}/v3/edit/notification/update`, {
                json: updateNotification,
                searchParams
            });

            expect(statusCode).toEqual(204);

            const {body} = await client.get(`${url}/v3/edit/notification`, {
                searchParams
            });

            expect(body).toEqual(updatedNotification);
        });

        it('should update published notification', async () => {
            const searchParams = {
                id: 5
            };

            const {statusCode} = await client.post(`${url}/v3/edit/notification/update`, {
                json: updateNotification,
                searchParams
            });

            expect(statusCode).toEqual(204);

            const {body} = await client.get(`${url}/v3/edit/notification`, {
                searchParams
            });

            expect(body).toEqual(updatedPublishedNotification);
        });
    });

    describe('/v3/edit/notification/publish', () => {
        it('should publish notification', async () => {
            const {statusCode} = await client.post(`${url}/v3/edit/notification/publish`, {
                searchParams: {id: 2}
            });

            expect(statusCode).toEqual(204);

            const {body} = await client.get(`${url}/v3/edit/notification`, {
                searchParams: {id: 7}
            });

            expect(body).toEqual(publishedNotification);
        });
    });

    describe('/v3/edit/notification/unpublish', () => {
        it('should unpublish notification', async () => {
            const unpublishResponse = await client.post(`${url}/v3/edit/notification/unpublish`, {
                searchParams: {id: 3}
            });

            expect(unpublishResponse.statusCode).toEqual(204);

            const getResponse = await client.get(`${url}/v3/edit/notification`, {
                searchParams: {id: 4}
            });

            expect(getResponse.statusCode).toEqual(404);
        });
    });

    describe('/v3/edit/notification/delete', () => {
        it('should delete draft notification and archive public', async () => {
            const deleteResponse = await client.post(`${url}/v3/edit/notification/delete`, {
                searchParams: {id: 3}
            });

            expect(deleteResponse.statusCode).toEqual(204);

            const getDraftResponse = await client.get(`${url}/v3/edit/notification`, {
                searchParams: {id: 3}
            });

            expect(getDraftResponse.statusCode).toEqual(404);

            const getArchivedResponse = await client.get(`${url}/v3/edit/notification`, {
                searchParams: {id: 4}
            });

            expect(getArchivedResponse.body).toEqual(archivedNotification);
        });
    });
});

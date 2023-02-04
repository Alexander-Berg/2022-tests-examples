/* eslint-disable @typescript-eslint/no-unused-vars */
import * as http from 'http';
import nock from 'nock';
import Ajv from 'ajv';
import * as got from 'got';
import {promises} from 'fs';
import {app} from 'app/app';
import {intHostConfigLoader} from 'app/lib/host-loader';
import {logger} from 'app/lib/logger';
import {TestDb} from 'tests/test-db';
import {ResponseBody} from 'tests/types';
import {fixtures} from 'tests/fixtures/db';
import {startServer, stopServer} from 'tests/test-server';
import {createEventJson} from 'tests/fixtures/integration/v1/event/create-event';
import {createdEventJson} from 'tests/fixtures/integration/v1/event/created-event';
import {eventJson} from 'tests/fixtures/integration/v1/event';
import {EVENT_LIST_SCHEMA} from 'tests/integration/v1/ajv-schemas/event-list';

const client = got.default.extend({
    throwHttpErrors: false,
    retry: 0,
    timeout: 2000,
    responseType: 'json'
});

const ORG_ID = 1094008369;

const ajv = new Ajv({
    nullable: true
});

describe('/v1/edit', () => {
    const testDb = new TestDb();
    let server: http.Server;
    let origin: string;
    let getEventUrl: string;
    let getPublicEventUrl: string;
    let getEventListUrl: string;
    let createEventUrl: string;
    let updateEventUrl: string;
    let publishEventUrl: string;
    let unpublishEventUrl: string;
    let deleteEventUrl: string;

    beforeAll(async () => {
        [server, origin] = await startServer(app);
        nock.disableNetConnect();
        nock.enableNetConnect(/localhost/);
        getPublicEventUrl = `${origin}/v1/event`;
        getEventUrl = `${origin}/v1/edit/event`;
        getEventListUrl = `${getEventUrl}/list`;
        createEventUrl = `${getEventUrl}/create`;
        updateEventUrl = `${getEventUrl}/update`;
        publishEventUrl = `${getEventUrl}/publish`;
        unpublishEventUrl = `${getEventUrl}/unpublish`;
        deleteEventUrl = `${getEventUrl}/delete`;
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

    describe('/v1/edit/event', () => {
        it('should return event', async () => {
            const {statusCode, body} = await client.get<ResponseBody>(getEventUrl, {
                searchParams: {
                    id: 1
                }
            });

            delete body.eventMeta.createdTime;
            delete body.eventMeta.updatedTime;

            expect(statusCode).toEqual(200);
            expect(body).toEqual(eventJson.getEvent);
        });

        it('should return 404 if event was not found', async () => {
            const {statusCode} = await client.get(getEventUrl, {
                searchParams: {
                    id: 9999
                }
            });

            expect(statusCode).toEqual(404);
        });
    });

    describe('/v1/edit/event/list', () => {
        it('should return event list', async () => {
            const {search} = await intHostConfigLoader.get();
            nock(search)
                .get('/yandsearch')
                .query({
                    origin: 'discovery',
                    lang: 'ru_RU',
                    type: 'biz',
                    format: 'json',
                    snippets: [
                        'businessrating/1.x',
                        'masstransit/1.x',
                        'photos/2.x',
                        'cluster_permalinks',
                        'promo_mastercard/1.x:mastercardoffers'
                    ].join(','),
                    ms: 'pb',
                    business_oid: ORG_ID
                })
                .reply(200, await promises.readFile(`src/tests/fixtures/backend/organizations/${ORG_ID}.protobuf`));

            await Promise.all(
                [
                    createEventJson.withUrlMinReqField,
                    createEventJson.withImagesAndFeatures,
                    createEventJson.withButtons,
                    createEventJson.withOidMinReqField
                ].map((json) =>
                    client
                        .post<ResponseBody>(createEventUrl, {
                            json
                        })
                        .catch()
                )
            );
            const {statusCode, body} = await client.get<ResponseBody>(getEventListUrl, {
                searchParams: {
                    limit: 10,
                    offset: 0
                }
            });

            expect(statusCode).toEqual(200);
            expect(body.events.length).toBeGreaterThan(0);

            const valid = ajv.validate(EVENT_LIST_SCHEMA, body);
            if (!valid) {
                logger.error(ajv.errorsText());
            }

            expect(valid).toBeTruthy();
        });
    });

    describe('/v1/edit/event/create', () => {
        describe('should create event', () => {
            it('minimum required fields with url', async () => {
                const {statusCode, body} = await client.post<ResponseBody>(createEventUrl, {
                    json: createEventJson.withUrlMinReqField
                });

                expect(statusCode).toEqual(200);

                const getEventResponse = await client.get<ResponseBody>(getEventUrl, {
                    searchParams: {
                        id: body.id
                    }
                });

                delete getEventResponse.body.eventMeta.createdTime;
                delete getEventResponse.body.eventMeta.updatedTime;
                delete getEventResponse.body.externalId;

                expect(getEventResponse.statusCode).toEqual(200);
                expect(getEventResponse.body).toEqual(createdEventJson.withUrlMinReqField);
            });

            it('minimum required fields with oid', async () => {
                const {search} = await intHostConfigLoader.get();
                nock(search)
                    .get('/yandsearch')
                    .query({
                        origin: 'discovery',
                        lang: 'ru_RU',
                        type: 'biz',
                        format: 'json',
                        snippets: [
                            'businessrating/1.x',
                            'masstransit/1.x',
                            'photos/2.x',
                            'cluster_permalinks',
                            'promo_mastercard/1.x:mastercardoffers'
                        ].join(','),
                        ms: 'pb',
                        business_oid: ORG_ID
                    })
                    .reply(200, await promises.readFile(`src/tests/fixtures/backend/organizations/${ORG_ID}.protobuf`));

                const {statusCode, body} = await client.post<ResponseBody>(createEventUrl, {
                    json: createEventJson.withOidMinReqField
                });

                expect(statusCode).toEqual(200);

                const getEventResponse = await client.get<ResponseBody>(getEventUrl, {
                    searchParams: {
                        id: body.id
                    }
                });

                delete getEventResponse.body.eventMeta.createdTime;
                delete getEventResponse.body.eventMeta.updatedTime;
                delete getEventResponse.body.externalId;

                expect(getEventResponse.statusCode).toEqual(200);
                expect(getEventResponse.body).toEqual(createdEventJson.withOidMinReqField);
            });

            it('with buttons', async () => {
                const {statusCode, body} = await client.post<ResponseBody>(createEventUrl, {
                    json: createEventJson.withButtons
                });

                expect(statusCode).toEqual(200);

                const getEventResponse = await client.get<ResponseBody>(getEventUrl, {
                    searchParams: {
                        id: body.id
                    }
                });

                delete getEventResponse.body.eventMeta.createdTime;
                delete getEventResponse.body.eventMeta.updatedTime;
                delete getEventResponse.body.externalId;

                expect(getEventResponse.statusCode).toEqual(200);
                expect(getEventResponse.body).toEqual(createdEventJson.withButtons);
            });

            it('with images and features', async () => {
                const {statusCode, body} = await client.post<ResponseBody>(createEventUrl, {
                    json: createEventJson.withImagesAndFeatures
                });

                expect(statusCode).toEqual(200);

                const getEventResponse = await client.get<ResponseBody>(getEventUrl, {
                    searchParams: {
                        id: body.id
                    }
                });

                delete getEventResponse.body.eventMeta.createdTime;
                delete getEventResponse.body.eventMeta.updatedTime;
                delete getEventResponse.body.externalId;

                expect(getEventResponse.statusCode).toEqual(200);
                expect(getEventResponse.body).toEqual(createdEventJson.withImagesAndFeatures);
            });

            it('creates event with custom id', async () => {
                const externalId = 'custom-id';
                const {statusCode, body} = await client.post<ResponseBody>(createEventUrl, {
                    json: {...createEventJson.withImagesAndFeatures, externalId}
                });

                expect(statusCode).toEqual(200);

                const getEventResponse = await client.get<ResponseBody>(getEventUrl, {
                    searchParams: {
                        id: body.id
                    }
                });

                expect(getEventResponse.statusCode).toEqual(200);
                expect(getEventResponse.body.externalId).toEqual(externalId);
            });
        });

        describe('should not create event', () => {
            it('with existed id', async () => {
                const externalId = 'custom-id';
                const {statusCode} = await client.post<ResponseBody>(createEventUrl, {
                    json: {...createEventJson.withImagesAndFeatures, externalId}
                });

                expect(statusCode).toEqual(200);

                const existedEventResponse = await client.post<ResponseBody>(createEventUrl, {
                    json: {...createEventJson.withImagesAndFeatures, externalId}
                });

                expect(existedEventResponse.statusCode).toEqual(400);
                expect(existedEventResponse.body.message).toEqual(`"${externalId}" already exists`);
            });

            it('with oid and url simultaneously', async () => {
                const {statusCode, body} = await client.post<ResponseBody>(createEventUrl, {
                    json: createEventJson.withOidAndUrl
                });

                expect(statusCode).toEqual(400);
                expect(body.message).toEqual('"oid" must not exist simultaneously with [url]');
            });
        });
    });

    describe('/v1/edit/event/update', () => {
        it('should update events data, poi data and id', async () => {
            const externalId = 'custom-id';
            const searchParams = {
                id: 1
            };

            const {statusCode} = await client.post(updateEventUrl, {
                json: {...createEventJson.withUrlMinReqField, externalId},
                searchParams
            });
            expect(statusCode).toEqual(204);

            const {body} = await client.get<ResponseBody>(getEventUrl, {
                searchParams
            });

            expect(body.externalId).toEqual(externalId);

            delete body.eventMeta.createdTime;
            delete body.eventMeta.updatedTime;
            delete body.externalId;
            body.eventMeta.id = 2;

            expect(body).toEqual(createdEventJson.withUrlMinReqField);
        });
    });

    describe('/v1/edit/event/publish', () => {
        it('should publish event', async () => {
            const createdEventResponse = await client.post<ResponseBody>(createEventUrl, {
                json: createEventJson.withUrlMinReqField
            });
            expect(createdEventResponse.statusCode).toEqual(200);
            const searchParams = {
                id: createdEventResponse.body.id
            };

            const {body} = await client.get<ResponseBody>(getEventUrl, {
                searchParams
            });
            expect(body.eventMeta.published).toBeFalsy();

            const {statusCode} = await client.post(publishEventUrl, {
                searchParams
            });
            expect(statusCode).toEqual(204);

            const publishedPublicEventResponse = await client.get<ResponseBody>(getPublicEventUrl, {
                searchParams: {
                    id: body.externalId
                }
            });
            expect(publishedPublicEventResponse.statusCode).toEqual(200);

            const publishedDraftEventResponse = await client.get<ResponseBody>(getEventUrl, {searchParams});
            expect(publishedDraftEventResponse.body.eventMeta.published).toBeTruthy();
        });
    });

    describe('/v1/edit/event/unpublish', () => {
        it('should unpublish event', async () => {
            const createdEventResponse = await client.post<ResponseBody>(createEventUrl, {
                json: createEventJson.withUrlMinReqField
            });
            expect(createdEventResponse.statusCode).toEqual(200);
            const searchParams = {
                id: createdEventResponse.body.id
            };

            const {body} = await client.get<ResponseBody>(getEventUrl, {
                searchParams
            });
            expect(body.eventMeta.published).toBeFalsy();

            const {statusCode} = await client.post(publishEventUrl, {
                searchParams
            });
            expect(statusCode).toEqual(204);

            const publishedPublicEventResponse = await client.get<ResponseBody>(getPublicEventUrl, {
                searchParams: {
                    id: body.externalId
                }
            });
            expect(publishedPublicEventResponse.statusCode).toEqual(200);

            const publishedDraftEventResponse = await client.get<ResponseBody>(getEventUrl, {searchParams});
            expect(publishedDraftEventResponse.body.eventMeta.published).toBeTruthy();

            const unpublishResponse = await client.post(unpublishEventUrl, {
                searchParams
            });
            expect(unpublishResponse.statusCode).toEqual(204);

            const unpublishedPublicEventResponse = await client.get<ResponseBody>(getPublicEventUrl, {
                searchParams: {
                    id: body.externalId
                }
            });
            expect(unpublishedPublicEventResponse.statusCode).toEqual(404);

            const unpublishedDraftEventResponse = await client.get<ResponseBody>(getEventUrl, {searchParams});
            expect(unpublishedDraftEventResponse.body.eventMeta.published).toBeFalsy();
        });
    });

    describe('/v1/edit/event/delete', () => {
        it('should return correct response', async () => {
            const createdEventResponse = await client.post<ResponseBody>(createEventUrl, {
                json: createEventJson.withUrlMinReqField
            });
            expect(createdEventResponse.statusCode).toEqual(200);
            const searchParams = {
                id: createdEventResponse.body.id
            };

            const {body} = await client.get<ResponseBody>(getEventUrl, {
                searchParams
            });
            expect(body.eventMeta.published).toBeFalsy();

            const {statusCode} = await client.post(publishEventUrl, {
                searchParams
            });
            expect(statusCode).toEqual(204);

            const publishedPublicEventResponse = await client.get<ResponseBody>(getPublicEventUrl, {
                searchParams: {
                    id: body.externalId
                }
            });
            expect(publishedPublicEventResponse.statusCode).toEqual(200);

            const publishedDraftEventResponse = await client.get<ResponseBody>(getEventUrl, {searchParams});
            expect(publishedDraftEventResponse.body.eventMeta.published).toBeTruthy();

            const deleteResponse = await client.post(deleteEventUrl, {
                searchParams
            });
            expect(deleteResponse.statusCode).toEqual(204);

            const unpublishedPublicEventResponse = await client.get<ResponseBody>(getPublicEventUrl, {
                searchParams: {
                    id: body.externalId
                }
            });
            expect(unpublishedPublicEventResponse.statusCode).toEqual(404);

            const unpublishedDraftEventResponse = await client.get<ResponseBody>(getEventUrl, {searchParams});
            expect(unpublishedDraftEventResponse.statusCode).toEqual(404);
        });
    });
});

import {expect} from 'chai';
import {EndpointSuiteDefinition} from 'tests/integration/endpoints/suite';
import {createResponseSchemaValidator} from 'tests/integration/response-validator';
import {
    UNKNOWN_SERVICE_TICKET,
    SELF_SERVICE_TICKET,
    UID1_USER_TICKET
} from 'tests/integration/endpoints/tvm-tickets';
import {mapFixture} from 'tests/integration/endpoints/fixtures/map';
import {objectFixture} from 'tests/integration/endpoints/fixtures/object';

export const main: EndpointSuiteDefinition = (context) => {
    describe('GET /v2/maps/{sid}/objects/{id}', () => {
        const validateResponseSchema = createResponseSchemaValidator({
            path: '/v2/maps/{sid}/objects/{id}'
        });

        beforeEach(async () => {
            await context.db.loadFixtures([
                {
                    table: 'maps',
                    rows: [
                        {
                            id: 1,
                            sid: 'sid-1',
                            time_created: '2016-07-14 01:00:00',
                            time_updated: '2016-07-14 01:00:00',
                            revision: '0',
                            deleted: true,
                            properties: mapFixture.properties,
                            options: mapFixture.options,
                            state: mapFixture.state
                        },
                        {
                            id: 2,
                            sid: 'sid-2',
                            time_created: '2016-07-14 01:00:00',
                            time_updated: '2016-07-14 01:00:00',
                            revision: '5',
                            deleted: false,
                            properties: mapFixture.properties,
                            options: mapFixture.options,
                            state: mapFixture.state
                        }
                    ]
                },
                {
                    table: 'maps_users',
                    rows: [
                        {
                            map_id: 1,
                            uid: 1,
                            role: 'administrator'
                        },
                        {
                            map_id: 2,
                            uid: 1,
                            role: 'administrator'
                        }
                    ]
                },
                {
                    table: 'objects',
                    rows: [
                        {
                            id: 1,
                            map_id: 2,
                            geometry: {
                                type: 'Point',
                                coordinates: [37.62, 55.75]
                            },
                            options: objectFixture.options,
                            properties: objectFixture.properties,
                            // This fields are needed only to satisfy constraints.
                            bbox_max_quad_key: 1,
                            bbox_min_quad_key: 1
                        },
                        {
                            id: 2,
                            map_id: 2,
                            geometry: {
                                type: 'LineString',
                                coordinates: [
                                    [10, 10],
                                    [15, 11],
                                    [20, 20]
                                ]
                            },
                            properties: {
                                name: '<script></script><b></b>',
                                description: '<script></script><b></b>'
                            },
                            options: {},
                            bbox_max_quad_key: 1,
                            bbox_min_quad_key: 1
                        }
                    ]
                }
            ]);
        });

        describe('authentication', () => {
            it('should return 401 for request without tickets', async () => {
                const res = await context.server.request('/v2/maps/sid-1/objects/1');
                expect(res.statusCode).to.equal(401);
            });

            it('should return 401 for request without service ticket', async () => {
                const res = await context.server.request('/v2/maps/sid-1/objects/1', {
                    headers: {
                        'X-Ya-User-Ticket': UID1_USER_TICKET
                    }
                });
                expect(res.statusCode).to.equal(401);
            });

            it('should return 401 for request without user ticket', async () => {
                const res = await context.server.request('/v2/maps/sid-1/objects/1', {
                    headers: {
                        'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                    }
                });
                expect(res.statusCode).to.equal(401);
            });

            it('should return 403 for unknown service ticket', async () => {
                const res = await context.server.request('/v2/maps/sid-1/objects/1', {
                    headers: {
                        'X-Ya-Service-Ticket': UNKNOWN_SERVICE_TICKET,
                        'X-Ya-User-Ticket': UID1_USER_TICKET
                    }
                });
                expect(res.statusCode).to.equal(403);
            });
        });

        it('should return 404 if map not found', async () => {
            const res = await context.server.request('/v2/maps/sid-99/objects/1', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                    'X-Ya-User-Ticket': UID1_USER_TICKET
                },
                json: true
            });

            expect(res.statusCode).to.equal(404);
            validateResponseSchema(res);
            expect(res.body.type).to.equal('ObjectNotFoundError');
        });

        it('should return 404 if map is deleted', async () => {
            const res = await context.server.request('/v2/maps/sid-1/objects/1', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                    'X-Ya-User-Ticket': UID1_USER_TICKET
                },
                json: true
            });

            expect(res.statusCode).to.equal(404);
            validateResponseSchema(res);
            expect(res.body.type).to.equal('ObjectNotFoundError');
        });

        it('should return 404 if object not found', async () => {
            const res = await context.server.request('/v2/maps/sid-2/objects/99', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                    'X-Ya-User-Ticket': UID1_USER_TICKET
                },
                json: true
            });

            expect(res.statusCode).to.equal(404);
            validateResponseSchema(res);
            expect(res.body.type).to.equal('ObjectNotFoundError');
        });

        it('should return 400 if object id has not unsigned integer format', async () => {
            const res = await context.server.request('/v2/maps/sid-2/objects/a', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                    'X-Ya-User-Ticket': UID1_USER_TICKET
                },
                json: true
            });

            expect(res.statusCode).to.equal(400);
        });

        it('should return object by id on map', async () => {
            const res = await context.server.request('/v2/maps/sid-2/objects/1', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                    'X-Ya-User-Ticket': UID1_USER_TICKET
                },
                json: true
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);
            expect(res.body.id).to.equal('1');
            expect(res.body.geometry.type).to.equal('Point');
        });
    });

    describe('GET /v2/public_maps/{sid}/objects/{id}', () => {
        const validateResponseSchema = createResponseSchemaValidator({
            path: '/v2/public_maps/{sid}/objects/{id}'
        });

        beforeEach(async () => {
            await context.db.loadFixtures([
                {
                    table: 'maps',
                    rows: [
                        {
                            id: 1,
                            sid: 'sid-1',
                            time_created: '2016-07-14 01:00:00',
                            time_updated: '2016-07-14 01:00:00',
                            revision: '0',
                            deleted: true,
                            properties: mapFixture.properties,
                            options: mapFixture.options,
                            state: mapFixture.state
                        },
                        {
                            id: 2,
                            sid: 'sid-2',
                            time_created: '2016-07-14 01:00:00',
                            time_updated: '2016-07-14 01:00:00',
                            revision: '0',
                            deleted: false,
                            properties: {
                                name: 'name',
                                access: 'private',
                                description: 'desc'
                            },
                            options: mapFixture.options,
                            state: mapFixture.state
                        },
                        {
                            id: 3,
                            sid: 'sid-3',
                            time_created: '2016-07-14 01:00:00',
                            time_updated: '2016-07-14 01:00:00',
                            revision: '10',
                            deleted: false,
                            properties: {
                                name: 'name',
                                access: 'public',
                                description: 'desc'
                            },
                            options: mapFixture.options,
                            state: mapFixture.state
                        }
                    ]
                },
                {
                    table: 'maps_users',
                    rows: [
                        {
                            map_id: 1,
                            uid: 1,
                            role: 'administrator'
                        },
                        {
                            map_id: 2,
                            uid: 1,
                            role: 'administrator'
                        },
                        {
                            map_id: 3,
                            uid: 1,
                            role: 'administrator'
                        }
                    ]
                },
                {
                    table: 'objects',
                    rows: [
                        {
                            id: 1,
                            map_id: 3,
                            geometry: {
                                type: 'Point',
                                coordinates: [37.62, 55.75]
                            },
                            options: objectFixture.options,
                            properties: objectFixture.properties,
                            // This fields are needed only to satisfy constraints.
                            bbox_max_quad_key: 1,
                            bbox_min_quad_key: 1
                        },
                        {
                            id: 2,
                            map_id: 3,
                            geometry: {
                                type: 'LineString',
                                coordinates: [
                                    [10, 10],
                                    [15, 11],
                                    [20, 20]
                                ]
                            },
                            properties: {
                                name: '<script></script><b></b>',
                                description: '<script></script><b></b>'
                            },
                            options: {},
                            bbox_max_quad_key: 123,
                            bbox_min_quad_key: 123
                        }
                    ]
                }
            ]);
        });

        describe('authentication', () => {
            it('should return 401 for request without tickets', async () => {
                const res = await context.server.request('/v2/public_maps/sid-1/objects/1');
                expect(res.statusCode).to.equal(401);
            });

            it('should return 403 for unknown service ticket', async () => {
                const res = await context.server.request('/v2/public_maps/sid-1/objects/1', {
                    headers: {
                        'X-Ya-Service-Ticket': UNKNOWN_SERVICE_TICKET
                    }
                });
                expect(res.statusCode).to.equal(403);
            });
        });

        it('should return 404 if map not found', async () => {
            const res = await context.server.request('/v2/public_maps/sid-99/objects/1', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                },
                json: true
            });

            expect(res.statusCode).to.equal(404);
            validateResponseSchema(res);
            expect(res.body.type).to.equal('ObjectNotFoundError');
        });

        it('should return 404 if map is deleted', async () => {
            const res = await context.server.request('/v2/public_maps/sid-1/objects/1', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                },
                json: true
            });

            expect(res.statusCode).to.equal(404);
            validateResponseSchema(res);
            expect(res.body.type).to.equal('ObjectNotFoundError');
        });

        it('should return 404 if map is private', async () => {
            const res = await context.server.request('/v2/public_maps/sid-2/objects/1', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                },
                json: true
            });

            expect(res.statusCode).to.equal(404);
            validateResponseSchema(res);
            expect(res.body.type).to.equal('ObjectNotFoundError');
        });

        it('should return 404 if object not found', async () => {
            const res = await context.server.request('/v2/public_maps/sid-3/objects/99', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                },
                json: true
            });

            expect(res.statusCode).to.equal(404);
            validateResponseSchema(res);
            expect(res.body.type).to.equal('ObjectNotFoundError');
        });

        it('should return object by id on public map', async () => {
            const res = await context.server.request('/v2/public_maps/sid-3/objects/1', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                },
                json: true
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);
            expect(res.body.id).to.equal('1');
            expect(res.body.geometry.type).to.equal('Point');
        });
    });
};

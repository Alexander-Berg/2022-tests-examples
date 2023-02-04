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
    describe('GET /v1/maps/{sid}', () => {
        const validateResponseSchema = createResponseSchemaValidator({path: '/v1/maps/{sid}'});

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
                }
            ]);
        });

        describe('authentication', () => {
            it('should return 401 for request without tickets', async () => {
                const res = await context.server.request('/v1/maps/sid-1');
                expect(res.statusCode).to.equal(401);
            });

            it('should return 401 for request without service ticket', async () => {
                const res = await context.server.request('/v1/maps/sid-1', {
                    headers: {
                        'X-Ya-User-Ticket': UID1_USER_TICKET
                    }
                });
                expect(res.statusCode).to.equal(401);
            });

            it('should return 401 for request without user ticket', async () => {
                const res = await context.server.request('/v1/maps/sid-1', {
                    headers: {
                        'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                    }
                });
                expect(res.statusCode).to.equal(401);
            });

            it('should return 403 for unknown service ticket', async () => {
                const res = await context.server.request('/v1/maps/sid-1', {
                    headers: {
                        'X-Ya-Service-Ticket': UNKNOWN_SERVICE_TICKET,
                        'X-Ya-User-Ticket': UID1_USER_TICKET
                    }
                });
                expect(res.statusCode).to.equal(403);
            });
        });

        it('should return 404 if map not found', async () => {
            const res = await context.server.request('/v1/maps/sid-3', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                    'X-Ya-User-Ticket': UID1_USER_TICKET
                },
                json: true
            });

            expect(res.statusCode).to.equal(404);
            validateResponseSchema(res);
            expect(res.body.type).to.equal('MapNotFoundError');
        });

        it('should return 404 if map is deleted', async () => {
            const res = await context.server.request('/v1/maps/sid-1', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                    'X-Ya-User-Ticket': UID1_USER_TICKET
                },
                json: true
            });

            expect(res.statusCode).to.equal(404);
            validateResponseSchema(res);
            expect(res.body.type).to.equal('MapNotFoundError');
        });

        it('should return map without objects', async () => {
            const res = await context.server.request('/v1/maps/sid-2', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                    'X-Ya-User-Ticket': UID1_USER_TICKET
                },
                json: true
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);
            expect(res.body.maps).to.have.length(1, 'Invalid number of maps');
            expect(res.body.maps[0].properties.sid).to.equal('sid-2', 'Invalid map sid');
            expect(res.body.maps[0].geoObjects.features).to.have.length(0, 'Invalid number of objects');
        });

        it('should return map with objects', async () => {
            await context.db.loadFixtures([
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
                            options: objectFixture.options,
                            properties: objectFixture.properties,
                            bbox_max_quad_key: 1,
                            bbox_min_quad_key: 1
                        }
                    ]
                }
            ]);

            const res = await context.server.request('/v1/maps/sid-2', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                    'X-Ya-User-Ticket': UID1_USER_TICKET
                },
                json: true
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);
            expect(res.body.maps).to.have.length(1, 'Invalid number of maps');
            expect(res.body.maps[0].properties.sid).to.equal('sid-2', 'Invalid map sid');
            expect(res.body.maps[0].geoObjects.features).to.have.length(2, 'Invalid number of objects');
        });
    });

    describe('GET /v1/public_maps/{sid}', () => {
        const validateResponseSchema = createResponseSchemaValidator({path: '/v1/public_maps/{sid}'});

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
                }
            ]);
        });

        describe('authentication', () => {
            it('should return 401 for request without service ticket', async () => {
                const res = await context.server.request('/v1/public_maps/sid-1');
                expect(res.statusCode).to.equal(401);
            });

            it('should return 403 for unknown service ticket', async () => {
                const res = await context.server.request('/v1/public_maps/sid-1', {
                    headers: {
                        'X-Ya-Service-Ticket': UNKNOWN_SERVICE_TICKET
                    }
                });
                expect(res.statusCode).to.equal(403);
            });
        });

        it('should return 404 if map does not exist', async () => {
            const res = await context.server.request('/v1/public_maps/sid-4', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                },
                json: true
            });

            expect(res.statusCode).to.equal(404);
            validateResponseSchema(res);
            expect(res.body.type).to.equal('MapNotFoundError');
        });

        it('should return 404 if map is deleted', async () => {
            const res = await context.server.request('/v1/public_maps/sid-1', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                },
                json: true
            });

            expect(res.statusCode).to.equal(404);
            validateResponseSchema(res);
            expect(res.body.type).to.equal('MapNotFoundError');
        });

        it('should return 404 if map is private', async () => {
            const res = await context.server.request('/v1/public_maps/sid-2', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                },
                json: true
            });

            expect(res.statusCode).to.equal(404);
            validateResponseSchema(res);
            expect(res.body.type).to.equal('MapNotFoundError');
        });

        it('should return public map without objects', async () => {
            const res = await context.server.request('/v1/public_maps/sid-3', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                },
                json: true
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);
            expect(res.body.maps).to.have.length(1, 'Invalid number of maps');
            expect(res.body.maps[0].properties.sid).to.equal('sid-3', 'Invalid map sid');
            expect(res.body.maps[0].geoObjects.features).to.have.length(0, 'Invalid number of objects');
        });

        it('should return map with objects', async () => {
            await context.db.loadFixtures([
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
                            options: objectFixture.options,
                            properties: objectFixture.properties,
                            bbox_max_quad_key: 1,
                            bbox_min_quad_key: 1
                        }
                    ]
                }
            ]);

            const res = await context.server.request('/v1/public_maps/sid-3', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                },
                json: true
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);
            expect(res.body.maps).to.have.length(1, 'Invalid number of maps');
            expect(res.body.maps[0].properties.sid).to.equal('sid-3', 'Invalid map sid');
            expect(res.body.maps[0].geoObjects.features).to.have.length(2, 'Invalid number of objects');
        });

        describe('sanitizing', () => {
            beforeEach(async () => {
                await context.db.loadFixtures([
                    {
                        table: 'maps',
                        rows: [
                            {
                                id: 4,
                                sid: 'sid-4',
                                time_created: '2016-07-14 01:00:00',
                                time_updated: '2016-07-14 01:00:00',
                                revision: '1',
                                deleted: false,
                                properties: {
                                    name: '<script>alert()</script>',
                                    access: 'public',
                                    description: '<script>alert()</script>'
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
                                map_id: 4,
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
                                map_id: 4,
                                geometry: {
                                    type: 'Point',
                                    coordinates: [37.62, 55.75]
                                },
                                options: objectFixture.options,
                                properties: {
                                    name: '<script>alert()</script>',
                                    description: '<script>alert()</script>'
                                },
                                // This fields are needed only to satisfy constraints.
                                bbox_max_quad_key: 1,
                                bbox_min_quad_key: 1
                            }
                        ]
                    }
                ]);
            });

            it('should not sanitize map and objects by default', async () => {
                const res = await context.server.request('/v1/public_maps/sid-4', {
                    headers: {
                        'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                    },
                    json: true
                });

                expect(res.statusCode).to.equal(200);
                validateResponseSchema(res);

                const map = res.body.maps[0];
                expect(map.properties.name).to.equal('<script>alert()</script>');
                expect(map.properties.description).to.equal('<script>alert()</script>');

                const feature = map.geoObjects.features[0];
                expect(feature.properties.name).to.equal('<script>alert()</script>');
                expect(feature.properties.description).to.equal('');
            });

            describe('when sanitize parameter is true', () => {
                it('should sanitize map and objects', async () => {
                    const res = await context.server.request('/v1/public_maps/sid-4', {
                        headers: {
                            'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                        },
                        query: {
                            sanitize: true
                        },
                        json: true
                    });

                    expect(res.statusCode).to.equal(200);
                    validateResponseSchema(res);

                    const map = res.body.maps[0];
                    expect(map.properties.name).to.equal('');
                    expect(map.properties.description).to.equal('');

                    const feature = map.geoObjects.features[0];
                    expect(feature.properties.name).to.equal('');
                    expect(feature.properties.description).to.equal('');
                });
            });
        });
    });
};

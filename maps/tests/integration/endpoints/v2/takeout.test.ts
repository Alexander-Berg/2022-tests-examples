import {expect} from 'chai';
import {EndpointSuiteDefinition} from 'tests/integration/endpoints/suite';
import * as tvmTickets from 'tests/integration/endpoints/tvm-tickets';
import {mapFixture, mapFixtureFull} from 'tests/integration/endpoints/fixtures/map';
import {objectFixture} from 'tests/integration/endpoints/fixtures/object';

export const main: EndpointSuiteDefinition = (ctx) => {
    describe('POST /v2/takeout', () => {
        describe('authentication', () => {
            it('should return 401 on invalid service ticket', async () => {
                const res = await ctx.server.request('/v2/takeout', {
                    method: 'POST',
                    headers: {
                        'X-Ya-Service-Ticket': 'invalid_ticket'
                    },
                    form: true,
                    body: {
                        uid: 1
                    }
                });

                expect(res.statusCode).to.equal(401);
            });

            it('should return 403 on request from unknown service', async () => {
                const res = await ctx.server.request('/v2/takeout', {
                    method: 'POST',
                    headers: {
                        'X-Ya-Service-Ticket': tvmTickets.UNKNOWN_SERVICE_TICKET
                    },
                    form: true,
                    body: {
                        uid: 1
                    }
                });

                expect(res.statusCode).to.equal(403);
            });
        });

        it('should return 400 for request without body', async () => {
            const res = await ctx.server.request('/v2/takeout', {
                method: 'POST',
                headers: {
                    'X-Ya-Service-Ticket': tvmTickets.TAKEOUT_SERVICE_TICKET
                }
            });

            expect(res.statusCode).to.equal(400);
        });

        describe('when user with the specified "uid" has not maps', () => {
            it('should return 200 with "no_data" status', async () => {
                const res = await ctx.server.request('/v2/takeout', {
                    method: 'POST',
                    headers: {
                        'X-Ya-Service-Ticket': tvmTickets.TAKEOUT_SERVICE_TICKET
                    },
                    form: true,
                    body: {
                        uid: 1
                    },
                    json: true
                });

                expect(res.statusCode).to.equal(200);
                expect(res.body).to.deep.equal({status: 'no_data'});
            });
        });

        it('should allow unknown body parameters', async () => {
            const res = await ctx.server.request('/v2/takeout', {
                method: 'POST',
                headers: {
                    'X-Ya-Service-Ticket': tvmTickets.TAKEOUT_SERVICE_TICKET
                },
                form: true,
                body: {
                    uid: 1,
                    unixtime: 1234
                },
                json: true
            });

            expect(res.statusCode).to.equal(200);
            expect(res.body).to.deep.equal({status: 'no_data'});
        });

        describe('when user with the specified "uid" has only deleted maps', () => {
            it('should return 200 with "no_data" status', async () => {
                await ctx.db.loadFixtures([
                    {
                        table: 'maps',
                        rows: [
                            {
                                id: 1,
                                sid: 'sid-1',
                                time_created: '2016-07-14 01:00:00',
                                time_updated: '2016-07-14 01:00:00',
                                revision: '1',
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
                                revision: '1',
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
                                uid: 2,
                                role: 'administrator'
                            }
                        ]
                    }
                ]);

                const res = await ctx.server.request('/v2/takeout', {
                    method: 'POST',
                    headers: {
                        'X-Ya-Service-Ticket': tvmTickets.TAKEOUT_SERVICE_TICKET
                    },
                    form: true,
                    body: {
                        uid: 1
                    },
                    json: true
                });

                expect(res.statusCode).to.equal(200);
                expect(res.body).to.deep.equal({status: 'no_data'});
            });
        });

        it('should return "file_links" to existing maps for the specified "uid"', async () => {
            await ctx.db.loadFixtures([
                {
                    table: 'maps',
                    rows: [
                        {
                            id: 1,
                            sid: 'sid-1',
                            time_created: '2016-07-14 01:00:00',
                            time_updated: '2016-07-14 01:00:00',
                            revision: '1',
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
                            revision: '1',
                            deleted: false,
                            properties: mapFixture.properties,
                            options: mapFixture.options,
                            state: mapFixture.state
                        },
                        {
                            id: 3,
                            sid: 'sid-3',
                            time_created: '2016-07-14 01:00:00',
                            time_updated: '2016-07-14 01:00:00',
                            revision: '1',
                            deleted: false,
                            properties: mapFixture.properties,
                            options: mapFixture.options,
                            state: mapFixture.state
                        },
                        {
                            id: 4,
                            sid: 'sid-4',
                            time_created: '2016-07-14 01:00:00',
                            time_updated: '2016-07-14 01:00:00',
                            revision: '1',
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
                            uid: 2,
                            role: 'administrator'
                        },
                        {
                            map_id: 3,
                            uid: 1,
                            role: 'administrator'
                        },
                        {
                            map_id: 4,
                            uid: 1,
                            role: 'administrator'
                        }
                    ]
                }
            ]);

            const res = await ctx.server.request('/v2/takeout', {
                method: 'POST',
                headers: {
                    'X-Ya-Service-Ticket': tvmTickets.TAKEOUT_SERVICE_TICKET
                },
                form: true,
                body: {
                    uid: 1
                },
                json: true
            });

            expect(res.statusCode).to.equal(200);
            expect(res.body.status).to.equal('ok');
            expect(res.body.file_links).to.have.members([
                'http://127.0.0.1/v2/takeout/maps/sid-3',
                'http://127.0.0.1/v2/takeout/maps/sid-4'
            ]);
        });

        it('should return "file_links" to maps where user has "administrator" role', async () => {
            await ctx.db.loadFixtures([
                {
                    table: 'maps',
                    rows: [
                        {
                            id: 1,
                            sid: 'sid-1',
                            time_created: '2016-07-14 01:00:00',
                            time_updated: '2016-07-14 01:00:00',
                            revision: '1',
                            deleted: false,
                            properties: mapFixture.properties,
                            options: mapFixture.options,
                            state: mapFixture.state
                        },
                        {
                            id: 2,
                            sid: 'sid-2',
                            time_created: '2016-07-14 01:00:00',
                            time_updated: '2016-07-14 01:00:00',
                            revision: '1',
                            deleted: false,
                            properties: mapFixture.properties,
                            options: mapFixture.options,
                            state: mapFixture.state
                        },
                        {
                            id: 3,
                            sid: 'sid-3',
                            time_created: '2016-07-14 01:00:00',
                            time_updated: '2016-07-14 01:00:00',
                            revision: '1',
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
                            role: 'moderator'
                        },
                        {
                            map_id: 3,
                            uid: 1,
                            role: 'spectator'
                        }
                    ]
                }
            ]);

            const res = await ctx.server.request('/v2/takeout', {
                method: 'POST',
                headers: {
                    'X-Ya-Service-Ticket': tvmTickets.TAKEOUT_SERVICE_TICKET
                },
                form: true,
                body: {
                    uid: 1
                },
                json: true
            });

            expect(res.statusCode).to.equal(200);
            expect(res.body.status).to.equal('ok');
            expect(res.body.file_links).to.have.members([
                'http://127.0.0.1/v2/takeout/maps/sid-1'
            ]);
        });
    });

    describe('GET /v2/takeout/maps/{sid}', () => {
        it('should return 401 on invalid service ticket', async () => {
            const res = await ctx.server.request('/v2/takeout/maps/sid-1', {
                headers: {
                    'X-Ya-Service-Ticket': 'invalid_ticket'
                },
                json: true
            });

            expect(res.statusCode).to.equal(401);
        });

        it('should return 403 on request from unknown service', async () => {
            const res = await ctx.server.request('/v2/takeout/maps/sid-1', {
                headers: {
                    'X-Ya-Service-Ticket': tvmTickets.UNKNOWN_SERVICE_TICKET
                },
                json: true
            });

            expect(res.statusCode).to.equal(403);
        });

        it('should return 404 if map not found', async () => {
            const res = await ctx.server.request('/v2/takeout/maps/sid-1', {
                headers: {
                    'X-Ya-Service-Ticket': tvmTickets.TAKEOUT_SERVICE_TICKET
                },
                json: true
            });

            expect(res.statusCode).to.equal(404);
            expect(res.body.type).to.equal('MapNotFoundError');
        });

        it('should return map with objects by sid', async () => {
            await ctx.db.loadFixtures([
                {
                    table: 'maps',
                    rows: [
                        {
                            id: 1,
                            sid: 'sid-1',
                            time_created: '2016-07-14 01:00:00',
                            time_updated: '2016-07-14 01:00:00',
                            revision: '1',
                            deleted: false,
                            properties: {
                                ...mapFixture.properties,
                                name: 'Map name',
                                description: 'Map description'
                            },
                            options: mapFixture.options,
                            state: mapFixture.state
                        },
                        {
                            id: 2,
                            sid: 'sid-2',
                            time_created: '2016-07-14 01:00:00',
                            time_updated: '2016-07-14 01:00:00',
                            revision: '1',
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
                            map_id: 1,
                            geometry: {
                                type: 'Point',
                                coordinates: [1, 2]
                            },
                            properties: {
                                ...objectFixture.properties,
                                name: 'Point name',
                                description: 'Point description'
                            },
                            options: objectFixture.options,
                            bbox_max_quad_key: 1,
                            bbox_min_quad_key: 1
                        },
                        {
                            id: 2,
                            map_id: 1,
                            geometry: {
                                type: 'LineString',
                                coordinates: [[1, 2], [3, 4]]
                            },
                            properties: {
                                ...objectFixture.properties,
                                name: 'LineString name',
                                description: 'LineString description'
                            },
                            options: objectFixture.options,
                            bbox_max_quad_key: 1,
                            bbox_min_quad_key: 1
                        },
                        {
                            id: 3,
                            map_id: 2,
                            geometry: {
                                type: 'Point',
                                coordinates: [1, 2]
                            },
                            properties: objectFixture.properties,
                            options: objectFixture.options,
                            bbox_max_quad_key: 1,
                            bbox_min_quad_key: 1
                        }
                    ]
                }
            ]);

            const res = await ctx.server.request('/v2/takeout/maps/sid-1', {
                headers: {
                    'X-Ya-Service-Ticket': tvmTickets.TAKEOUT_SERVICE_TICKET
                },
                json: true
            });

            expect(res.statusCode).to.equal(200);
            expect(res.headers['content-disposition']).to.equal(
                'attachment; filename="map_1.json"'
            );

            const body = res.body;
            expect(Object.keys(body)).to.have.members(['type', 'properties', 'features']);
            expect(body.type).to.equal('FeatureCollection');
            expect(body.properties).to.deep.equal({
                name: 'Map name',
                description: 'Map description'
            });
            expect(body.features).to.have.deep.members([
                {
                    type: 'Feature',
                    geometry: {
                        type: 'Point',
                        coordinates: [1, 2]
                    },
                    properties: {
                        name: 'Point name',
                        description: 'Point description'
                    },
                    options: {}
                },
                {
                    type: 'Feature',
                    geometry: {
                        type: 'LineString',
                        coordinates: [[1, 2], [3, 4]]
                    },
                    properties: {
                        name: 'LineString name',
                        description: 'LineString description'
                    },
                    options: {}
                }
            ]);
        });
    });

    const describePassportTakeoutAuthentication = (method: string, url: string) => {
        describe('authentication', () => {
            it('should return 401 on invalid service ticket', async () => {
                const res = await ctx.server.request(`${url}`, {
                    method,
                    headers: {'X-Ya-Service-Ticket': 'invalid_ticket'},
                    json: true
                });

                expect(res.statusCode).to.equal(401);
            });

            it('should return 403 on request from unknown service', async () => {
                const res = await ctx.server.request(`${url}`, {
                    method,
                    headers: {
                        'X-Ya-Service-Ticket': tvmTickets.UNKNOWN_SERVICE_TICKET,
                        'X-Ya-User-Ticket': tvmTickets.UID1_USER_TICKET
                    },
                    json: true
                });

                expect(res.statusCode).to.equal(403);
            });

            it('should return 401 on request without user ticket', async () => {
                const res = await ctx.server.request(`${url}?request_id=12345`, {
                    method,
                    headers: {'X-Ya-Service-Ticket': tvmTickets.PASSPORT_SERVICE_TICKET},
                    json: true
                });

                expect(res.statusCode).to.equal(401);
            });

            it('should return 403 on invalid user ticket service', async () => {
                const res = await ctx.server.request(`${url}`, {
                    method,
                    headers: {
                        'X-Ya-Service-Ticket': tvmTickets.PASSPORT_SERVICE_TICKET,
                        'X-Ya-User-Ticket': 'invalid'
                    },
                    json: true
                });

                expect(res.statusCode).to.equal(401);
            });

            it('should return 400 if request_id is not passed', async () => {
                const res = await ctx.server.request(`${url}`, {
                    method,
                    headers: {
                        'X-Ya-Service-Ticket': tvmTickets.PASSPORT_SERVICE_TICKET,
                        'X-Ya-User-Ticket': tvmTickets.UID1_USER_TICKET
                    },
                    json: true
                });

                expect(res.statusCode).to.equal(400);
            });
        });
    };

    describe('POST /v2/takeout/delete', () => {
        describePassportTakeoutAuthentication('POST', '/v2/takeout/delete');

        const commonHeadersUid1 = {
            method: 'POST',
            headers: {
                'X-Ya-Service-Ticket': tvmTickets.PASSPORT_SERVICE_TICKET,
                'X-Ya-User-Ticket': tvmTickets.UID1_USER_TICKET
            }
        };

        describe('validation', () => {
            it('should return 400 if body.id array is not passed', async () => {
                const res = await ctx.server.request('/v2/takeout/delete?request_id=12345', {
                    ...commonHeadersUid1,
                    json: true
                });

                expect(res.statusCode).to.equal(400);
            });

            it('should return error if body.id array is not exactly ["1"] #1', async () => {
                const res1 = await ctx.server.request('/v2/takeout/delete?request_id=12345', {
                    ...commonHeadersUid1,
                    json: true,
                    body: {id: ['1', '2']}
                });

                expect(res1.statusCode).to.equal(200);
                expect(res1.body?.status).to.equal('error');
                expect(res1.body?.errors.length).to.equal(1);
                expect(res1.body?.errors[0].code).to.equal('bad_id');
            });

            it('should return error if body.id array is not exactly ["1"] #2', async () => {
                const res2 = await ctx.server.request('/v2/takeout/delete?request_id=12345', {
                    ...commonHeadersUid1,
                    json: true,
                    body: {id: ['2']}
                });

                expect(res2.statusCode).to.equal(200);
                expect(res2.body?.status).to.equal('error');
                expect(res2.body?.errors.length).to.equal(1);
                expect(res2.body?.errors[0].code).to.equal('bad_id');
            });
        });

        const createMaps = async () => {
            const object = (id: number, map_id: number) => ({
                id,
                map_id,
                geometry: {
                    type: 'Point',
                    coordinates: [1, 2]
                },
                properties: {
                    ...objectFixture.properties,
                    name: 'Point name',
                    description: 'Point description'
                },
                options: objectFixture.options,
                bbox_max_quad_key: 1,
                bbox_min_quad_key: 1
            });

            await ctx.db.loadFixtures({
                maps: [
                    {id: 1, sid: 'sid-1', ...mapFixtureFull},
                    {id: 2, sid: 'sid-2', ...mapFixtureFull, deleted: true},
                    {id: 3, sid: 'sid-3', ...mapFixtureFull},
                    {id: 4, sid: 'sid-4', ...mapFixtureFull}
                ],
                maps_users: [
                    {map_id: 1, uid: 1, role: 'administrator'},
                    {map_id: 2, uid: 1, role: 'administrator'},
                    {map_id: 3, uid: 9, role: 'administrator'},
                    {map_id: 4, uid: 9, role: 'administrator'}
                ],
                objects: [
                    object(1, 1),
                    object(3, 3)
                ]
            });
        };

        it('should delete all maps of the user', async () => {
            await createMaps();

            const res = await ctx.server.request('/v2/takeout/delete?request_id=12345', {
                ...commonHeadersUid1,
                json: true,
                body: {id: ['1']} as any
            });

            expect(res.statusCode).to.equal(200);
            expect(res.body).to.deep.equal({id: ['1']});

            const maps = await ctx.db.query('SELECT id FROM maps ORDER BY id');
            expect(maps.rows).to.deep.equal([{id: '3'}, {id: '4'}]);

            const objects = await ctx.db.query('SELECT id FROM objects ORDER BY id');
            expect(objects.rows).to.deep.equal([{id: '3'}]);
        });

        it('should delete all roles of the user', async () => {
            await createMaps();
            await ctx.db.loadFixtures({
                maps_users: [
                    {map_id: 3, uid: 1, role: 'moderator'},
                    {map_id: 4, uid: 1, role: 'spectator'}
                ]
            });

            const res = await ctx.server.request('/v2/takeout/delete?request_id=12345', {
                ...commonHeadersUid1,
                json: true,
                body: {id: ['1']} as any
            });

            expect(res.statusCode).to.equal(200);
            expect(res.body).to.deep.equal({id: ['1']});

            const roles = await ctx.db.query('SELECT map_id, uid, role FROM maps_users ORDER BY map_id, uid');
            expect(roles.rows).to.deep.equal([
                {map_id: '3', uid: '9', role: 'administrator'},
                {map_id: '4', uid: '9', role: 'administrator'}
            ]);
        });

        it('should write deleted maps to deleted_maps_log table', async () => {
            await createMaps();

            const res = await ctx.server.request('/v2/takeout/delete?request_id=12345', {
                ...commonHeadersUid1,
                json: true,
                body: {id: ['1']} as any
            });

            expect(res.statusCode).to.equal(200);
            expect(res.body).to.deep.equal({id: ['1']});

            const roles = await ctx.db.query('SELECT sid, uid FROM deleted_maps_log ORDER BY sid');
            expect(roles.rows).to.deep.equal([
                {sid: 'sid-1', uid: '1'},
                {sid: 'sid-2', uid: '1'}
            ]);
        });
    });

    describe('GET /v2/takeout/status', () => {
        describePassportTakeoutAuthentication('GET', '/v2/takeout/status');

        const commonOptions = {
            headers: {
                'X-Ya-Service-Ticket': tvmTickets.PASSPORT_SERVICE_TICKET,
                'X-Ya-User-Ticket': tvmTickets.UID1_USER_TICKET
            },
            json: true
        };

        it('should return `empty` status if user has no data', async () => {
            const res = await ctx.server.request('/v2/takeout/status?request_id=12345', commonOptions);

            expect(res.statusCode).to.equal(200);
            expect(res.body).to.deep.equal({
                status: 'ok',
                data: [{
                    id: '1',
                    slug: 'maps',
                    state: 'empty'
                }]
            });
        });

        it('should return `ready_to_delete` status if user has some maps', async () => {
            await ctx.db.loadFixtures({
                maps: [
                    {id: 1, sid: 'sid-1', ...mapFixtureFull},
                    {id: 2, sid: 'sid-2', ...mapFixtureFull}
                ],
                maps_users: [
                    {map_id: 1, uid: 1, role: 'administrator'},
                    {map_id: 2, uid: 1, role: 'administrator'}
                ]
            });

            const res = await ctx.server.request('/v2/takeout/status?request_id=12345', commonOptions);

            expect(res.statusCode).to.equal(200);
            expect(res.body).to.deep.equal({
                status: 'ok',
                data: [{
                    id: '1',
                    slug: 'maps',
                    state: 'ready_to_delete'
                }]
            });
        });
    });
};

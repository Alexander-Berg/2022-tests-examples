import {expect} from 'chai';
import {EndpointSuiteDefinition} from 'tests/integration/endpoints/suite';
import {createResponseSchemaValidator} from 'tests/integration/response-validator';
import {
    UNKNOWN_SERVICE_TICKET,
    SELF_SERVICE_TICKET,
    UID1_USER_TICKET,
    UID2_USER_TICKET,
    UID3_USER_TICKET
} from 'tests/integration/endpoints/tvm-tickets';
import {mapFixture} from 'tests/integration/endpoints/fixtures/map';

const validateResponseSchema = createResponseSchemaValidator({
    path: '/v2/restoring/maps/{sid}',
    method: 'put'
});

export const main: EndpointSuiteDefinition = (context) => {
    describe('PUT /v2/restoring/maps/{sid}', () => {
        describe('authentication', () => {
            it('should return 401 for request without tickets', async () => {
                const res = await context.server.request('/v2/restoring/maps/sid-1', {
                    method: 'PUT'
                });
                expect(res.statusCode).to.equal(401);
            });

            it('should return 401 for request without service ticket', async () => {
                const res = await context.server.request('/v2/restoring/maps/sid-1', {
                    method: 'PUT',
                    headers: {
                        'X-Ya-User-Ticket': UID1_USER_TICKET
                    }
                });
                expect(res.statusCode).to.equal(401);
            });

            it('should return 401 for request without user ticket', async () => {
                const res = await context.server.request('/v2/restoring/maps/sid-1', {
                    method: 'PUT',
                    headers: {
                        'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                    }
                });
                expect(res.statusCode).to.equal(401);
            });

            it('should return 403 for unknown service ticket', async () => {
                const res = await context.server.request('/v2/restoring/maps/sid-1', {
                    method: 'PUT',
                    headers: {
                        'X-Ya-Service-Ticket': UNKNOWN_SERVICE_TICKET,
                        'X-Ya-User-Ticket': UID1_USER_TICKET
                    }
                });
                expect(res.statusCode).to.equal(403);
            });
        });

        it('should return 404 if map does not exist', async () => {
            const res = await context.server.request('/v2/restoring/maps/sid-1', {
                method: 'PUT',
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

        it('should return 200 if map is not deleted', async () => {
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
                        }
                    ]
                }
            ]);

            const res = await context.server.request('/v2/restoring/maps/sid-1', {
                method: 'PUT',
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                    'X-Ya-User-Ticket': UID1_USER_TICKET
                },
                json: true
            });
            expect(res.statusCode).to.equal(200);
            expect(res.headers.location).to.match(
                /\/v2\/maps\/sid-1$/,
                'Invalid Location header value'
            );
        });

        describe('rights verifying', () => {
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
                                revision: '0',
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
                                role: 'moderator'
                            },
                            {
                                map_id: 1,
                                uid: 2,
                                role: 'spectator'
                            },
                            {
                                map_id: 2,
                                uid: 3,
                                role: 'administrator'
                            }
                        ]
                    }
                ]);
            });

            it('should return 403 if user role is "moderator"', async () => {
                const res = await context.server.request('/v2/restoring/maps/sid-1', {
                    method: 'PUT',
                    headers: {
                        'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                        'X-Ya-User-Ticket': UID1_USER_TICKET
                    },
                    json: true
                });
                expect(res.statusCode).to.equal(403);
                validateResponseSchema(res);
                expect(res.body.type).to.equal('EditMapForbiddenError');
            });

            it('should return 403 if user role is "spectator"', async () => {
                const res = await context.server.request('/v2/restoring/maps/sid-1', {
                    method: 'PUT',
                    headers: {
                        'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                        'X-Ya-User-Ticket': UID2_USER_TICKET
                    },
                    json: true
                });
                expect(res.statusCode).to.equal(403);
                validateResponseSchema(res);
                expect(res.body.type).to.equal('EditMapForbiddenError');
            });

            it('should return 404 if user does not own the map', async () => {
                const res = await context.server.request('/v2/restoring/maps/sid-1', {
                    method: 'PUT',
                    headers: {
                        'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                        'X-Ya-User-Ticket': UID3_USER_TICKET
                    },
                    json: true
                });
                expect(res.statusCode).to.equal(404);
                validateResponseSchema(res);
                expect(res.body.type).to.equal('MapNotFoundError');
            });
        });

        it('should mark map as not deleted and unset delete time', async () => {
            await context.db.loadFixtures([
                {
                    table: 'maps',
                    rows: [
                        {
                            id: 1,
                            sid: 'sid-1',
                            time_created: '2016-07-14 01:00:00',
                            time_updated: '2016-07-14 01:00:00',
                            time_deleted: '2017-07-14 01:00:00',
                            revision: '0',
                            deleted: true,
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
                        }
                    ]
                }
            ]);

            const res = await context.server.request('/v2/restoring/maps/sid-1', {
                method: 'PUT',
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                    'X-Ya-User-Ticket': UID1_USER_TICKET
                },
                json: true
            });
            expect(res.statusCode).to.equal(200);
            expect(res.headers.location).to.match(
                /\/v2\/maps\/sid-1$/,
                'Invalid Location header value'
            );

            const result = await context.db.query(
                `SELECT
                    maps.deleted,
                    maps.time_deleted
                FROM maps
                JOIN maps_users ON maps.id = maps_users.map_id`
            );

            expect(result.rowCount).to.equal(1);
            const row = result.rows[0];
            expect(row.deleted).to.equal(false);
            expect(row.time_deleted).to.equal(null);
        });
    });
};

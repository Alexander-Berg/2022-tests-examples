import {expect} from 'chai';
import {EndpointSuiteDefinition} from 'tests/integration/endpoints/suite';
import {createResponseSchemaValidator} from 'tests/integration/response-validator';
import {
    UNKNOWN_SERVICE_TICKET,
    SELF_SERVICE_TICKET,
    UID1_USER_TICKET,
    UID2_USER_TICKET
} from 'tests/integration/endpoints/tvm-tickets';
import {mapFixture} from '../fixtures/map';

const validateResponseSchema = createResponseSchemaValidator({
    path: '/v2/maps/{sid}',
    method: 'delete'
});

export const main: EndpointSuiteDefinition = (context) => {
    describe('DELETE /v2/maps/{sid}', () => {
        describe('authentication', () => {
            it('should return 401 for request without tickets', async () => {
                const res = await context.server.request('/v2/maps/sid-1', {
                    method: 'DELETE'
                });
                expect(res.statusCode).to.equal(401);
            });

            it('should return 401 for request without service ticket', async () => {
                const res = await context.server.request('/v2/maps/sid-1', {
                    method: 'DELETE',
                    headers: {
                        'X-Ya-User-Ticket': UID1_USER_TICKET
                    }
                });
                expect(res.statusCode).to.equal(401);
            });

            it('should return 401 for request without user ticket', async () => {
                const res = await context.server.request('/v2/maps/sid-1', {
                    method: 'DELETE',
                    headers: {
                        'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                    }
                });
                expect(res.statusCode).to.equal(401);
            });

            it('should return 403 for unknown service ticket', async () => {
                const res = await context.server.request('/v2/maps/sid-1', {
                    method: 'DELETE',
                    headers: {
                        'X-Ya-Service-Ticket': UNKNOWN_SERVICE_TICKET,
                        'X-Ya-User-Ticket': UID1_USER_TICKET
                    }
                });
                expect(res.statusCode).to.equal(403);
            });
        });

        it('should return 404 if map does not exist', async () => {
            const res = await context.server.request('/v2/maps/sid-1', {
                method: 'DELETE',
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

            const res = await context.server.request('/v2/maps/sid-1', {
                method: 'DELETE',
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                    'X-Ya-User-Ticket': UID1_USER_TICKET
                },
                json: true
            });

            validateResponseSchema(res);
            expect(res.body.type).to.equal('MapNotFoundError');
        });

        describe('rights verifying', () => {
            beforeEach(() => {
                return context.db.loadFixtures([
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
                                role: 'moderator'
                            },
                            {
                                map_id: 1,
                                uid: 2,
                                role: 'spectator'
                            }
                        ]
                    }
                ]);
            });

            it('should return 403 if user role is "moderator"', async () => {
                const res = await context.server.request('/v2/maps/sid-1', {
                    method: 'DELETE',
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
                const res = await context.server.request('/v2/maps/sid-1', {
                    method: 'DELETE',
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
        });

        it('should mark map with given "sid" as deleted and set delete time', async () => {
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

            // Get timestamp from database, because local time can be different.
            let result = await context.db.query('SELECT now()');
            const beforeDeleteTime = result.rows[0].now;

            const res = await context.server.request('/v2/maps/sid-1', {
                method: 'DELETE',
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                    'X-Ya-User-Ticket': UID1_USER_TICKET
                },
                json: true
            });

            expect(res.statusCode).to.equal(204);

            result = await context.db.query(
                `SELECT
                    maps.deleted,
                    maps.time_deleted,
                    now()
                FROM maps
                JOIN maps_users ON maps.id = maps_users.map_id`
            );

            expect(result.rowCount).to.equal(1);
            const row = result.rows[0];
            expect(row.deleted).to.equal(true);

            expect(row.time_deleted.getTime()).to.be.above(beforeDeleteTime.getTime())
                .and.to.be.below(row.now.getTime());
        });
    });
};

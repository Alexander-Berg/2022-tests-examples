import * as jspath from 'jspath';
import {expect} from 'chai';
import {EndpointSuiteDefinition} from 'tests/integration/endpoints/suite';
import {createResponseSchemaValidator} from 'tests/integration/response-validator';
import {
    UNKNOWN_SERVICE_TICKET,
    SELF_SERVICE_TICKET,
    UID1_USER_TICKET
} from 'tests/integration/endpoints/tvm-tickets';
import {mapFixture} from 'tests/integration/endpoints/fixtures/map';

const validateResponseSchema = createResponseSchemaValidator({path: '/v2/maps'});

export const main: EndpointSuiteDefinition = (context) => {
    describe('GET /v2/maps', () => {
        describe('authentication', () => {
            it('should return 401 for request without tickets', async () => {
                const res = await context.server.request('/v2/maps');
                expect(res.statusCode).to.equal(401);
            });

            it('should return 401 for request without service ticket', async () => {
                const res = await context.server.request('/v2/maps', {
                    headers: {
                        'X-Ya-User-Ticket': UID1_USER_TICKET
                    }
                });
                expect(res.statusCode).to.equal(401);
            });

            it('should return 401 for request without user ticket', async () => {
                const res = await context.server.request('/v2/maps', {
                    headers: {
                        'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                    }
                });
                expect(res.statusCode).to.equal(401);
            });

            it('should return 403 for unknown service ticket', async () => {
                const res = await context.server.request('/v2/maps', {
                    headers: {
                        'X-Ya-Service-Ticket': UNKNOWN_SERVICE_TICKET,
                        'X-Ya-User-Ticket': UID1_USER_TICKET
                    }
                });
                expect(res.statusCode).to.equal(403);
            });
        });

        it('should return empty list if user does not have maps', async () => {
            const res = await context.server.request('/v2/maps', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                    'X-Ya-User-Ticket': UID1_USER_TICKET
                },
                query: {
                    limit: 10
                },
                json: true
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);
            expect(res.body.totalCount).to.equal(0, 'Invalid total count of maps');
            expect(res.body.items).to.have.length(0, 'Invalid count of maps');
        });

        it('should return list of maps of given user', async () => {
            const fixtures = [
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
                        },
                        {
                            id: 3,
                            sid: 'sid-3',
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
                        },
                        {
                            map_id: 2,
                            uid: 1,
                            role: 'administrator'
                        },
                        {
                            map_id: 3,
                            uid: 2,
                            role: 'administrator'
                        }
                    ]
                }
            ];

            await context.db.loadFixtures(fixtures);

            const res = await context.server.request('/v2/maps', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                    'X-Ya-User-Ticket': UID1_USER_TICKET
                },
                query: {
                    limit: 10
                },
                json: true
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);
            expect(res.body.totalCount).to.equal(2, 'Invalid total count of maps');
            expect(res.body.items).to.have.length(2, 'Invalid count of maps');
            expect(jspath('.items.sid', res.body)).to.have.members(
                ['sid-1', 'sid-2'],
                'Invalid maps in response'
            );
        });

        it('should not return deleted maps', async () => {
            const fixtures = [
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
                            properties: mapFixture.properties,
                            options: mapFixture.options,
                            state: mapFixture.state
                        },
                        {
                            id: 3,
                            sid: 'sid-3',
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
            ];

            await context.db.loadFixtures(fixtures);

            const res = await context.server.request('/v2/maps', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                    'X-Ya-User-Ticket': UID1_USER_TICKET
                },
                query: {
                    limit: 10
                },
                json: true
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);
            expect(res.body.totalCount).to.equal(1, 'Invalid total count of maps');
            expect(res.body.items).to.have.length(1, 'Invalid count of maps');
            expect(res.body.items[0].sid).to.equal('sid-2', 'Invalid map in response');
        });

        it('should sort maps by update time', async () => {
            const fixtures = [
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
                            time_updated: '2016-07-14 03:00:00',
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
                            time_updated: '2016-07-14 02:00:00',
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
                        },
                        {
                            map_id: 3,
                            uid: 1,
                            role: 'administrator'
                        }
                    ]
                }
            ];

            await context.db.loadFixtures(fixtures);

            const res = await context.server.request('/v2/maps', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                    'X-Ya-User-Ticket': UID1_USER_TICKET
                },
                query: {
                    limit: 10
                },
                json: true
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);
            expect(res.body.totalCount).to.equal(3, 'Invalid total count of maps');
            expect(res.body.items).to.have.length(3, 'Invalid count of maps');
            expect(jspath('.items.sid', res.body)).to.deep.equal(
                ['sid-2', 'sid-3', 'sid-1'],
                'Invalid maps order in response'
            );
        });

        describe('user role selection', () => {
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
                            },
                            {
                                id: 3,
                                sid: 'sid-3',
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
                            },
                            {
                                map_id: 2,
                                uid: 1,
                                role: 'moderator'
                            },
                            {
                                map_id: 3,
                                uid: 1,
                                role: 'moderator'
                            }
                        ]
                    }
                ]);
            });

            it('should return maps with "administrator" role by default', async () => {
                const res = await context.server.request('/v2/maps', {
                    headers: {
                        'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                        'X-Ya-User-Ticket': UID1_USER_TICKET
                    },
                    query: {
                        limit: 10
                    },
                    json: true
                });

                expect(res.statusCode).to.equal(200);
                validateResponseSchema(res);
                expect(res.body.items).to.have.length(1, 'Invalid count of maps');
                expect(res.body.items[0].sid).to.equal('sid-1', 'Invalid maps in response');
            });

            it('should return maps with "moderator" role if "role" query param is "moderator"', async () => {
                const res = await context.server.request('/v2/maps', {
                    headers: {
                        'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                        'X-Ya-User-Ticket': UID1_USER_TICKET
                    },
                    query: {
                        limit: 10,
                        role: 'moderator'
                    },
                    json: true
                });

                expect(res.statusCode).to.equal(200);
                validateResponseSchema(res);
                expect(res.body.items).to.have.length(2, 'Invalid count of maps');
                expect(jspath('.items.sid', res.body)).to.have.members(
                    ['sid-2', 'sid-3'],
                    'Invalid maps in response'
                );
            });
        });

        describe('pagination', () => {
            const TOTAL_COUNT = 10;

            function range(start: number, stop: number): number[] {
                const result = [];
                for (let i = start; i <= stop; i++) {
                    result.push(i);
                }
                return result;
            }

            beforeEach(() => {
                const fixtures = [
                    {
                        table: 'maps',
                        rows: range(1, TOTAL_COUNT).map((id) => {
                            const time = (new Date(id)).toISOString();
                            return {
                                id: id,
                                sid: `sid-${id}`,
                                time_created: time,
                                time_updated: time,
                                revision: '0',
                                deleted: false,
                                properties: mapFixture.properties,
                                options: mapFixture.options,
                                state: mapFixture.state
                            };
                        })
                    },
                    {
                        table: 'maps_users',
                        rows: range(1, TOTAL_COUNT).map((mapId) => {
                            return {
                                map_id: mapId,
                                uid: 1,
                                role: 'administrator'
                            };
                        })
                    }
                ];

                return context.db.loadFixtures(fixtures);
            });

            it('should apply "offset" and "limit" query parameters', async () => {
                const res = await context.server.request('/v2/maps', {
                    headers: {
                        'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                        'X-Ya-User-Ticket': UID1_USER_TICKET
                    },
                    query: {
                        offset: 1,
                        limit: 2
                    },
                    json: true
                });

                expect(res.statusCode).to.equal(200);
                validateResponseSchema(res);
                expect(res.body.totalCount).to.equal(TOTAL_COUNT, 'Invalid total count of maps');
                expect(res.body.items).to.have.length(2, 'Invalid count of maps');
                expect(jspath('.items.sid', res.body)).to.deep.equal(
                    ['sid-9', 'sid-8'],
                    'Invalid maps in response'
                );
            });

            it('should return correct total count if offset is greater than or equal to total count', async () => {
                const res = await context.server.request('/v2/maps', {
                    headers: {
                        'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                        'X-Ya-User-Ticket': UID1_USER_TICKET
                    },
                    query: {
                        offset: TOTAL_COUNT,
                        limit: 2
                    },
                    json: true
                });

                expect(res.statusCode).to.equal(200);
                validateResponseSchema(res);
                expect(res.body.totalCount).to.equal(TOTAL_COUNT, 'Invalid total count of maps');
                expect(res.body.items).to.have.length(0, 'Invalid count of maps');
            });

            it('should not compare total count and offset as strings (issue #152)', async () => {
                const offset = 4;
                expect(String(offset) > String(TOTAL_COUNT)).to.be.true;
                expect(offset < TOTAL_COUNT).to.be.true;

                const res = await context.server.request('/v2/maps', {
                    headers: {
                        'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                        'X-Ya-User-Ticket': UID1_USER_TICKET
                    },
                    query: {
                        offset,
                        limit: 4
                    },
                    json: true
                });

                expect(res.statusCode).to.equal(200);
                validateResponseSchema(res);
                expect(res.body.totalCount).to.equal(TOTAL_COUNT, 'Invalid total count of maps');
                expect(res.body.items).to.have.length(4, 'Invalid count of maps');
            });
        });
    });
};

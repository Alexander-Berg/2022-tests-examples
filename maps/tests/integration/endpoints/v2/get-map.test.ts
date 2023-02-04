import {expect} from 'chai';
import {EndpointSuiteDefinition} from 'tests/integration/endpoints/suite';
import {createResponseSchemaValidator} from 'tests/integration/response-validator';
import {
    UNKNOWN_SERVICE_TICKET,
    SELF_SERVICE_TICKET,
    UID1_USER_TICKET
} from 'tests/integration/endpoints/tvm-tickets';
import {mapFixture} from 'tests/integration/endpoints/fixtures/map';

export const main: EndpointSuiteDefinition = (context) => {
    describe('GET /v2/maps/{sid}', () => {
        const validateResponseSchema = createResponseSchemaValidator({path: '/v2/maps/{sid}'});

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
                const res = await context.server.request('/v2/maps/sid-1');
                expect(res.statusCode).to.equal(401);
            });

            it('should return 401 for request without service ticket', async () => {
                const res = await context.server.request('/v2/maps/sid-1', {
                    headers: {
                        'X-Ya-User-Ticket': UID1_USER_TICKET
                    }
                });
                expect(res.statusCode).to.equal(401);
            });

            it('should return 401 for request without user ticket', async () => {
                const res = await context.server.request('/v2/maps/sid-1', {
                    headers: {
                        'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                    }
                });
                expect(res.statusCode).to.equal(401);
            });

            it('should return 403 for unknown service ticket', async () => {
                const res = await context.server.request('/v2/maps/sid-1', {
                    headers: {
                        'X-Ya-Service-Ticket': UNKNOWN_SERVICE_TICKET,
                        'X-Ya-User-Ticket': UID1_USER_TICKET
                    }
                });
                expect(res.statusCode).to.equal(403);
            });
        });

        it('should return 404 if map not found', async () => {
            const res = await context.server.request('/v2/maps/sid-3', {
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
            const res = await context.server.request('/v2/maps/sid-1', {
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

        it('should return map by sid', async () => {
            const res = await context.server.request('/v2/maps/sid-2', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                    'X-Ya-User-Ticket': UID1_USER_TICKET
                },
                json: true
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);
            expect(res.body.sid).to.equal('sid-2', 'Invalid map sid');
            expect(res.body.revision).to.equal('5', 'Invalid map revision');
        });
    });

    describe('GET /v2/public_maps/{sid}', () => {
        const validateResponseSchema = createResponseSchemaValidator({path: '/v2/public_maps/{sid}'});

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
                            properties: mapFixture.properties,
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
                                ...mapFixture.properties,
                                access: 'public'
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
                const res = await context.server.request('/v2/public_maps/sid-1');
                expect(res.statusCode).to.equal(401);
            });

            it('should return 403 for unknown service ticket', async () => {
                const res = await context.server.request('/v2/public_maps/sid-1', {
                    headers: {
                        'X-Ya-Service-Ticket': UNKNOWN_SERVICE_TICKET
                    }
                });
                expect(res.statusCode).to.equal(403);
            });
        });

        it('should return 404 if map does not exist', async () => {
            const res = await context.server.request('/v2/public_maps/sid-4', {
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
            const res = await context.server.request('/v2/public_maps/sid-1', {
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
            const res = await context.server.request('/v2/public_maps/sid-2', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                },
                json: true
            });

            expect(res.statusCode).to.equal(404);
            validateResponseSchema(res);
            expect(res.body.type).to.equal('MapNotFoundError');
        });

        it('should return public map', async () => {
            const res = await context.server.request('/v2/public_maps/sid-3', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                },
                json: true
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);
            expect(res.body.sid).to.equal('sid-3', 'Invalid map sid');
            expect(res.body.revision).to.equal('10', 'Invalid map revision');
        });
    });
};

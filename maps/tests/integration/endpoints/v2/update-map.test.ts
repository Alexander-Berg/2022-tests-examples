import * as got from 'got';
import {expect} from 'chai';
import {EndpointSuiteDefinition} from 'tests/integration/endpoints/suite';
import {createResponseSchemaValidator} from 'tests/integration/response-validator';
import {
    UNKNOWN_SERVICE_TICKET,
    SELF_SERVICE_TICKET,
    UID1_USER_TICKET
} from 'tests/integration/endpoints/tvm-tickets';
import {mapFixture} from 'tests/integration/endpoints/fixtures/map';

const validateResponseSchema = createResponseSchemaValidator({
    path: '/v2/maps/{sid}',
    method: 'put'
});

export const main: EndpointSuiteDefinition = (context) => {
    describe('PUT /v2/maps/{sid}', () => {
        let newMap: any;

        async function updateMap(): Promise<got.Response<any>> {
            return context.server.request('/v2/maps/sid-1', {
                method: 'PUT',
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                    'X-Ya-User-Ticket': UID1_USER_TICKET
                },
                json: true,
                body: newMap
            });
        }

        beforeEach(() => {
            newMap = {
                revision: '0',
                properties: {
                    ...mapFixture.properties,
                    name: 'new name',
                    description: 'new description'
                },
                options: mapFixture.options,
                state: {
                    ...mapFixture.state,
                    zoom: mapFixture.state.zoom + 1
                }
            };
        });

        describe('authentication', () => {
            it('should return 401 for request without tickets', async () => {
                const res = await context.server.request('/v2/maps/sid-1', {
                    method: 'PUT'
                });
                expect(res.statusCode).to.equal(401);
            });

            it('should return 401 for request without service ticket', async () => {
                const res = await context.server.request('/v2/maps/sid-1', {
                    method: 'PUT',
                    headers: {
                        'X-Ya-User-Ticket': UID1_USER_TICKET
                    }
                });
                expect(res.statusCode).to.equal(401);
            });

            it('should return 401 for request without user ticket', async () => {
                const res = await context.server.request('/v2/maps/sid-1', {
                    method: 'PUT',
                    headers: {
                        'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                    }
                });
                expect(res.statusCode).to.equal(401);
            });

            it('should return 403 for unknown service ticket', async () => {
                const res = await context.server.request('/v2/maps/sid-1', {
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
            const res = await updateMap();
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

            const res = await updateMap();
            expect(res.statusCode).to.equal(404);
            validateResponseSchema(res);
            expect(res.body.type).to.equal('MapNotFoundError');
        });

        it('should return 403 if user role is "spectator"', async () => {
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
                            role: 'spectator'
                        }
                    ]
                }
            ]);

            const res = await updateMap();
            expect(res.statusCode).to.equal(403);
            validateResponseSchema(res);
            expect(res.body.type).to.equal('EditMapForbiddenError');
        });

        it('should return 409 if current map revision is different', async () => {
            await context.db.loadFixtures([
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

            const res = await updateMap();
            expect(res.statusCode).to.equal(409);
        });

        describe('updating', () => {
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

            it('should return updated map in response', async () => {
                const res = await updateMap();
                expect(res.statusCode).to.equal(200);
                expect(res.headers['content-type']).to.match(/json/);

                validateResponseSchema(res);
                const body = res.body;
                expect(body.sid).to.equal('sid-1');
                expect(body.revision).to.equal('1');
                expect(Date.parse(body.properties.timeUpdated)).to.be.above(
                    Date.parse(body.properties.timeCreated)
                );
                expect(body.properties.name).to.equal(newMap.properties.name);
                expect(body.properties.description).to.equal(newMap.properties.description);
                expect(body.options).to.deep.equal(newMap.options);
                expect(body.state).to.deep.equal(newMap.state);
            });

            it('should update map with given sid in database', async () => {
                const res = await updateMap();
                expect(res.statusCode).to.equal(200);

                const result = await context.db.query('SELECT * FROM maps WHERE sid = $1', ['sid-1']);
                const row = result.rows[0];
                expect(row.revision).to.equal('1');
                expect(row.time_updated.getTime()).to.be.above(row.time_created.getTime());
                expect(row.properties).to.deep.equal(newMap.properties);
                expect(row.options).to.deep.equal(newMap.options);
                expect(row.state).to.deep.equal(newMap.state);
            });

            it('should not update map with another sid in database', async () => {
                const selectMap = () => context.db.query('SELECT * FROM maps WHERE sid = $1', ['sid-2']);

                const oldResult = await selectMap();
                const res = await updateMap();
                expect(res.statusCode).to.equal(200);
                const newResult = await selectMap();
                expect(newResult.rows).to.deep.equal(oldResult.rows);
            });
        });
    });
};

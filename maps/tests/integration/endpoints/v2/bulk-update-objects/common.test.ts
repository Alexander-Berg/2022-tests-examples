import {expect} from 'chai';
import {EndpointSuiteDefinition} from 'tests/integration/endpoints/suite';
import {
    SELF_SERVICE_TICKET,
    UNKNOWN_SERVICE_TICKET,
    UID1_USER_TICKET
} from 'tests/integration/endpoints/tvm-tickets';
import {objectFixture} from 'tests/integration/endpoints/fixtures/object';
import {createMap, batchUpdate, validateResponseSchema} from './utils';

export const main: EndpointSuiteDefinition = (context) => {
    describe('PATCH /v2/maps/{sid}/objects', () => {
        const TEST_OPERATION = [
            {type: 'delete', id: '1'}
        ];

        describe('authorization', () => {
            it('should return 401 for request without tickets', async () => {
                const res = await context.server.request('/v2/maps/sid-1/objects', {
                    method: 'PATCH'
                });
                expect(res.statusCode).to.equal(401);
            });

            it('should return 401 for request without service ticket', async () => {
                const res = await context.server.request('/v2/maps/sid-1/objects', {
                    method: 'PATCH',
                    headers: {
                        'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                    }
                });
                expect(res.statusCode).to.equal(401);
            });

            it('should return 401 for request without user ticket', async () => {
                const res = await context.server.request('/v2/maps/sid-1/objects', {
                    method: 'PATCH',
                    headers: {
                        'X-Ya-User-Ticket': UID1_USER_TICKET
                    }
                });
                expect(res.statusCode).to.equal(401);
            });

            it('should return 403 for unknown service ticket', async () => {
                const res = await context.server.request('/v2/maps/sid-1/objects', {
                    method: 'PATCH',
                    headers: {
                        'X-Ya-Service-Ticket': UNKNOWN_SERVICE_TICKET,
                        'X-Ya-User-Ticket': UID1_USER_TICKET
                    }
                });
                expect(res.statusCode).to.equal(403);
            });
        });

        it('should return 404 if map does not exist', async () => {
            const res = await batchUpdate(context.server, TEST_OPERATION);
            expect(res.statusCode).to.equal(404);
            validateResponseSchema(res);
            expect(res.body.type).to.equal('MapNotFoundError');
        });

        it('should return 404 if map does not exist for user with given uid', async () => {
            await context.db.loadFixtures([
                {
                    table: 'maps',
                    rows: [
                        createMap({id: 1, sid: 'sid-1'})
                    ]
                },
                {
                    table: 'maps_users',
                    rows: [
                        {
                            map_id: 1,
                            uid: 20,
                            role: 'administrator'
                        }
                    ]
                }
            ]);

            const res = await batchUpdate(context.server, TEST_OPERATION);
            expect(res.statusCode).to.equal(404);
            validateResponseSchema(res);
            expect(res.body.type).to.equal('MapNotFoundError');
        });

        it('should return 404 if map is deleted', async () => {
            await context.db.loadFixtures([
                {
                    table: 'maps',
                    rows: [
                        createMap({id: 1, sid: 'sid-1', deleted: true})
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

            const res = await batchUpdate(context.server, TEST_OPERATION);
            expect(res.statusCode).to.equal(404);
            validateResponseSchema(res);
            expect(res.body.type).to.equal('MapNotFoundError');
        });

        it('should return 403 if user role is "spectator"', async () => {
            await context.db.loadFixtures([
                {
                    table: 'maps',
                    rows: [
                        createMap({id: 1, sid: 'sid-1'})
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

            const res = await batchUpdate(context.server, TEST_OPERATION);
            expect(res.statusCode).to.equal(403);
            validateResponseSchema(res);
            expect(res.body.type).to.equal('EditMapForbiddenError');
        });

        it('should return 409 if map revision is mismatched', async () => {
            await context.db.loadFixtures([
                {
                    table: 'maps',
                    rows: [
                        createMap({id: 1, sid: 'sid-1', revision: '10'})
                    ]
                },
                {
                    table: 'maps_users',
                    rows: [
                        {
                            map_id: 1,
                            uid: 1,
                            role: 'moderator'
                        }
                    ]
                }
            ]);

            const res = await batchUpdate(context.server, TEST_OPERATION);
            expect(res.statusCode).to.equal(409);
            validateResponseSchema(res);
            expect(res.body.type).to.equal('MapRevisionConflictError');
        });

        it('should return 400 if got conflicted operations', async () => {
            await context.db.loadFixtures([
                {
                    table: 'maps',
                    rows: [
                        createMap({id: 1, sid: 'sid-1'})
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

            const operations = [
                {
                    type: 'update_geometry',
                    id: '1',
                    geometry: {
                        type: 'Point',
                        coordinates: [1, 2]
                    }
                },
                {
                    type: 'create',
                    geometry: {
                        type: 'Point',
                        coordinates: [1, 2]
                    },
                    properties: {
                        name: 'name',
                        description: 'description'
                    },
                    options: {}
                },
                {
                    type: 'delete',
                    id: '1'
                }
            ];

            const res = await batchUpdate(context.server, operations);
            expect(res.statusCode).to.equal(400);
            validateResponseSchema(res);
            expect(res.body.type).to.equal('BulkOperationsConflictError');
        });

        describe('bulk update', () => {
            beforeEach(async () => {
                await context.db.loadFixtures([
                    {
                        table: 'maps',
                        rows: [
                            createMap({id: 1, sid: 'sid-1'})
                        ]
                    },
                    {
                        table: 'maps_users',
                        rows: [
                            {
                                map_id: 1,
                                uid: 1,
                                role: 'moderator'
                            }
                        ]
                    }
                ]);
            });

            it('should cancel all operations if any operation failed', async () => {
                const operations = [
                    {
                        type: 'create',
                        geometry: {
                            type: 'Point',
                            coordinates: [1, 2]
                        },
                        properties: {
                            name: 'name',
                            description: 'description'
                        },
                        options: {}
                    },
                    {
                        // This operation will fail.
                        type: 'delete',
                        id: '1'
                    }
                ];

                const res = await batchUpdate(context.server, operations);
                expect(res.statusCode).to.equal(400);
                validateResponseSchema(res);
                expect(res.body.type).to.equal('BulkObjectNotFoundError');
                expect(res.body.data.operationIndex).to.equal(1);

                const result = await context.db.query('SELECT * FROM objects');
                expect(result.rowCount).to.equal(0);
            });

            describe('when all operations completed', () => {
                beforeEach(async () => {
                    await context.db.loadFixtures([
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
                                    options: objectFixture.options,
                                    properties: objectFixture.properties,
                                    // This fields are needed only to satisfy constraints.
                                    bbox_max_quad_key: 1,
                                    bbox_min_quad_key: 1
                                }
                            ]
                        }
                    ]);
                });

                function makeOperations(): any {
                    return [
                        {
                            type: 'create',
                            geometry: {
                                type: 'LineString',
                                coordinates: [[1, 2], [3, 4]]
                            },
                            properties: {
                                name: 'name',
                                description: 'description'
                            },
                            options: {}
                        },
                        {
                            type: 'delete',
                            id: '1'
                        }
                    ];
                }

                it('should make all operations in database', async () => {
                    const res = await batchUpdate(context.server, makeOperations());
                    expect(res.statusCode).to.equal(200);

                    const result = await context.db.query('SELECT * FROM objects');
                    expect(result.rowCount).to.equal(1);
                    expect(result.rows[0].geometry.type).to.equal('LineString');
                });

                it('should update map revision and time of last modification', async () => {
                    const selectMap = () => context.db.query(
                        'SELECT * FROM maps WHERE sid = \'sid-1\''
                    );

                    const oldResult = await selectMap();

                    const res = await batchUpdate(context.server, makeOperations());
                    expect(res.statusCode).to.equal(200);

                    const newResult = await selectMap();
                    const oldMap = oldResult.rows[0];
                    const newMap = newResult.rows[0];
                    expect(newMap.revision).to.equal('2');
                    expect(newMap.time_updated.getTime())
                        .to.be.above(oldMap.time_updated.getTime());
                    expect(newMap.time_created.getTime())
                        .to.equal(oldMap.time_created.getTime());
                });

                it('should return results of operations in response', async () => {
                    const operations = makeOperations();
                    const res = await batchUpdate(context.server, operations);
                    expect(res.statusCode).to.equal(200);
                    expect(res.headers['content-type']).to.match(/application\/json/);

                    validateResponseSchema(res);
                    const body = res.body;

                    const result = await context.db.query(
                        `SELECT id FROM objects
                        WHERE geometry->>'type' = 'LineString'`
                    );

                    expect(body.revision).to.equal('2');
                    expect(body.results).to.have.length(operations.length);
                    expect(body.results[0]).to.deep.equal({id: result.rows[0].id});
                    expect(body.results[1]).to.equal(null);
                });
            });

            it('should update metadata and geometry of the same object (issue #120)', async () => {
                await context.db.loadFixtures([
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
                                options: objectFixture.options,
                                properties: objectFixture.properties,
                                // This fields are needed only to satisfy constraints.
                                bbox_max_quad_key: 1,
                                bbox_min_quad_key: 1
                            }
                        ]
                    }
                ]);

                const operations = [
                    {
                        type: 'update_geometry',
                        id: '1',
                        geometry: {
                            type: 'Point',
                            coordinates: [3, 4]
                        }
                    },
                    {
                        type: 'update_metadata',
                        id: '1',
                        options: objectFixture.options,
                        properties: objectFixture.properties
                    }
                ];

                const res = await batchUpdate(context.server, operations);
                expect(res.statusCode).to.equal(200);
            });
        });
    });
};

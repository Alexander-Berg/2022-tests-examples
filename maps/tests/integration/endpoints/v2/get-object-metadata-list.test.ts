import * as got from 'got';
import * as jspath from 'jspath';
import {expect} from 'chai';
import {Geometry} from 'src/v2/geometry';
import {EndpointSuiteDefinition, EndpointSuiteContext} from 'tests/integration/endpoints/suite';
import {createResponseSchemaValidator, ResponseSchemaValidator} from 'tests/integration/response-validator';
import {
    UNKNOWN_SERVICE_TICKET,
    SELF_SERVICE_TICKET,
    UID1_USER_TICKET
} from 'tests/integration/endpoints/tvm-tickets';
import {mapFixture} from 'tests/integration/endpoints/fixtures/map';
import {objectFixture} from 'tests/integration/endpoints/fixtures/object';

interface CreateMapOptions {
    id: number;
    sid: string;
    revision?: string;
    deleted?: boolean;
    access?: 'private' | 'public' | 'link';
}

function createMap({
    id,
    sid,
    revision = '1',
    deleted = false,
    access = 'private'
}: CreateMapOptions) {
    return {
        id,
        sid,
        time_created: '2016-07-14 01:00:00',
        time_updated: '2016-07-14 01:00:00',
        revision,
        deleted,
        properties: {
            ...mapFixture.properties,
            access
        },
        options: mapFixture.options,
        state: mapFixture.state
    };
}

type CreateMap = typeof createMap;

interface CreateObjectOptions {
    id: number | string;
    map_id: number | string;
    geometry: Geometry;
    options: object;
    properties: object;
}

function createObject({
    id,
    map_id,
    geometry,
    options,
    properties
}: CreateObjectOptions) {
    return {
        id,
        map_id,
        geometry,
        options,
        properties,
        // This fields are needed only to satisfy constraints.
        bbox_max_quad_key: 1,
        bbox_min_quad_key: 1
    };
}

interface GetObjectMetadataListOptions {
    offset?: number;
    limit?: number;
}

type GetObjectMetadataList = (options?: GetObjectMetadataListOptions) => Promise<got.Response<any>>;

export const main: EndpointSuiteDefinition = (context) => {
    describe('GET /v2/maps/{sid}/objects_metadata', () => {
        const validateResponseSchema = createResponseSchemaValidator({
            path: '/v2/maps/{sid}/objects_metadata'
        });

        const getObjectMetadataList: GetObjectMetadataList = (options) => {
            return context.server.request('/v2/maps/sid-1/objects_metadata', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                    'X-Ya-User-Ticket': UID1_USER_TICKET
                },
                query: {
                    uid: 1,
                    offset: 0,
                    limit: 10,
                    ...options
                },
                json: true
            });
        };

        describe('authentication', () => {
            it('should return 401 for request without tickets', async () => {
                const res = await context.server.request('/v2/maps/sid-1/objects_metadata');
                expect(res.statusCode).to.equal(401);
            });

            it('should return 401 for request without service ticket', async () => {
                const res = await context.server.request('/v2/maps/sid-1/objects_metadata', {
                    headers: {
                        'X-Ya-User-Ticket': UID1_USER_TICKET
                    }
                });
                expect(res.statusCode).to.equal(401);
            });

            it('should return 401 for request without user ticket', async () => {
                const res = await context.server.request('/v2/maps/sid-1/objects_metadata', {
                    headers: {
                        'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                    }
                });
                expect(res.statusCode).to.equal(401);
            });

            it('should return 403 for unknown service ticket', async () => {
                const res = await context.server.request('/v2/maps/sid-1/objects_metadata', {
                    headers: {
                        'X-Ya-Service-Ticket': UNKNOWN_SERVICE_TICKET,
                        'X-Ya-User-Ticket': UID1_USER_TICKET
                    }
                });
                expect(res.statusCode).to.equal(403);
            });
        });

        commonTests(context, createMap, getObjectMetadataList, validateResponseSchema);
    });

    describe('GET /v2/public_maps/{sid}/objects_metadata', () => {
        const validateResponseSchema = createResponseSchemaValidator({
            path: '/v2/public_maps/{sid}/objects_metadata'
        });

        const getObjectMetadataList: GetObjectMetadataList = (options) => {
            return context.server.request('/v2/public_maps/sid-1/objects_metadata', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                },
                query: {
                    offset: 0,
                    limit: 10,
                    ...options
                },
                json: true
            });
        };

        const createPublicMap: CreateMap = (options) => {
            options.access = 'public';
            return createMap(options);
        };

        describe('authentication', () => {
            it('should return 401 for request without tickets', async () => {
                const res = await context.server.request('/v2/public_maps/sid-1/objects_metadata');
                expect(res.statusCode).to.equal(401);
            });

            it('should return 403 for unknown service ticket', async () => {
                const res = await context.server.request('/v2/public_maps/sid-1/objects_metadata', {
                    headers: {
                        'X-Ya-Service-Ticket': UNKNOWN_SERVICE_TICKET
                    }
                });
                expect(res.statusCode).to.equal(403);
            });
        });

        commonTests(context, createPublicMap, getObjectMetadataList, validateResponseSchema);

        it('should return 404 if map is private', async () => {
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

            const res = await getObjectMetadataList();
            expect(res.statusCode).to.equal(404);
            validateResponseSchema(res);
            expect(res.body.type).to.equal('MapNotFoundError');
        });
    });
};

function commonTests(
    context: EndpointSuiteContext,
    createMap: CreateMap,
    getObjectMetadataList: GetObjectMetadataList,
    validateResponseSchema: ResponseSchemaValidator
): void {
    it('should return 404 if user have not maps', async () => {
        const res = await getObjectMetadataList();
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

        const res = await getObjectMetadataList();
        expect(res.statusCode).to.equal(404);
        validateResponseSchema(res);
        expect(res.body.type).to.equal('MapNotFoundError');
    });

    describe('when map exist', () => {
        beforeEach(async () => {
            await context.db.loadFixtures([
                {
                    table: 'maps',
                    rows: [
                        createMap({id: 1, sid: 'sid-1'}),
                        createMap({id: 2, sid: 'sid-2'})
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

        it('should return empty list for map without objects', async () => {
            const res = await getObjectMetadataList();
            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);
            expect(res.body.totalCount).to.equal(0, 'Invalid total count of items');
            expect(res.body.items).to.deep.equal([], 'Invalid items in response');
        });

        it('should return list of objects metadata of the given map', async () => {
            await context.db.loadFixtures([
                {
                    table: 'objects',
                    rows: [
                        createObject({
                            id: 1,
                            map_id: 1,
                            geometry: {
                                type: 'Point',
                                coordinates: [1, 2]
                            },
                            properties: {
                                name: 'Point name',
                                description: 'Point description'
                            },
                            options: {
                                iconContent: '1'
                            }
                        }),
                        createObject({
                            id: 2,
                            map_id: 1,
                            geometry: {
                                type: 'LineString',
                                coordinates: [[1, 2], [3, 4]]
                            },
                            properties: {
                                name: 'Line name',
                                description: 'Line description'
                            },
                            options: {
                                strokeColor: 'ffffff'
                            }
                        })
                    ]
                }
            ]);

            const res = await getObjectMetadataList();
            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);

            const expectedMetadata = [
                {
                    id: '1',
                    properties: {
                        name: 'Point name',
                        description: 'Point description'
                    },
                    options: {
                        iconContent: '1'
                    }
                },
                {
                    id: '2',
                    properties: {
                        name: 'Line name',
                        description: 'Line description'
                    },
                    options: {
                        strokeColor: 'ffffff'
                    }
                }
            ];

            expect(res.body.totalCount)
                .to.equal(expectedMetadata.length, 'Invalid total count of items');
            expect(res.body.items)
                .to.have.deep.members(expectedMetadata, 'Invalid items in response');
        });

        it('should sort objects in descending order by options.order and id', async () => {
            await context.db.loadFixtures([
                {
                    table: 'objects',
                    // Insert objects in random order.
                    rows: [
                        // Use 20 for assert non-string comparison ('3' > '20').
                        {id: 1, order: 20},
                        {id: 2, order: 1},
                        {id: 6, order: 1},
                        {id: 4, order: 3},
                        {id: 5, order: 1},
                        {id: 3, order: 1}
                    ].map(({id, order}) => {
                        return createObject({
                            id: id,
                            map_id: 1,
                            geometry: {
                                type: 'Point',
                                coordinates: [1, 2]
                            },
                            options: {...objectFixture.options, order},
                            properties: objectFixture.properties
                        });
                    })
                }
            ]);

            const res = await getObjectMetadataList();
            expect(res.statusCode).to.equal(200);

            validateResponseSchema(res);
            expect(res.body.totalCount)
                .to.equal(6, 'Invalid total count of items');
            expect(jspath('.items.id', res.body))
                .to.deep.equal(['1', '4', '6', '5', '3', '2'], 'Invalid items order');
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

            beforeEach(async () => {
                await context.db.loadFixtures([
                    {
                        table: 'objects',
                        rows: range(1, TOTAL_COUNT).map((id) => {
                            return createObject({
                                id: id,
                                map_id: 1,
                                geometry: {
                                    type: 'Point',
                                    coordinates: [1, 2]
                                },
                                options: objectFixture.options,
                                properties: objectFixture.properties
                            });
                        })
                    }
                ]);
            });

            it('should apply "offset" and "limit" query parameters', async () => {
                const res = await getObjectMetadataList({offset: 1, limit: 2});
                expect(res.statusCode).to.equal(200);
                validateResponseSchema(res);
                expect(res.body.totalCount).to.equal(TOTAL_COUNT, 'Invalid total count of items');
                expect(res.body.items).to.have.length(2, 'Invalid count of items');
            });

            it('should return correct total count if offset is greater than or equal to total count', async () => {
                const res = await getObjectMetadataList({offset: TOTAL_COUNT, limit: 2});
                expect(res.statusCode).to.equal(200);
                validateResponseSchema(res);
                expect(res.body.totalCount).to.equal(TOTAL_COUNT, 'Invalid total count of items');
                expect(res.body.items).to.have.length(0, 'Invalid count of items');
            });

            it('should not compare total count and offset as strings (issue #152)', async () => {
                const offset = 4;
                expect(String(offset) > String(TOTAL_COUNT)).to.be.true;
                expect(offset < TOTAL_COUNT).to.be.true;

                const res = await getObjectMetadataList({offset: offset, limit: 3});
                expect(res.statusCode).to.equal(200);
                validateResponseSchema(res);
                expect(res.body.totalCount).to.equal(TOTAL_COUNT, 'Invalid total count of items');
                expect(res.body.items).to.have.length(3, 'Invalid count of items');
            });
        });
    });
}

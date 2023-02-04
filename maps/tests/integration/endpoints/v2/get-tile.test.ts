import * as assert from 'assert';
import * as util from 'util';
import * as got from 'got';
import * as jspath from 'jspath';
import {expect, AssertionError} from 'chai';
import 'tests/chai-extensions';
import {pixelBoundsFromTileBounds, fromPixelBounds} from 'src/v2/bounds';
import {TILE_SIZE} from 'src/v2/geo-constants';
import {EndpointSuiteDefinition, EndpointSuiteContext} from 'tests/integration/endpoints/suite';
import {createResponseSchemaValidator, ResponseSchemaValidator} from 'tests/integration/response-validator';
import {
    UNKNOWN_SERVICE_TICKET,
    SELF_SERVICE_TICKET,
    UID1_USER_TICKET,
    UID2_USER_TICKET,
    UID999_USER_TICKET
} from 'tests/integration/endpoints/tvm-tickets';
import {mapFixture} from 'tests/integration/endpoints/fixtures/map';
import {objectFixture} from 'tests/integration/endpoints/fixtures/object';
import {minQuadKey, maxQuadKey, TILE_GRID_ZOOM} from 'tests/integration/endpoints/fixtures/tile-grid';

export const main: EndpointSuiteDefinition = (context) => {
    describe('GET /v2/maps/{sid}/tiles', () => {
        const validateResponseSchema = createResponseSchemaValidator({
            path: '/v2/maps/{sid}/tiles'
        });

        beforeEach(async () => {
            await context.db.loadFixtures([
                {
                    table: 'maps',
                    rows: [
                        {
                            id: 1,
                            sid: 'sid-1',
                            deleted: true,
                            revision: '1',
                            time_created: '2016-07-14 01:00:00',
                            time_updated: '2016-07-14 01:00:00',
                            properties: mapFixture.properties,
                            options: mapFixture.options,
                            state: mapFixture.state
                        },
                        {
                            id: 2,
                            sid: 'sid-2',
                            deleted: false,
                            revision: '1',
                            time_created: '2016-07-14 01:00:00',
                            time_updated: '2016-07-14 01:00:00',
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
                const res = await context.server.request('/v2/maps/sid-1/tiles');
                expect(res.statusCode).to.equal(401);
            });

            it('should return 401 for request without service ticket', async () => {
                const res = await context.server.request('/v2/maps/sid-1/tiles', {
                    headers: {
                        'X-Ya-User-Ticket': UID1_USER_TICKET
                    }
                });
                expect(res.statusCode).to.equal(401);
            });

            it('should return 401 for request without user ticket', async () => {
                const res = await context.server.request('/v2/maps/sid-1/tiles', {
                    headers: {
                        'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                    }
                });
                expect(res.statusCode).to.equal(401);
            });

            it('should return 403 for unknown service ticket', async () => {
                const res = await context.server.request('/v2/maps/sid-1/tiles', {
                    headers: {
                        'X-Ya-Service-Ticket': UNKNOWN_SERVICE_TICKET,
                        'X-Ya-User-Ticket': UID1_USER_TICKET
                    }
                });
                expect(res.statusCode).to.equal(403);
            });
        });

        it('should return 400 if `tile_size` query param is not a power of two', async () => {
            const res = await context.server.request('/v2/maps/sid-2/tiles', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                    'X-Ya-User-Ticket': UID1_USER_TICKET
                },
                query: {
                    uid: 1,
                    revision: 1,
                    x: 1,
                    y: 1,
                    z: 1,
                    tile_size: 400,
                    clusterize: 'none'
                },
                json: true
            });
            expect(res.statusCode).to.equal(400);
        });

        it('should return 400 if value of the `x` query param out of range', async () => {
            const res = await context.server.request('/v2/maps/sid-2/tiles', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                    'X-Ya-User-Ticket': UID1_USER_TICKET
                },
                query: {
                    revision: 1,
                    x: 1,
                    y: 0,
                    z: 0,
                    tile_size: 256,
                    clusterize: 'none'
                },
                json: true
            });
            expect(res.statusCode).to.equal(400);
        });

        it('should consider `tile_size` value when check max `x` value', async () => {
            const res = await context.server.request('/v2/maps/sid-2/tiles', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                    'X-Ya-User-Ticket': UID1_USER_TICKET
                },
                query: {
                    revision: 1,
                    x: 1,
                    y: 0,
                    z: 1,
                    tile_size: 512,
                    clusterize: 'none'
                },
                json: true
            });
            expect(res.statusCode).to.equal(400);
        });

        it('should return 400 if value of the `y` query param out of range', async () => {
            const res = await context.server.request('/v2/maps/sid-2/tiles', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                    'X-Ya-User-Ticket': UID1_USER_TICKET
                },
                query: {
                    revision: 1,
                    x: 0,
                    y: 1,
                    z: 0,
                    tile_size: 256,
                    clusterize: 'none'
                },
                json: true
            });
            expect(res.statusCode).to.equal(400);
        });

        it('should consider `tile_size` value when check max `y` value', async () => {
            const res = await context.server.request('/v2/maps/sid-2/tiles', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                    'X-Ya-User-Ticket': UID1_USER_TICKET
                },
                query: {
                    revision: 1,
                    x: 0,
                    y: 1,
                    z: 1,
                    tile_size: 512,
                    clusterize: 'none'
                },
                json: true
            });
            expect(res.statusCode).to.equal(400);
        });

        it('should return 404 if user does not exist', async () => {
            const res = await context.server.request('/v2/maps/sid-1/tiles', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                    'X-Ya-User-Ticket': UID999_USER_TICKET
                },
                query: {
                    revision: 1,
                    x: 1,
                    y: 1,
                    z: 1,
                    tile_size: 256,
                    clusterize: 'none'
                },
                json: true
            });
            expect(res.statusCode).to.equal(404);
            validateResponseSchema(res);
            expect(res.body.type).to.equal('MapNotFoundError');
        });

        it('should return 404 if user has not map with the given sid', async () => {
            const res = await context.server.request('/v2/maps/sid-1/tiles', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                    'X-Ya-User-Ticket': UID2_USER_TICKET
                },
                query: {
                    revision: 1,
                    x: 1,
                    y: 1,
                    z: 1,
                    tile_size: 256,
                    clusterize: 'none'
                },
                json: true
            });
            expect(res.statusCode).to.equal(404);
            validateResponseSchema(res);
            expect(res.body.type).to.equal('MapNotFoundError');
        });

        it('should return 404 if map does not exist', async () => {
            const res = await context.server.request('/v2/maps/sid-123/tiles', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                    'X-Ya-User-Ticket': UID1_USER_TICKET
                },
                query: {
                    revision: 1,
                    x: 1,
                    y: 1,
                    z: 1,
                    tile_size: 256,
                    clusterize: 'none'
                },
                json: true
            });
            expect(res.statusCode).to.equal(404);
            validateResponseSchema(res);
            expect(res.body.type).to.equal('MapNotFoundError');
        });

        it('should return 404 if map is deleted', async () => {
            const res = await context.server.request('/v2/maps/sid-1/tiles', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                    'X-Ya-User-Ticket': UID1_USER_TICKET
                },
                query: {
                    revision: 1,
                    x: 1,
                    y: 1,
                    z: 1,
                    tile_size: 256,
                    clusterize: 'none'
                },
                json: true
            });
            expect(res.statusCode).to.equal(404);
            validateResponseSchema(res);
            expect(res.body.type).to.equal('MapNotFoundError');
        });

        it('should return 409 if try to get map with invalid revision', async () => {
            const res = await context.server.request('/v2/maps/sid-2/tiles', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                    'X-Ya-User-Ticket': UID1_USER_TICKET
                },
                query: {
                    revision: 0,
                    x: 1,
                    y: 1,
                    z: 1,
                    tile_size: 256,
                    clusterize: 'none'
                },
                json: true
            });
            expect(res.statusCode).to.equal(409);
            validateResponseSchema(res);
            expect(res.body.type).to.equal('MapRevisionConflictError');
        });

        describe('tile selection', () => {
            const getTiles = (options: any) => {
                return context.server.request('/v2/maps/sid-2/tiles', {
                    headers: {
                        'X-Ya-Service-Ticket': SELF_SERVICE_TICKET,
                        'X-Ya-User-Ticket': UID1_USER_TICKET
                    },
                    query: {
                        ...options,
                        revision: 1
                    },
                    json: true
                });
            };

            testTileSelection(
                context,
                2,
                getTiles,
                validateResponseSchema
            );
        });
    });

    describe('GET /v2/public_maps/{sid}/tiles', () => {
        const validateResponseSchema = createResponseSchemaValidator({
            path: '/v2/public_maps/{sid}/tiles'
        });

        beforeEach(async () => {
            await context.db.loadFixtures([
                {
                    table: 'maps',
                    rows: [
                        {
                            id: 1,
                            sid: 'sid-1',
                            deleted: false,
                            revision: '1',
                            time_created: '2016-07-14 01:00:00',
                            time_updated: '2016-07-14 01:00:00',
                            properties: mapFixture.properties,
                            options: mapFixture.options,
                            state: mapFixture.state
                        },
                        {
                            id: 2,
                            sid: 'sid-2',
                            deleted: true,
                            revision: '1',
                            time_created: '2016-07-14 01:00:00',
                            time_updated: '2016-07-14 01:00:00',
                            properties: {
                                ...mapFixture.properties,
                                access: 'public'
                            },
                            options: mapFixture.options,
                            state: mapFixture.state
                        },
                        {
                            id: 3,
                            sid: 'sid-3',
                            deleted: false,
                            revision: '1',
                            time_created: '2016-07-14 01:00:00',
                            time_updated: '2016-07-14 01:00:00',
                            properties: {
                                ...mapFixture.properties,
                                access: 'public'
                            },
                            options: mapFixture.options,
                            state: mapFixture.state
                        }
                    ]
                }
            ]);
        });

        describe('authentication', () => {
            it('should return 401 for request without tickets', async () => {
                const res = await context.server.request('/v2/public_maps/sid-3/tiles');
                expect(res.statusCode).to.equal(401);
            });

            it('should return 403 for unknown service ticket', async () => {
                const res = await context.server.request('/v2/public_maps/sid-3/tiles', {
                    headers: {
                        'X-Ya-Service-Ticket': UNKNOWN_SERVICE_TICKET
                    }
                });
                expect(res.statusCode).to.equal(403);
            });
        });

        it('should return 400 if `tile_size` query param is not a power of two', async () => {
            const res = await context.server.request('/v2/public_maps/sid-3/tiles', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                },
                query: {
                    revision: 1,
                    x: 1,
                    y: 1,
                    z: 1,
                    tile_size: 400,
                    clusterize: 'none'
                },
                json: true
            });
            expect(res.statusCode).to.equal(400);
        });

        it('should return 400 if value of the `x` query param out of range', async () => {
            const res = await context.server.request('/v2/public_maps/sid-3/tiles', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                },
                query: {
                    revision: 1,
                    x: 1,
                    y: 0,
                    z: 0,
                    tile_size: 256,
                    clusterize: 'none'
                },
                json: true
            });
            expect(res.statusCode).to.equal(400);
        });

        it('should consider `tile_size` value when check max `x` value', async () => {
            const res = await context.server.request('/v2/public_maps/sid-3/tiles', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                },
                query: {
                    revision: 1,
                    x: 1,
                    y: 0,
                    z: 1,
                    tile_size: 512,
                    clusterize: 'none'
                },
                json: true
            });
            expect(res.statusCode).to.equal(400);
        });

        it('should return 400 if value of the `y` query param out of range', async () => {
            const res = await context.server.request('/v2/public_maps/sid-3/tiles', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                },
                query: {
                    revision: 1,
                    x: 0,
                    y: 1,
                    z: 0,
                    tile_size: 256,
                    clusterize: 'none'
                },
                json: true
            });
            expect(res.statusCode).to.equal(400);
        });

        it('should consider `tile_size` value when check max `y` value', async () => {
            const res = await context.server.request('/v2/public_maps/sid-3/tiles', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                },
                query: {
                    revision: 1,
                    x: 0,
                    y: 1,
                    z: 1,
                    tile_size: 512,
                    clusterize: 'none'
                },
                json: true
            });
            expect(res.statusCode).to.equal(400);
        });

        it('should return 404 if map does not exist', async () => {
            const res = await context.server.request('/v2/public_maps/sid-123/tiles', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                },
                query: {
                    revision: 1,
                    x: 1,
                    y: 1,
                    z: 1,
                    tile_size: 256,
                    clusterize: 'none'
                },
                json: true
            });
            expect(res.statusCode).to.equal(404);
            validateResponseSchema(res);
            expect(res.body.type).to.equal('MapNotFoundError');
        });

        it('should return 404 if map is private', async () => {
            const res = await context.server.request('/v2/public_maps/sid-1/tiles', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                },
                query: {
                    revision: 1,
                    x: 1,
                    y: 1,
                    z: 1,
                    tile_size: 256,
                    clusterize: 'none'
                },
                json: true
            });
            expect(res.statusCode).to.equal(404);
            validateResponseSchema(res);
            expect(res.body.type).to.equal('MapNotFoundError');
        });

        it('should return 404 for public deleted map', async () => {
            const res = await context.server.request('/v2/public_maps/sid-2/tiles', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                },
                query: {
                    revision: 1,
                    x: 1,
                    y: 1,
                    z: 1,
                    tile_size: 256,
                    clusterize: 'none'
                },
                json: true
            });
            expect(res.statusCode).to.equal(404);
            validateResponseSchema(res);
            expect(res.body.type).to.equal('MapNotFoundError');
        });

        it('should return 409 if try to get map with invalid revision', async () => {
            const res = await context.server.request('/v2/public_maps/sid-3/tiles', {
                headers: {
                    'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                },
                query: {
                    revision: 0,
                    x: 1,
                    y: 1,
                    z: 1,
                    tile_size: 256,
                    clusterize: 'none'
                },
                json: true
            });
            expect(res.statusCode).to.equal(409);
            validateResponseSchema(res);
            expect(res.body.type).to.equal('MapRevisionConflictError');
        });

        describe('tile selection', () => {
            const getTiles = (options: any) => {
                return context.server.request('/v2/public_maps/sid-3/tiles', {
                    headers: {
                        'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                    },
                    query: {
                        ...options,
                        revision: 1
                    },
                    json: true
                });
            };

            testTileSelection(
                context,
                3,
                getTiles,
                validateResponseSchema
            );
        });
    });
};

interface ActualCluster {
    id: string;
    number: number;
    bbox: number[][];
}

interface ExpectedCluster {
    /**
     * Bounding box in tile coordinates.
     */
    tileBbox: number[][];
    /**
     * Number of points in cluster.
     */
    number: number;
}

interface ExpectedTile {
    clusters?: ExpectedCluster[];
    /**
     * List of feature identifiers.
     */
    features?: string[];
}

class ResponseMap {
    private readonly _tiles: (ExpectedTile | undefined)[][] = [];

    constructor(private readonly _size: number) {}

    set(x: number, y: number, expectedResponse: ExpectedTile): this {
        assert(x < this._size, 'x out of range');
        assert(y < this._size, 'y out of range');
        if (!this._tiles[x]) {
            this._tiles[x] = [];
        }
        this._tiles[x][y] = expectedResponse;
        return this;
    }

    get(x: number, y: number): ExpectedTile {
        assert(x < this._size, 'x out of range');
        assert(y < this._size, 'y out of range');
        return this._tiles[x] && this._tiles[x][y] || {};
    }
}

/**
 * Declares common tests on tile selection for non-public and public endpoints.
 */
function testTileSelection(
    context: EndpointSuiteContext,
    mapId: number,
    getTiles: (options: any) => Promise<got.Response<any>>,
    validateResponseSchema: ResponseSchemaValidator
): void {
    interface CheckTileResponseOptions {
        z: number;
        tileSize: number;
        clusterize: 'none' | 'points';
        gridSize?: number;
        minObjectsPerCluster?: number;
        fillExpectedResults: (expectedResults: ResponseMap) => void;
    }

    async function checkTileResponses({
        z,
        tileSize,
        gridSize,
        clusterize,
        minObjectsPerCluster,
        fillExpectedResults
    }: CheckTileResponseOptions): Promise<void> {
        const size = 2 ** (z + Math.log2(TILE_SIZE) - Math.log2(tileSize));
        const expectedResults = new ResponseMap(size);
        fillExpectedResults(expectedResults);
        const tileRequests: Promise<void>[] = [];

        for (let x = 0; x < size; x++) {
            for (let y = 0; y < size; y++) {
                const options: any = {
                    x,
                    y,
                    z,
                    tile_size: tileSize,
                    clusterize
                };
                if (gridSize) {
                    options.grid_size = gridSize;
                }
                if (minObjectsPerCluster) {
                    options.min_objects_per_cluster = minObjectsPerCluster;
                }

                const promise = getTiles(options)
                    .then((res) => {
                        expect(res.statusCode).to.equal(200);
                        validateResponseSchema(res);

                        const expectedResult = expectedResults.get(x, y);

                        const actualFeatures = jspath('.features{.type === "Feature"}.id', res.body);
                        expect(actualFeatures).to.have.members(
                            expectedResult.features || [],
                            'Invalid features'
                        );

                        const clusters = jspath(
                            '.features{.type === "Cluster"}',
                            res.body
                        ) as ActualCluster[];

                        const clusterIds = clusters.map((cluster) => cluster.id);
                        expect(new Set(clusterIds).size).to.equal(
                            clusterIds.length,
                            `Duplicate cluster ids ${clusterIds}`
                        );

                        const actualClusters = clusters.map((cluster) => {
                            return {
                                bbox: cluster.bbox,
                                number: cluster.number
                            };
                        });

                        // Convert expected tile bbox to geo bbox.
                        const expectedClusters = (expectedResult.clusters || []).map((cluster) => {
                            const pixelBounds = pixelBoundsFromTileBounds(cluster.tileBbox);
                            return {
                                bbox: fromPixelBounds(pixelBounds, TILE_GRID_ZOOM),
                                number: cluster.number
                            };
                        });

                        // Check actual and expected clusters, ignoring order.
                        const unmatchedExpectedClusters = new Set(expectedClusters);
                        for (const actual of actualClusters) {
                            let found = false;
                            for (const expected of unmatchedExpectedClusters) {
                                try {
                                    expect(actual.number).to.equal(expected.number);
                                    expect(actual.bbox).to.roughlyEqualBounds(expected.bbox);
                                } catch (err) {
                                    if (err instanceof AssertionError) {
                                        continue;
                                    }
                                    throw err;
                                }
                                found = true;
                                unmatchedExpectedClusters.delete(expected);
                                break;
                            }
                            if (!found) {
                                throw new Error(
                                    'Actual cluster\n' +
                                    util.inspect(actual) + '\n' +
                                    'not found in expected clusters\n' +
                                    util.inspect(expectedClusters, {depth: null})
                                );
                            }
                        }
                    })
                    .catch((err) => {
                        throw new Error(`Failed to test tile x=${x}, y=${y}, z=${z}: ${err}`);
                    });

                tileRequests.push(promise);
            }
        }
        await Promise.all(tileRequests);
    }

    describe('without clustering', () => {
        beforeEach(async () => {
            await context.db.loadFixtures([
                {
                    // Note: geometry coordinates in objects doesn't matter here and need only for
                    // satisfaction db schema constraints.
                    table: 'objects',
                    //      0     1     2     3     4     5     6     7
                    //   +-----+-----+-----+-----+-----+-----+-----+-----+--- x
                    // 0 |     |     |     |  2  |     |     |     |     |
                    //   +-----+-----+-----+-----+-----+-----+-----+-----+
                    // 1 |     |  1  |     |     |     |     |     |     |
                    //   +-----+-----+-----+-----+-----+-----+-----+-----+
                    // 2 |     |     |     |     |     |     |     |     |
                    //   +-----+-----+-----+-----+-----+-----+-----+-----+
                    // 3 |     |  3  |  3  |     |     |     |     |     |
                    //   +-----+-----+-----+-----+-----+-----+-----+-----+
                    // 4 |     |  3  |  3  |     |     |     |     |     |
                    //   +-----+-----+-----+-----+-----+-----+-----+-----+
                    // 5 |     |  3  |  3  |     |     |     |     |     |
                    //   +-----+-----+-----+-----+-----+-----+-----+-----+
                    // 6 |     |  3  | 3,4 |     |     |     |     |     |
                    //   +-----+-----+-----+-----+-----+-----+-----+-----+
                    // 7 |     |  3  |  3  |     |     |     |     |     |
                    //   +-----+-----+-----+-----+-----+-----+-----+-----+
                    //   |
                    //   y
                    rows: [
                        {
                            id: 1,
                            map_id: mapId,
                            geometry: {
                                type: 'Point',
                                coordinates: [1, 2]
                            },
                            options: objectFixture.options,
                            properties: objectFixture.properties,
                            bbox_min_quad_key: minQuadKey(1, 1),
                            bbox_max_quad_key: maxQuadKey(1, 1)
                        },
                        {
                            id: 2,
                            map_id: mapId,
                            geometry: {
                                type: 'Point',
                                coordinates: [1, 2]
                            },
                            options: objectFixture.options,
                            properties: objectFixture.properties,
                            bbox_min_quad_key: minQuadKey(3, 0),
                            bbox_max_quad_key: maxQuadKey(3, 0)
                        },
                        {
                            id: 3,
                            map_id: mapId,
                            geometry: {
                                type: 'LineString',
                                coordinates: [[1, 2], [3, 4]]
                            },
                            options: objectFixture.options,
                            properties: objectFixture.properties,
                            bbox_min_quad_key: minQuadKey(1, 3),
                            bbox_max_quad_key: maxQuadKey(2, 7)
                        },
                        {
                            id: 4,
                            map_id: mapId,
                            geometry: {
                                type: 'Point',
                                coordinates: [1, 2]
                            },
                            options: objectFixture.options,
                            properties: objectFixture.properties,
                            bbox_min_quad_key: minQuadKey(2, 6),
                            bbox_max_quad_key: maxQuadKey(2, 6)
                        }
                    ]
                }
            ]);
        });

        describe('when tile_size=256', () => {
            it('should return all objects for tile on z=0', async () => {
                await checkTileResponses({
                    z: 0,
                    tileSize: 256,
                    clusterize: 'none',
                    fillExpectedResults: (res) => {
                        res.set(0, 0, {features: ['1', '2', '3', '4']});
                    }
                });
            });

            it('should return objects which intersect requested tile on z=1', async () => {
                await checkTileResponses({
                    z: 1,
                    tileSize: 256,
                    clusterize: 'none',
                    fillExpectedResults: (res) => {
                        res.set(0, 0, {features: ['1', '2', '3']});
                        res.set(0, 1, {features: ['3', '4']});
                    }
                });
            });

            it('should return objects which intersect requested tile on z=2', async () => {
                await checkTileResponses({
                    z: 2,
                    tileSize: 256,
                    clusterize: 'none',
                    fillExpectedResults: (res) => {
                        res.set(0, 0, {features: ['1']});
                        res.set(0, 1, {features: ['3']});
                        res.set(0, 2, {features: ['3']});
                        res.set(0, 3, {features: ['3']});

                        res.set(1, 0, {features: ['2']});
                        res.set(1, 1, {features: ['3']});
                        res.set(1, 2, {features: ['3']});
                        res.set(1, 3, {features: ['3', '4']});
                    }
                });
            });

            it('should return objects which intersect requested tile on z=3', async () => {
                await checkTileResponses({
                    z: 3,
                    tileSize: 256,
                    clusterize: 'none',
                    fillExpectedResults: (res) => {
                        res.set(1, 1, {features: ['1']});
                        res.set(1, 3, {features: ['3']});
                        res.set(1, 4, {features: ['3']});
                        res.set(1, 5, {features: ['3']});
                        res.set(1, 6, {features: ['3']});
                        res.set(1, 7, {features: ['3']});

                        res.set(2, 3, {features: ['3']});
                        res.set(2, 4, {features: ['3']});
                        res.set(2, 5, {features: ['3']});
                        res.set(2, 6, {features: ['3', '4']});
                        res.set(2, 7, {features: ['3']});

                        res.set(3, 0, {features: ['2']});
                    }
                });
            });
        });

        describe('when tile_size=512', () => {
            it('should return all objects for tile on z=0', async () => {
                await checkTileResponses({
                    z: 0,
                    tileSize: 512,
                    clusterize: 'none',
                    fillExpectedResults: (res) => {
                        res.set(0, 0, {features: ['1', '2', '3', '4']});
                    }
                });
            });

            it('should return all objects for tile on z=1', async () => {
                await checkTileResponses({
                    z: 1,
                    tileSize: 512,
                    clusterize: 'none',
                    fillExpectedResults: (res) => {
                        res.set(0, 0, {features: ['1', '2', '3', '4']});
                    }
                });
            });

            it('should return objects which intersect requested tile on z=2', async () => {
                await checkTileResponses({
                    z: 2,
                    tileSize: 512,
                    clusterize: 'none',
                    fillExpectedResults: (res) => {
                        res.set(0, 0, {features: ['1', '2', '3']});
                        res.set(0, 1, {features: ['3', '4']});
                    }
                });
            });

            it('should return objects which intersect requested tile on z=3', async () => {
                await checkTileResponses({
                    z: 3,
                    tileSize: 512,
                    clusterize: 'none',
                    fillExpectedResults: (res) => {
                        res.set(0, 0, {features: ['1']});
                        res.set(0, 1, {features: ['3']});
                        res.set(0, 2, {features: ['3']});
                        res.set(0, 3, {features: ['3']});

                        res.set(1, 0, {features: ['2']});
                        res.set(1, 1, {features: ['3']});
                        res.set(1, 2, {features: ['3']});
                        res.set(1, 3, {features: ['3', '4']});
                    }
                });
            });
        });
    });

    describe('point clustering', () => {
        function createPoint(id: number, x: number, y: number) {
            return {
                id,
                map_id: mapId,
                geometry: {
                    type: 'Point',
                    coordinates: [1, 2]
                },
                options: objectFixture.options,
                properties: objectFixture.properties,
                bbox_min_quad_key: minQuadKey(x, y),
                bbox_max_quad_key: maxQuadKey(x, y)
            };
        }

        beforeEach(async () => {
            await context.db.loadFixtures([
                {
                    table: 'objects',
                    //      0     1     2     3     4     5     6     7
                    //   +-----+-----+-----+-----+-----+-----+-----+-----+--- x
                    // 0 |  1  |  2  |     |     |     |     |     |     |
                    //   +-----+-----+-----+-----+-----+-----+-----+-----+
                    // 1 |  3  | 4,5 |     |     |     |     |     |     |
                    //   +-----+-----+-----+-----+-----+-----+-----+-----+
                    // 2 |     |     |     |  8  |     |     |     |     |
                    //   +-----+-----+-----+-----+-----+-----+-----+-----+
                    // 3 |     |     |     |  7  |     |     |     |     |
                    //   +-----+-----+-----+-----+-----+-----+-----+-----+
                    // 4 |     |  6  |     |     |     |     |     |     |
                    //   +-----+-----+-----+-----+-----+-----+-----+-----+
                    // 5 |     |     |     |     |     |     |     |     |
                    //   +-----+-----+-----+-----+-----+-----+-----+-----+
                    // 6 |     |     |     |     |     |     |     |     |
                    //   +-----+-----+-----+-----+-----+-----+-----+-----+
                    // 7 |     |     |     |     |     |     |     |     |
                    //   +-----+-----+-----+-----+-----+-----+-----+-----+
                    //   |
                    //   y
                    rows: [
                        createPoint(1, 0, 0),
                        createPoint(2, 1, 0),
                        createPoint(3, 0, 1),
                        createPoint(4, 1, 1),
                        createPoint(5, 1, 1),
                        createPoint(6, 1, 4),
                        createPoint(7, 3, 3),
                        createPoint(8, 3, 2)
                    ]
                }
            ]);
        });

        describe('when tile_size=256, grid_size=128', () => {
            it('should return all objects at z=0', async () => {
                await checkTileResponses({
                    z: 0,
                    tileSize: 256,
                    gridSize: 128,
                    clusterize: 'points',
                    minObjectsPerCluster: 2,
                    fillExpectedResults: (res) => {
                        res.set(0, 0, {
                            features: ['6'],
                            clusters: [
                                {

                                    tileBbox: [[0, 0], [3, 3]],
                                    number: 7
                                }
                            ]
                        });
                    }
                });
            });

            it('should return objects which intersect requested tile on z=1', async () => {
                await checkTileResponses({
                    z: 1,
                    tileSize: 256,
                    gridSize: 128,
                    clusterize: 'points',
                    minObjectsPerCluster: 2,
                    fillExpectedResults: (res) => {
                        res.set(0, 0, {
                            clusters: [
                                {

                                    tileBbox: [[0, 0], [1, 1]],
                                    number: 5
                                },
                                {
                                    tileBbox: [[3, 2], [3, 3]],
                                    number: 2
                                }
                            ]
                        });
                        res.set(0, 1, {features: ['6']});
                    }
                });
            });

            it('should return objects which intersect requested tile on z=2', async () => {
                await checkTileResponses({
                    z: 2,
                    tileSize: 256,
                    gridSize: 128,
                    clusterize: 'points',
                    minObjectsPerCluster: 2,
                    fillExpectedResults: (res) => {
                        res.set(0, 0, {
                            features: ['1', '2', '3'],
                            clusters: [
                                {

                                    tileBbox: [[1, 1], [1, 1]],
                                    number: 2
                                }
                            ]
                        });
                        res.set(0, 2, {features: ['6']});
                        res.set(1, 1, {features: ['7', '8']});
                    }
                });
            });
        });

        describe('when tile_size=512, grid_size=128', () => {
            it('should return all objects for tile on z=0', async () => {
                await checkTileResponses({
                    z: 0,
                    tileSize: 512,
                    gridSize: 128,
                    clusterize: 'points',
                    minObjectsPerCluster: 2,
                    fillExpectedResults: (res) => {
                        res.set(0, 0, {
                            features: ['6'],
                            clusters: [
                                {

                                    tileBbox: [[0, 0], [3, 3]],
                                    number: 7
                                }
                            ]
                        });
                    }
                });
            });

            it('should return all objects for tile on z=1', async () => {
                await checkTileResponses({
                    z: 1,
                    tileSize: 512,
                    gridSize: 128,
                    clusterize: 'points',
                    minObjectsPerCluster: 2,
                    fillExpectedResults: (res) => {
                        res.set(0, 0, {
                            features: ['6'],
                            clusters: [
                                {

                                    tileBbox: [[0, 0], [1, 1]],
                                    number: 5
                                },
                                {
                                    tileBbox: [[3, 2], [3, 3]],
                                    number: 2
                                }
                            ]
                        });
                    }
                });
            });

            it('should return objects which intersect requested tile on z=2', async () => {
                await checkTileResponses({
                    z: 2,
                    tileSize: 512,
                    gridSize: 128,
                    clusterize: 'points',
                    minObjectsPerCluster: 2,
                    fillExpectedResults: (res) => {
                        res.set(0, 0, {
                            features: ['1', '2', '3', '7', '8'],
                            clusters: [
                                {

                                    tileBbox: [[1, 1], [1, 1]],
                                    number: 2
                                }
                            ]
                        });
                        res.set(0, 1, {features: ['6']});
                    }
                });
            });
        });

        describe('when tile_size=256, grid_size=128, min_objects_per_cluster=3', () => {
            it('should return only one cluster at z=1', async () => {
                await checkTileResponses({
                    z: 1,
                    tileSize: 256,
                    gridSize: 128,
                    minObjectsPerCluster: 3,
                    clusterize: 'points',
                    fillExpectedResults: (res) => {
                        res.set(0, 0, {
                            features: ['7', '8'],
                            clusters: [
                                {

                                    tileBbox: [[0, 0], [1, 1]],
                                    number: 5
                                }
                            ]
                        });
                        res.set(0, 1, {
                            features: ['6']
                        });
                    }
                });
            });

            it('should not return clusters at z=2', async () => {
                await checkTileResponses({
                    z: 2,
                    tileSize: 256,
                    gridSize: 128,
                    minObjectsPerCluster: 3,
                    clusterize: 'points',
                    fillExpectedResults: (res) => {
                        res.set(0, 0, {
                            features: ['1', '2', '3', '4', '5']
                        });
                        res.set(0, 2, {
                            features: ['6']
                        });
                        res.set(1, 1, {
                            features: ['7', '8']
                        });
                    }
                });
            });
        });
    });
}

import * as Long from 'long';
import {expect} from 'chai';
import {Geometry} from 'src/v2/geometry';
import {X_MASK, Y_MASK} from 'src/v2/quad-key';
import {EndpointSuiteDefinition} from 'tests/integration/endpoints/suite';
import {objectFixture} from 'tests/integration/endpoints/fixtures/object';
import {createMap, batchUpdate, validateResponseSchema} from './utils';

function createObject({id, map_id, geometry}: {id: number, map_id: number, geometry: Geometry}) {
    return {
        id: id,
        map_id: map_id,
        geometry: geometry,
        options: objectFixture.options,
        properties: objectFixture.properties,
        // This fields are needed only to satisfy constraints.
        bbox_max_quad_key: 1,
        bbox_min_quad_key: 1
    };
}

export const main: EndpointSuiteDefinition = (context) => {
    describe('PATCH /v2/maps/{sid}/objects', () => {
        describe('operation "update_geometry"', () => {
            beforeEach(() => {
                return context.db.loadFixtures([
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
                    },
                    {
                        table: 'objects',
                        rows: [
                            createObject({
                                id: 1,
                                map_id: 1,
                                geometry: {
                                    type: 'Point',
                                    coordinates: [1, 2]
                                }
                            }),
                            createObject({
                                id: 2,
                                map_id: 1,
                                geometry: {
                                    type: 'LineString',
                                    coordinates: [[1, 2], [3, 4]]
                                }
                            })
                        ]
                    }
                ]);
            });

            it('should return 400 if object not found', async () => {
                const operations = [
                    {
                        type: 'update_geometry',
                        id: '123',
                        geometry: {
                            type: 'Point',
                            coordinates: [3, 4]
                        }
                    }
                ];

                const res = await batchUpdate(context.server, operations);
                expect(res.statusCode).to.equal(400);
                validateResponseSchema(res);
                expect(res.body.type).to.equal('BulkObjectNotFoundError');
            });

            it('should return 400 if try to change geometry type', async () => {
                const operations = [
                    {
                        type: 'update_geometry',
                        id: '1',
                        geometry: {
                            type: 'Polygon',
                            coordinates: [
                                [[1, 2], [3, 4], [5, 6], [1, 2]],
                                [[7, 8], [9, 10], [11, 12], [7, 8]]
                            ]
                        }
                    }
                ];

                const res = await batchUpdate(context.server, operations);
                expect(res.statusCode).to.equal(400);
                validateResponseSchema(res);
                expect(res.body.type).to.equal('GeometryTypeChangeError');
            });

            it('should update geometry of object in database by id', async () => {
                async function selectObjects(): Promise<any[]> {
                    const result = await context.db.query('SELECT * FROM objects ORDER BY id');
                    return result.rows;
                }

                const operations = [
                    {
                        type: 'update_geometry',
                        id: '2',
                        geometry: {
                            type: 'LineString',
                            coordinates: [[1, 2], [3, 4], [5, 6]]
                        }
                    }
                ];

                const oldObjects = await selectObjects();
                const res = await batchUpdate(context.server, operations);
                expect(res.statusCode).to.equal(200);

                validateResponseSchema(res);
                expect(res.body.results).to.deep.equal([null]);

                const newObjects = await selectObjects();
                // Check that only geometry of the object with id = 2 was updated.
                expect(newObjects.length).to.equal(oldObjects.length);
                expect(newObjects[0]).to.deep.equal(oldObjects[0]);
                expect(newObjects[1].properties).to.deep.equal(oldObjects[1].properties);
                expect(newObjects[1].options).to.deep.equal(oldObjects[1].options);
                expect(newObjects[1].geometry.type).to.equal(oldObjects[1].geometry.type);
                expect(newObjects[1].geometry.coordinates).to.deep.equal(operations[0].geometry.coordinates);

                // Check that bbox quad keys of the object with id = 2 was updated.
                expect(newObjects[1].bbox_min_quad_key).to.not.equal(oldObjects[1].bbox_min_quad_key);
                expect(newObjects[1].bbox_max_quad_key).to.not.equal(oldObjects[1].bbox_max_quad_key);

                const bboxMinQuadKey = Long.fromNumber(newObjects[1].bbox_min_quad_key);
                const bboxMaxQuadKey = Long.fromNumber(newObjects[1].bbox_max_quad_key);
                expect(bboxMinQuadKey.and(X_MASK).lte(bboxMaxQuadKey.and(X_MASK)))
                    .to.equal(true, 'bbox_min_quad_key must be less than or equal bbox_max_quad_key by X axis');
                expect(bboxMinQuadKey.and(Y_MASK).lte(bboxMaxQuadKey.and(Y_MASK)))
                    .to.equal(true, 'bbox_min_quad_key must be less than or equal bbox_max_quad_key by Y axis');
            });
        });
    });
};

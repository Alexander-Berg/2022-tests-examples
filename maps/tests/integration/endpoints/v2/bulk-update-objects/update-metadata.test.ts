import {expect} from 'chai';
import {EndpointSuiteDefinition} from 'tests/integration/endpoints/suite';
import {createMap, batchUpdate, validateResponseSchema} from './utils';

function createObject({id, map_id}: {id: number, map_id: number}) {
    return {
        id: id,
        map_id: map_id,
        geometry: {
            type: 'Point',
            coordinates: [1, 2]
        },
        options: {},
        properties: {
            name: 'default name',
            description: 'default description'
        },
        // This fields are needed only to satisfy constraints.
        bbox_max_quad_key: 1,
        bbox_min_quad_key: 1
    };
}

export const main: EndpointSuiteDefinition = (context) => {
    describe('PATCH /v2/maps/{sid}/objects', () => {
        describe('operation "update_metadata"', () => {
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
                            createObject({id: 1, map_id: 1}),
                            createObject({id: 2, map_id: 1})
                        ]
                    }
                ]);
            });

            function makeOperation(objectId: number) {
                return [
                    {
                        type: 'update_metadata',
                        id: String(objectId),
                        properties: {
                            name: 'new name',
                            description: 'new description'
                        },
                        options: {
                            fill: true
                        }
                    }
                ];
            }

            it('should return 400 if object not found', async () => {
                const res = await batchUpdate(context.server, makeOperation(123));
                expect(res.statusCode).to.equal(400);
                validateResponseSchema(res);
                expect(res.body.type).to.equal('BulkObjectNotFoundError');
            });

            it('should update object metadata in database by id', async () => {
                const selectObjects = () => context.db.query(
                    'SELECT * FROM objects ORDER BY id'
                );

                const oldResult = await selectObjects();
                const response = await batchUpdate(context.server, makeOperation(2));
                expect(response.statusCode).to.equal(200);
                validateResponseSchema(response);
                expect(response.body.results).to.deep.equal([null]);

                const newResult = await selectObjects();
                expect(newResult.rowCount).to.equal(oldResult.rowCount);

                const oldObjects = oldResult.rows;
                const newObjects = newResult.rows;
                expect(newObjects[0]).to.deep.equal(oldObjects[0]);

                expect(newObjects[1].properties).to.deep.equal({
                    name: 'new name',
                    description: 'new description'
                });
                expect(newObjects[1].options).to.deep.equal({
                    fill: true
                });
                expect(newObjects[1].geometry).to.deep.equal(oldObjects[1].geometry);
            });
        });
    });
};

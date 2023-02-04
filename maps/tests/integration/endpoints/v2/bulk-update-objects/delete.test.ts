import {expect} from 'chai';
import {EndpointSuiteDefinition} from 'tests/integration/endpoints/suite';
import {objectFixture} from 'tests/integration/endpoints/fixtures/object';
import {createMap, batchUpdate, validateResponseSchema} from './utils';

function createPoint({id, map_id}: {id: number, map_id: number}): any {
    return {
        id: id,
        map_id: map_id,
        geometry: {
            type: 'Point',
            coordinates: [1, 2]
        },
        options: objectFixture.options,
        properties: objectFixture.properties,
        // This fields are needed only to satisfy constraints.
        bbox_max_quad_key: 1,
        bbox_min_quad_key: 1
    };
}

export const main: EndpointSuiteDefinition = (context) => {
    describe('PATCH /v2/maps/{sid}/objects', () => {
        describe('operation "delete"', () => {
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
                            createPoint({id: 1, map_id: 1}),
                            createPoint({id: 2, map_id: 1})
                        ]
                    }
                ]);
            });

            it('should return 400 if object not found', async () => {
                const operations = [
                    {type: 'delete', id: '123'}
                ];
                const res = await batchUpdate(context.server, operations);
                expect(res.statusCode).to.equal(400);
                validateResponseSchema(res);
                expect(res.body.type).to.equal('BulkObjectNotFoundError');
            });

            it('should delete object in database by id', async () => {
                const operations = [
                    {type: 'delete', id: '1'}
                ];
                const res = await batchUpdate(context.server, operations);
                expect(res.statusCode).to.equal(200);
                validateResponseSchema(res);
                expect(res.body.results).to.deep.equal([null]);

                const result = await context.db.query('SELECT * FROM objects');
                expect(result.rowCount).to.equal(1);
                expect(result.rows[0].id).to.equal('2');
            });
        });
    });
};

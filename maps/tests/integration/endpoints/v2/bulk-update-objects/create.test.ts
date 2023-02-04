import * as got from 'got';
import * as Long from 'long';
import {expect} from 'chai';
import {Geometry} from 'src/v2/geometry';
import {X_MASK, Y_MASK} from 'src/v2/quad-key';
import {EndpointSuiteDefinition} from 'tests/integration/endpoints/suite';
import {objectFixture} from 'tests/integration/endpoints/fixtures/object';
import {createMap, batchUpdate, validateResponseSchema} from './utils';

interface NewObject {
    geometry: Geometry;
    options: object;
    properties: object;
}

function createObject(geometry: Geometry): NewObject {
    return {
        properties: objectFixture.properties,
        options: objectFixture.options,
        geometry
    };
}

export const main: EndpointSuiteDefinition = (context) => {
    describe('PATCH /v2/maps/{sid}/objects', () => {
        describe('operation "create"', () => {
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
                                role: 'administrator'
                            }
                        ]
                    }
                ]);
            });

            async function saveObject(newObject: NewObject): Promise<got.Response<any>> {
                const operations = [
                    {
                        type: 'create',
                        ...newObject
                    }
                ];
                return batchUpdate(context.server, operations);
            }

            function commonTests(obj: NewObject): void {
                it('should insert object', async () => {
                    const response = await saveObject(obj);
                    expect(response.statusCode).to.equal(200);
                    validateResponseSchema(response);

                    const result = await context.db.query('SELECT * FROM objects WHERE map_id = 1');

                    expect(result.rowCount).to.equal(1);

                    const row = result.rows[0];
                    expect(row.geometry).to.deep.equal(obj.geometry);
                    expect(row.properties).to.deep.equal(obj.properties);
                    expect(row.options).to.deep.equal(obj.options);

                    const bboxMinQuadKey = Long.fromNumber(row.bbox_min_quad_key);
                    const bboxMaxQuadKey = Long.fromNumber(row.bbox_max_quad_key);
                    expect(bboxMinQuadKey.and(X_MASK).lte(bboxMaxQuadKey.and(X_MASK)))
                        .to.equal(true, 'bbox_min_quad_key must be less than or equal bbox_max_quad_key by X axis');
                    expect(bboxMinQuadKey.and(Y_MASK).lte(bboxMaxQuadKey.and(Y_MASK)))
                        .to.equal(true, 'bbox_min_quad_key must be less than or equal bbox_max_quad_key by Y axis');
                });

                it('should return created object in response', async () => {
                    const response = await saveObject(obj);
                    expect(response.statusCode).to.equal(200);
                    validateResponseSchema(response);

                    const result = await context.db.query('SELECT id FROM objects');
                    const id = result.rows[0].id;
                    expect(response.body.results).to.have.length(1, 'Invalid result count in response');
                    expect(response.body.results[0].id).to.equal(id, 'Invalid object id in result');
                });
            }

            describe('Point', () => {
                const point = createObject({
                    type: 'Point',
                    coordinates: [37.62, 55.75]
                });

                commonTests(point);
            });

            describe('MultiPoint', () => {
                const multiPoint = createObject({
                    type: 'MultiPoint',
                    coordinates: [
                        [37, 55],
                        [40, 60]
                    ]
                });

                commonTests(multiPoint);
            });

            describe('LineString', () => {
                const lineString = createObject({
                    type: 'LineString',
                    coordinates: [
                        [10, 10],
                        [15, 11],
                        [20, 20]
                    ]
                });

                commonTests(lineString);
            });

            describe('Polygon without internal contour', () => {
                const polygon = createObject({
                    type: 'Polygon',
                    coordinates: [
                        [[-80, 60], [-90, 50], [-60, 40], [-80, 60]]
                    ]
                });

                commonTests(polygon);
            });

            describe('Polygon with internal contour', () => {
                const polygon = createObject({
                    type: 'Polygon',
                    coordinates: [
                        [[-80, 60], [-90, 50], [-60, 40], [-80, 60]],
                        [[-90, 80], [-90, 30], [-20, 40], [-90, 80]]
                    ]
                });

                commonTests(polygon);
            });

            describe('MultiPolygon', () => {
                const multiPolygon = createObject({
                    type: 'MultiPolygon',
                    coordinates: [
                        [
                            [[-70, 70], [-80, 60], [-50, 50], [-70, 70]]
                        ],
                        [
                            [[-80, 60], [-90, 50], [-60, 40], [-80, 60]],
                            [[-90, 80], [-90, 30], [-20, 40], [-90, 80]]
                        ]
                    ]
                });

                commonTests(multiPolygon);
            });

            describe('Rectangle', () => {
                const rect = createObject({
                    type: 'Rectangle',
                    coordinates: [[1, 2], [3, 4]]
                });

                commonTests(rect);
            });

            describe('Circle', () => {
                const circle = createObject({
                    type: 'Circle',
                    coordinates: [37.64, 55.76],
                    radius: 10000
                });

                commonTests(circle);
            });
        });
    });
};

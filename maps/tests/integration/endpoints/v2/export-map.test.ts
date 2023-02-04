import * as got from 'got';
import {expect} from 'chai';
import {validateYmapsmlSchema, validateKmlSchema} from 'tests/xml-schema';
import {EndpointSuiteDefinition} from 'tests/integration/endpoints/suite';
import {createResponseSchemaValidator} from 'tests/integration/response-validator';
import {UNKNOWN_SERVICE_TICKET, SELF_SERVICE_TICKET} from 'tests/integration/endpoints/tvm-tickets';
import {mapFixture} from 'tests/integration/endpoints/fixtures/map';
import {objectFixture} from 'tests/integration/endpoints/fixtures/object';

const validateResponseSchema = createResponseSchemaValidator({
    path: '/v2/public_maps/{sid}/export'
});

export const main: EndpointSuiteDefinition = (context) => {
    describe('GET /v2/public_maps/{sid}/export', () => {
        describe('basic', () => {
            beforeEach(async () => {
                await context.db.loadFixtures([
                    {
                        table: 'maps',
                        rows: [
                            {
                                id: 1,
                                sid: 'private-map-sid',
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
                                sid: 'public-deleted-map-sid',
                                time_created: '2016-07-14 01:00:00',
                                time_updated: '2016-07-14 01:00:00',
                                revision: '0',
                                deleted: true,
                                properties: {...mapFixture.properties, access: 'public'},
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
                it('should return 401 for request without service ticket', async () => {
                    const res = await context.server.request('/v2/public_maps/nonexistent-map-sid/export');
                    expect(res.statusCode).to.equal(401);
                });

                it('should return 403 for unknown service ticket', async () => {
                    const res = await context.server.request('/v2/public_maps/nonexistent-map-sid/export', {
                        headers: {
                            'X-Ya-Service-Ticket': UNKNOWN_SERVICE_TICKET
                        }
                    });
                    expect(res.statusCode).to.equal(403);
                });
            });

            it('should return 404 if map does not exist', async () => {
                const res = await context.server.request('/v2/public_maps/nonexistent-map-sid/export', {
                    headers: {
                        'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                    },
                    query: {
                        format: 'ymapsml'
                    },
                    json: true
                });
                expect(res.statusCode).to.equal(404);
                validateResponseSchema(res);
                expect(res.body.type).to.equal('MapNotFoundError');
            });

            it('should return 404 if map is private', async () => {
                const res = await context.server.request('/v2/public_maps/private-map-sid/export', {
                    headers: {
                        'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                    },
                    query: {
                        format: 'ymapsml'
                    },
                    json: true
                });
                expect(res.statusCode).to.equal(404);
                validateResponseSchema(res);
                expect(res.body.type).to.equal('MapNotFoundError');
            });

            it('should return 404 if public map is deleted', async () => {
                const res = await context.server.request('/v2/public_maps/public-deleted-map-sid/export', {
                    headers: {
                        'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                    },
                    query: {
                        format: 'ymapsml'
                    },
                    json: true
                });
                expect(res.statusCode).to.equal(404);
                validateResponseSchema(res);
                expect(res.body.type).to.equal('MapNotFoundError');
            });
        });

        describe('export', () => {
            async function exportMap(
                mapSid: string,
                format: 'ymapsml' | 'kml'
            ): Promise<got.Response<string>> {
                const res = await context.server.request(`/v2/public_maps/${mapSid}/export`, {
                    headers: {
                        'X-Ya-Service-Ticket': SELF_SERVICE_TICKET
                    },
                    query: {
                        format
                    }
                });
                expect(res.statusCode).to.equal(200);
                expect(res.headers['content-type']).to.equal('application/xml; charset=utf-8');
                return res;
            }

            beforeEach(async () => {
                await context.db.loadFixtures([
                    {
                        table: 'maps',
                        rows: [
                            {
                                id: 1,
                                sid: 'public-map-sid',
                                time_created: '2016-07-14 01:00:00',
                                time_updated: '2016-07-14 01:00:00',
                                revision: '0',
                                deleted: false,
                                properties: {...mapFixture.properties, access: 'public'},
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
            });

            describe('public map without objects', () => {
                it('to YMapsML', async () => {
                    const res = await exportMap('public-map-sid', 'ymapsml');
                    validateYmapsmlSchema(res.body);
                });

                it('to KML', async () => {
                    const res = await exportMap('public-map-sid', 'kml');
                    validateKmlSchema(res.body);
                });
            });

            describe('public map with objects', () => {
                beforeEach(() => {
                    return context.db.loadFixtures([
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
                                },
                                {
                                    id: 2,
                                    map_id: 1,
                                    geometry: {
                                        type: 'LineString',
                                        coordinates: [[1, 2], [3, 4]]
                                    },
                                    properties: objectFixture.properties,
                                    options: objectFixture.options,
                                    bbox_max_quad_key: 1,
                                    bbox_min_quad_key: 1
                                }
                            ]
                        }
                    ]);
                });

                it('to YMapsML', async () => {
                    const res = await exportMap('public-map-sid', 'ymapsml');
                    validateYmapsmlSchema(res.body);
                });

                it('to KML', async () => {
                    const res = await exportMap('public-map-sid', 'kml');
                    validateKmlSchema(res.body);
                });
            });
        });
    });
};

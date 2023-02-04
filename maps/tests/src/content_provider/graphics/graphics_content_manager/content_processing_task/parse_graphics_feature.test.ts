import {expect} from 'chai';
import {ObjectIdProvider} from '../../../../../../src/vector_render_engine/util/id_provider';
import {parseGraphicsFeature} from '../../../../../../src/vector_render_engine/content_provider/graphics/content_manager/content_processing_task/parsers/parse_graphics_object';
import {GraphicsFeature} from 'vector_render_engine/content_provider/graphics/graphics_object/graphics_object_description';

describe('parsers/parse_graphics_object', () => {
    describe('parseGraphicsObject', () => {
        let idProvider: ObjectIdProvider;

        beforeEach(() => {
            let i = 0;
            let j = 100;
            idProvider = {idGenerator: () => i++, collidingIdGenerator: () => j++}
        });

        it('should parse geojson', () => {
            const feature: GraphicsFeature = {
                type: 'FeatureCollection',
                features: [{
                    type: 'Feature',
                    geometry: {
                        type: 'LineString',
                        coordinates: [{x: 0, y: 0}, {x: 0, y: 1}]
                    },
                    properties: {
                        style: {
                            'line-width': 1,
                            'line-color': '#f00'
                        },
                        metadata: {
                            key: 0
                        }
                    }
                }, {
                    type: 'Feature',
                    geometry: {
                        type: 'Polygon',
                        coordinates: [[
                            {x: 0, y: 0},
                            {x: 1, y: 0},
                            {x: 0, y: 1},
                            {x: 1, y: 1}
                        ]]
                    },
                    properties: {
                        style: {
                            'fill-color': '#f00',
                            'fill-contour-color': '#0f0',
                            'fill-contour-width': 1
                        },
                        metadata: {
                            key: 1
                        }
                    }
                }, {
                    type: 'Feature',
                    geometry: {
                        type: 'Point',
                        coordinates: {x: 0, y: 0}
                    },
                    properties: {
                        style: {
                            'icon-image': 'http://url.com'
                        },
                        metadata: {
                            key: 2
                        }
                    }
                }]
            };

            const {polylines, polygons, icons} = parseGraphicsFeature(feature, idProvider);

            expect(polylines.length).to.equal(2);
            expect(polygons.length).to.equal(1);
            expect(icons.length).to.equal(1);
            expect(polylines[0].metadata!.get('key')).to.equal(0);
            expect(polylines[1].metadata!.get('key')).to.equal(1);
            expect(polygons[0].metadata!.get('key')).to.equal(1);
            expect(icons[0].metadata!.get('key')).to.equal(2);
            expect(icons[0].id).to.equal(100);
        });
    });
});

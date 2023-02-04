import {expect} from 'chai';
import {Presentation} from '../../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile_content_manager/content_processing_tasks/proto_aliases';
import {extractStyles} from '../../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile_content_manager/content_processing_tasks/util/extract_styles';
import {ParsedPrimitiveStyle} from 'vector_render_engine/content_provider/base_vector_content_provider/content_manager/content_processing_tasks/primitive/parsed_primitive_style';
import {TileItem} from '../../../../../../src/vector_render_engine/content_provider/vector_tile_map/util/tiles/tile_system';

function stubbyStyleConverter(
    _slice: Presentation.Class.ZoomSlice,
    styleBase: ParsedPrimitiveStyle
): ParsedPrimitiveStyle {
    return {...styleBase};
}

describe('style extraction', () => {
    describe('zoom slice style production', () => {
        function makeTile(zoom: number): TileItem {
            return {x: 40, y: 30, zoom};
        }

        it('should produce style for all zooms if the is a slice matching the tile min/max zoom range', () => {
            const presentation: any = [{
                classes: [{
                    id: 0,
                    slices: [{
                        zIndex: 1,
                        visibility: {
                            min: 2,
                            max: 2
                        }
                    }]
                }]
            }];

            expect([...extractStyles(presentation, 0, stubbyStyleConverter, makeTile(2))]).to.be.deep.equal([{
                classId: 0,
                zIndex: 1,
                minZoom: -Infinity,
                maxZoom: +Infinity
            }]);
        });

        it('should not produce style for a zoom range that is not specified in any slice', () => {
            const presentation: any = [{
                classes: [{
                    id: 0,
                    slices: [{
                        zIndex: 1,
                        visibility: {
                            min: 2,
                            max: 3
                        }
                    }]
                }]
            }];

            expect([...extractStyles(presentation, 0, stubbyStyleConverter, makeTile(3))]).to.be.deep.equal([{
                classId: 0,
                zIndex: 1,
                minZoom: 2,
                maxZoom: +Infinity
            }]);
        });

        it('should not produce style for a zoom range that is not specified in any slice', () => {
            const presentation: any = [{
                classes: [{
                    id: 0,
                    slices: [{
                        zIndex: 1,
                        visibility: {
                            min: 1,
                            max: 2
                        }
                    }]
                }]
            }];

            expect([...extractStyles(presentation, 0, stubbyStyleConverter, makeTile(1))]).to.be.deep.equal([{
                classId: 0,
                zIndex: 1,
                minZoom: -Infinity,
                maxZoom: 2
            }]);
        });

        it('should produce ragged styles according to zoom slices', () => {
            const presentation: any = [{
                classes: [{
                    id: 0,
                    slices: [{
                        zIndex: 1,
                        visibility: {
                            min: 1,
                            max: 1
                        }
                    }, {
                        zIndex: 2,
                        visibility: {
                            min: 2,
                            max: 3
                        }
                    }]
                }]
            }];

            expect([...extractStyles(presentation, 0, stubbyStyleConverter, makeTile(3))]).to.be.deep.equal([{
                classId: 0,
                zIndex: 1,
                minZoom: 1,
                maxZoom: 1
            }, {
                classId: 0,
                zIndex: 2,
                minZoom: 2,
                maxZoom: +Infinity
            }]);
        });

        it('should still provide styles even if there are no proper slices', () => {
            const presentation: any = [{
                classes: [{
                    id: 0,
                    slices: [{
                        zIndex: 1,
                        visibility: {
                            min: 1,
                            max: 1
                        }
                    }]
                }]
            }];

            expect([...extractStyles(presentation, 0, stubbyStyleConverter, makeTile(3))]).to.be.deep.equal([{
                classId: 0,
                zIndex: 1,
                minZoom: 1,
                maxZoom: 1
            }]);
        });
    });

});

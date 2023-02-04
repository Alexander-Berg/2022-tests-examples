import {VectorTile} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile/vector_tile';
import {VectorPrimitiveTypes} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/util/vector_primitive_types';

export function makeTileVisualizable(tile: VectorTile): VectorTile {
    const primitives = tile.content.primitives;
    primitives[VectorPrimitiveTypes.OPAQUE_POLYGON]  = primitives[VectorPrimitiveTypes.OPAQUE_POLYGON] || [];
    primitives[VectorPrimitiveTypes.POINT_LABEL]  = primitives[VectorPrimitiveTypes.POINT_LABEL] || [];
    return tile
}

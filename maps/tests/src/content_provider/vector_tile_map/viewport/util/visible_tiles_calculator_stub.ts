import {
    DEFAULT_TARGET_ZOOM_SHIFT,
    VisibleTilesCalculator
} from '../../../../../../src/vector_render_engine/content_provider/vector_tile_map/viewport/util/visible_tiles_calculator';
import Camera from '../../../../../../src/vector_render_engine/camera';
import {TileSize} from '../../../../../../src/vector_render_engine/content_provider/vector_tile_map/util/tile_size';
import {TileItem} from '../../../../../../src/vector_render_engine/content_provider/vector_tile_map/util/tiles/tile_system';

export class VisibleTilesCalculatorStub extends VisibleTilesCalculator {
    readonly visibleTiles: Set<TileItem>;

    constructor(
        tileSize: TileSize = TileSize.X1,
        targetZoomShift: number = DEFAULT_TARGET_ZOOM_SHIFT
    ) {
        super(new Camera(), tileSize, targetZoomShift);
        this.visibleTiles = new Set();
    }

    recalculateVisibleTiles(): Iterable<TileItem> {
        return this.visibleTiles;
    }
}

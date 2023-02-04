import {
    DEFAULT_OPTIONS,
    TileViewport,
    ViewportChange
} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/viewport/tile_viewport';
import {createPairOfCommunicators} from '../util/backend_communicator';
import {ViewportMessages} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/viewport/tile_viewport_messages';
import Camera from '../../../../../src/vector_render_engine/camera';
import {VisibleTilesCalculatorStub} from './util/visible_tiles_calculator_stub';

export class TileViewportStub extends TileViewport {
    readonly tilesCalculator: VisibleTilesCalculatorStub;

    constructor() {
        const tilesCalculator = new VisibleTilesCalculatorStub();
        super(createPairOfCommunicators<ViewportMessages>().master, new Camera(), DEFAULT_OPTIONS, tilesCalculator);

        this.tilesCalculator = tilesCalculator;
    }

    emulateViewportUpdate(update: ViewportChange): void {
        for (const tile of update.visibleAdded) {
            this._visibleTiles.set(tile.id, tile);
        }
        for (const tile of update.visibleRemoved) {
            this._visibleTiles.delete(tile.id);
        }
        this._onViewportChanged.fire(update);
    }
}

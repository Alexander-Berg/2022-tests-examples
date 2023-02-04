import {GraphicsViewport} from 'vector_render_engine/content_provider/graphics/graphics_viewport/graphics_viewport';
import {createPairOfCommunicators} from '../../vector_tile_map/util/backend_communicator';
import Camera from 'vector_render_engine/camera';

export class GraphicsViewportStub extends GraphicsViewport {
    constructor() {
        super(createPairOfCommunicators().master as any, new Camera());
    }

    setZoom(zoom: number): void {
        this._tryToUpdateZoom(zoom);
    }
}

import {GraphicsViewportBackend} from 'vector_render_engine/content_provider/graphics/graphics_viewport/graphics_viewport_backend';
import {createPairOfCommunicators} from '../../vector_tile_map/util/backend_communicator';

export class GraphicsViewportBackendStub extends GraphicsViewportBackend {
    constructor() {
        super(createPairOfCommunicators().master as any);
    }

    setZoom(zoom: number): void {
        this._updateZoom(zoom);
    }
}

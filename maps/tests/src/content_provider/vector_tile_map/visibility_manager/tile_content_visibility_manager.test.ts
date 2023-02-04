import {expect} from 'chai';
import {VectorTile} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile/vector_tile';
import {Scene} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/scene';
import {VectorPrimitiveTypes} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/util/vector_primitive_types';
import {SceneStub} from '../scene_stub';
import {VectorContentVisibilityManager} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/visibility_manager/vector_content_visibility_manager';

describe('ViewportVisibilityManager', () => {
    const TILE_1_1 = {id: '1', x: 1, y: 1, zoom: 1};
    const TILE_2_1 = {id: '2', x: 2, y: 1, zoom: 1};
    const POLYGON1 = {} as any;
    const POLYGON2 = {} as any;
    const POLYGON3 = {} as any;
    const POLYGON4 = {} as any;

    let scene: Scene;
    let contentManager: VectorContentVisibilityManager;

    beforeEach(() => {
        scene = new SceneStub();
        contentManager = new VectorContentVisibilityManager(scene);
    });

    it('should add into scene content of visible tiles and remove content of hidden', () => {
        const tile1 = new VectorTile(TILE_1_1);
        const tile2 = new VectorTile(TILE_2_1);

        tile1.addContent({
            primitives: {[VectorPrimitiveTypes.OPAQUE_POLYGON]: [POLYGON1, POLYGON2]},
            references: []
        });
        tile2.addContent({
            primitives: {
                [VectorPrimitiveTypes.OPAQUE_POLYGON]: [POLYGON3],
                [VectorPrimitiveTypes.TRANSPARENT_POLYGON]: [POLYGON4]
            },
            references: []
        });

        contentManager.showObject(tile1);
        contentManager.showObject(tile2);
        expect([...scene[VectorPrimitiveTypes.OPAQUE_POLYGON].primitives].length).to.be.equal(3);
        expect([...scene[VectorPrimitiveTypes.TRANSPARENT_POLYGON].primitives].length).to.be.equal(1);

        contentManager.hideObject(tile1);
        expect([...scene[VectorPrimitiveTypes.OPAQUE_POLYGON].primitives].length).to.be.equal(1);
        expect([...scene[VectorPrimitiveTypes.TRANSPARENT_POLYGON].primitives].length).to.be.equal(1);
    });

});

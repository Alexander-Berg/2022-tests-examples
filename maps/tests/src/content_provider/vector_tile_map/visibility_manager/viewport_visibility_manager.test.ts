import {expect} from 'chai';
import {VectorTile} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile/vector_tile';
import {ViewportVisibilityManager} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/visibility_manager/viewport_visibility_manager';
import {TileStorageStub} from '../tile_storage/tile_storage';
import {TileViewportStub} from '../viewport/tile_viewport_stub';
import {TileContentVisibilityManagerStub} from './tile_content_visibility_manager_stub';
import {TileModelVisibilityManager} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/visibility_manager/tile_model_visibility_manager';
import {ModelVisibilityManagerStub} from './model_visibility_manager_stub';
import {makeTileVisualizable} from '../util/visualizable_tile';

describe('ViewportVisibilityManager', () => {
    const TILE_1_1 = {id: '1', x: 1, y: 1, zoom: 1};
    const TILE_2_1 = {id: '2', x: 2, y: 1, zoom: 1};
    const TILE_3_1 = {id: '3', x: 3, y: 1, zoom: 1};

    let viewport: TileViewportStub;
    let storage: TileStorageStub<VectorTile>;
    let tileContentManager: TileContentVisibilityManagerStub;
    let modelVisibilityManager: TileModelVisibilityManager;
    let viewportManager: ViewportVisibilityManager;

    beforeEach(() => {
        viewport = new TileViewportStub();
        storage = new TileStorageStub();
        tileContentManager = new TileContentVisibilityManagerStub();
        modelVisibilityManager = new ModelVisibilityManagerStub();
        viewportManager = new ViewportVisibilityManager(
            viewport,
            storage,
            tileContentManager,
            modelVisibilityManager,
            {submit: () => {}},
            'map'
        );
    });

    it.skip('should show tiles if all of them ready', () => {
        const tile1 = new VectorTile(TILE_1_1);
        const tile2 = new VectorTile(TILE_2_1);
        const tile3 = new VectorTile(TILE_3_1);

        storage.addTile(tile1);
        storage.addTile(tile2);
        storage.addTile(tile3);

        makeTileVisualizable(tile1);
        viewport.emulateViewportUpdate({visibleAdded: [TILE_1_1, TILE_2_1, TILE_3_1], visibleRemoved: []});
        expect(tileContentManager.visibleTiles.size).to.be.equal(0);

        makeTileVisualizable(tile2);
        makeTileVisualizable(tile3);
        viewport.emulateViewportUpdate({visibleAdded: [TILE_1_1, TILE_2_1, TILE_3_1], visibleRemoved: []});
        expect(tileContentManager.visibleTiles.size).to.be.equal(3);
    });

    it('should hide invisible tiles', () => {
        const tile1 = new VectorTile(TILE_1_1);
        const tile2 = new VectorTile(TILE_2_1);
        const tile3 = new VectorTile(TILE_3_1);

        makeTileVisualizable(tile1);
        makeTileVisualizable(tile2);
        makeTileVisualizable(tile3);

        storage.addTile(tile1);
        storage.addTile(tile2);
        storage.addTile(tile3);
        viewport.emulateViewportUpdate({visibleAdded: [TILE_1_1, TILE_2_1], visibleRemoved: []});

        viewport.emulateViewportUpdate({visibleAdded: [], visibleRemoved: [TILE_2_1]});

        expect(tileContentManager.visibleTiles.size).to.be.equal(1);
    });

    it('should remember tiles missing in storage and recalculate viewport visibility after adding', () => {
        const tile1 = new VectorTile(TILE_1_1);
        const tile2 = new VectorTile(TILE_2_1);

        makeTileVisualizable(tile1);
        makeTileVisualizable(tile2);

        storage.addTile(tile1);
        viewport.emulateViewportUpdate({visibleAdded: [TILE_1_1, TILE_2_1], visibleRemoved: []});
        expect(tileContentManager.visibleTiles.size).to.be.equal(0);

        storage.addTile(tile2);
        expect(tileContentManager.visibleTiles.size).to.be.equal(2);
    });

    it.skip('should recalculate visibility after adding content to visible tile', () => {
        const tile1 = new VectorTile(TILE_1_1);
        const tile2 = new VectorTile(TILE_2_1);

        storage.addTile(tile1);
        storage.addTile(tile2);

        makeTileVisualizable(tile1);
        viewport.emulateViewportUpdate({visibleAdded: [TILE_1_1, TILE_2_1], visibleRemoved: []});

        makeTileVisualizable(tile2);
        tile2.addContent({primitives: {}, references: []});
        expect(tileContentManager.visibleTiles.size).to.be.equal(2);
    });

});

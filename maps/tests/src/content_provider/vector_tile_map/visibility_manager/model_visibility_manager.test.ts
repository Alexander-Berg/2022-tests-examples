import {expect} from 'chai';
import {VectorTile} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile/vector_tile';
import {Scene} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/scene';
import {VectorPrimitiveTypes} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/util/vector_primitive_types';
import {SceneStub} from '../scene_stub';
import {ModelStorageStub} from '../model_storage/model_storage_stub';
import {TileModelVisibilityManager} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/visibility_manager/tile_model_visibility_manager';
import {VectorModel} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/model_storage/vector_model';
import {VectorContentVisibilityManager} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/visibility_manager/vector_content_visibility_manager';

describe('ModelVisibilityManager', () => {
    const MODEL_ID1 = 'model#1';
    const MODEL_ID2 = 'model#2';
    const TILE_1_1 = {id: 'tile#1', x: 1, y: 1, zoom: 1};
    const TILE_2_1 = {id: 'tile#2', x: 2, y: 1, zoom: 1};

    const RENDERABLE_PRIMITIVE_STUB1: any = {};
    const RENDERABLE_PRIMITIVE_STUB2: any = {};
    let scene: Scene;
    let modelStorage: ModelStorageStub;
    let contentVisibilityManager: VectorContentVisibilityManager;
    let manager: TileModelVisibilityManager;

    beforeEach(() => {
        scene = new SceneStub();
        contentVisibilityManager = new VectorContentVisibilityManager(scene);
        modelStorage = new ModelStorageStub();
        manager = new TileModelVisibilityManager(contentVisibilityManager, modelStorage);
    });

    it('should add model primitives into scene', () => {
        const model = new VectorModel(MODEL_ID1);
        model.addContent({primitives: {[VectorPrimitiveTypes.MESH]: [RENDERABLE_PRIMITIVE_STUB1]}});
        modelStorage.addModel(model);

        const tile1 = new VectorTile(TILE_1_1);
        tile1.addContent({primitives: {}, references: [MODEL_ID1]});

        manager.showTileModels(tile1);
        expect([...scene[VectorPrimitiveTypes.MESH].primitives].length).to.be.equal(1);

        manager.hideTileModels(tile1);
        expect([...scene[VectorPrimitiveTypes.MESH].primitives].length).to.be.equal(0);
    });

    it('should wait for model', () => {
        const model = new VectorModel(MODEL_ID1);

        const tile1 = new VectorTile(TILE_1_1);
        tile1.addContent({primitives: {}, references: [MODEL_ID1]});

        manager.showTileModels(tile1);
        expect([...scene[VectorPrimitiveTypes.MESH].primitives].length).to.be.equal(0);

        modelStorage.addModel(model);
        expect([...scene[VectorPrimitiveTypes.MESH].primitives].length).to.be.equal(0);

        model.addContent({primitives: {[VectorPrimitiveTypes.MESH]: [RENDERABLE_PRIMITIVE_STUB1]}});
        expect([...scene[VectorPrimitiveTypes.MESH].primitives].length).to.be.equal(1);
    });

    it('should ignore updates of hidden models', () => {
        const model = new VectorModel(MODEL_ID1);

        const tile1 = new VectorTile(TILE_1_1);
        tile1.addContent({primitives: {}, references: [MODEL_ID1]});

        manager.showTileModels(tile1);
        expect([...scene[VectorPrimitiveTypes.MESH].primitives].length).to.be.equal(0);

        manager.hideTileModels(tile1);
        modelStorage.addModel(model);
        expect([...scene[VectorPrimitiveTypes.MESH].primitives].length).to.be.equal(0);

        model.addContent({primitives: {[VectorPrimitiveTypes.MESH]: [RENDERABLE_PRIMITIVE_STUB1]}});
        expect([...scene[VectorPrimitiveTypes.MESH].primitives].length).to.be.equal(0);
    });

    it('should count references for tiles', () => {
        const model1 = new VectorModel(MODEL_ID1);
        modelStorage.addModel(model1);
        model1.addContent({primitives: {[VectorPrimitiveTypes.MESH]: [RENDERABLE_PRIMITIVE_STUB1]}});
        const model2 = new VectorModel(MODEL_ID2);
        model2.addContent({primitives: {[VectorPrimitiveTypes.MESH]: [RENDERABLE_PRIMITIVE_STUB2]}});
        modelStorage.addModel(model2);

        const tile1 = new VectorTile(TILE_1_1);
        tile1.addContent({primitives: {}, references: [MODEL_ID1]});
        const tile2 = new VectorTile(TILE_2_1);
        tile2.addContent({primitives: {}, references: [MODEL_ID1, MODEL_ID2]});

        manager.showTileModels(tile1);
        expect([...scene[VectorPrimitiveTypes.MESH].primitives].length).to.be.equal(1);

        manager.showTileModels(tile2);
        expect([...scene[VectorPrimitiveTypes.MESH].primitives].length).to.be.equal(2);

        manager.hideTileModels(tile2);
        expect([...scene[VectorPrimitiveTypes.MESH].primitives].length).to.be.equal(1);

        manager.hideTileModels(tile1);
        expect([...scene[VectorPrimitiveTypes.MESH].primitives].length).to.be.equal(0);
    });

    it('should hide removed from storage models', () => {
        const model1 = new VectorModel(MODEL_ID1);
        modelStorage.addModel(model1);
        model1.addContent({primitives: {[VectorPrimitiveTypes.MESH]: [RENDERABLE_PRIMITIVE_STUB1]}});

        const tile1 = new VectorTile(TILE_1_1);
        tile1.addContent({primitives: {}, references: [MODEL_ID1]});

        manager.showTileModels(tile1);
        expect([...scene[VectorPrimitiveTypes.MESH].primitives].length).to.be.equal(1);

        modelStorage.removeModel(model1);
        expect([...scene[VectorPrimitiveTypes.MESH].primitives].length).to.be.equal(0);

        modelStorage.addModel(model1);
        expect([...scene[VectorPrimitiveTypes.MESH].primitives].length).to.be.equal(1);

        manager.hideTileModels(tile1);
        expect([...scene[VectorPrimitiveTypes.MESH].primitives].length).to.be.equal(0);
    });
});

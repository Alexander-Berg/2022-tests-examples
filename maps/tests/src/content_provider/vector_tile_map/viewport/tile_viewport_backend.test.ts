import {expect} from 'chai';
import {CenterWrapMode} from '../../../../../src/vector_render_engine/camera';
import {ViewportMessages, ViewportMessageType} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/viewport/tile_viewport_messages';
import {createPairOfCommunicators} from '../util/backend_communicator';
import {promisifyEventEmitter} from '../../../util/event_emitter';
import {convertToIdentified, IdentifiedTileItem} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/util/identified_tile_item';
import {TileViewportBackend} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/viewport/tile_viewport_backend';
import {VectorTileBackendState} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile/vector_tile_backend';
import {TileStorageBackend} from 'vector_render_engine/content_provider/vector_tile_map/tile_storage/tile_storage_backend';
import {SyncStorageMessages} from 'vector_render_engine/content_provider/base_vector_content_provider/storage/sync_storage_messages';

function identifiedTilesComparator(a: IdentifiedTileItem, b: IdentifiedTileItem): number {
    return a.id < b.id ? -1 : a.id === b.id ? 0 : 1;
}

function createTiles(zoom: number, prefix: string): IdentifiedTileItem[][] {
    const size = Math.pow(2, zoom);
    const tiles: IdentifiedTileItem[][] = [];
    for (let i = 0; i < size; ++i) {
        tiles[i] = [];
        for (let j = 0; j < size; ++j) {
            tiles[i][j] = convertToIdentified({x: i, y : j, zoom: zoom}, prefix);
        }
    }
    return tiles;
}

describe('TileViewportBackend', () => {
    const WRAP_X = CenterWrapMode.REPEAT;
    const WRAP_Y = CenterWrapMode.CLAMP_TO_EDGE;
    const PRELOADED_TILE_BELT_WIDTH = 1;
    const TILE_ID_PREFIX = 'test_tile_prefix';
    const USE_LOW_PRECISION_BELT_TILES = true;
    const SETUP_MESSAGE = {
        tileIdPrefix: TILE_ID_PREFIX,
        preloadedTilesBeltWidth: PRELOADED_TILE_BELT_WIDTH,
        wrapModeX: WRAP_X,
        wrapModeY: WRAP_Y,
        useLowPrecisionBeltTiles: USE_LOW_PRECISION_BELT_TILES
    };

    it.skip('should add tiles with correct states into storage', () => {
        const tiles = createTiles(3, TILE_ID_PREFIX);
        const stubStorageCommunicator = createPairOfCommunicators<SyncStorageMessages<IdentifiedTileItem, string>>();
        const stubStorage = new TileStorageBackend(stubStorageCommunicator.worker);
        const stubCommunicators = createPairOfCommunicators<ViewportMessages>();
        const viewportBackend = new TileViewportBackend(stubCommunicators.worker, stubStorage);

        stubCommunicators.master.sendMessage({type: ViewportMessageType.TO_BACKEND_SETUP, ...SETUP_MESSAGE}, true);
        stubCommunicators.master.sendMessage({
            type: ViewportMessageType.TO_BACKEND_UPDATE_VIEWPORT,
            visibleTilesToAdd: [tiles[2][2], tiles[3][2]],
            visibleTilesToRemove: [],
            cameraPosition: {x: 0, y: 0, zoom: 3}
        }, true);

        expect(stubStorage.size).to.be.equal(12);
        expect(stubStorage.get(tiles[2][2].id)!.state).to.be.equal(VectorTileBackendState.VISIBLE);
        expect(stubStorage.get(tiles[3][2].id)!.state).to.be.equal(VectorTileBackendState.VISIBLE);
        expect(stubStorage.get(tiles[1][1].id)!.state).to.be.equal(VectorTileBackendState.PRELOADED);
        expect(stubStorage.get(tiles[2][1].id)!.state).to.be.equal(VectorTileBackendState.PRELOADED);
        expect(stubStorage.get(tiles[3][1].id)!.state).to.be.equal(VectorTileBackendState.PRELOADED);
        expect(stubStorage.get(tiles[4][1].id)!.state).to.be.equal(VectorTileBackendState.PRELOADED);
        expect(stubStorage.get(tiles[1][2].id)!.state).to.be.equal(VectorTileBackendState.PRELOADED);
        expect(stubStorage.get(tiles[4][2].id)!.state).to.be.equal(VectorTileBackendState.PRELOADED);
        expect(stubStorage.get(tiles[1][3].id)!.state).to.be.equal(VectorTileBackendState.PRELOADED);
        expect(stubStorage.get(tiles[2][3].id)!.state).to.be.equal(VectorTileBackendState.PRELOADED);
        expect(stubStorage.get(tiles[3][3].id)!.state).to.be.equal(VectorTileBackendState.PRELOADED);
        expect(stubStorage.get(tiles[4][3].id)!.state).to.be.equal(VectorTileBackendState.PRELOADED);

        stubCommunicators.master.sendMessage({
            type: ViewportMessageType.TO_BACKEND_UPDATE_VIEWPORT,
            visibleTilesToAdd: [tiles[4][2]],
            visibleTilesToRemove: [tiles[2][2]],
            cameraPosition: {x: 0, y: 0, zoom: 3}
        }, true);

        expect(stubStorage.size).to.be.equal(15);
        expect(stubStorage.get(tiles[1][1].id)!.state).to.be.equal(VectorTileBackendState.CACHED);
        expect(stubStorage.get(tiles[1][2].id)!.state).to.be.equal(VectorTileBackendState.CACHED);
        expect(stubStorage.get(tiles[1][3].id)!.state).to.be.equal(VectorTileBackendState.CACHED);
        expect(stubStorage.get(tiles[3][2].id)!.state).to.be.equal(VectorTileBackendState.VISIBLE);
        expect(stubStorage.get(tiles[4][2].id)!.state).to.be.equal(VectorTileBackendState.VISIBLE);
        expect(stubStorage.get(tiles[2][1].id)!.state).to.be.equal(VectorTileBackendState.PRELOADED);
        expect(stubStorage.get(tiles[3][1].id)!.state).to.be.equal(VectorTileBackendState.PRELOADED);
        expect(stubStorage.get(tiles[4][1].id)!.state).to.be.equal(VectorTileBackendState.PRELOADED);
        expect(stubStorage.get(tiles[5][1].id)!.state).to.be.equal(VectorTileBackendState.PRELOADED);
        expect(stubStorage.get(tiles[2][2].id)!.state).to.be.equal(VectorTileBackendState.PRELOADED);
        expect(stubStorage.get(tiles[5][2].id)!.state).to.be.equal(VectorTileBackendState.PRELOADED);
        expect(stubStorage.get(tiles[2][3].id)!.state).to.be.equal(VectorTileBackendState.PRELOADED);
        expect(stubStorage.get(tiles[3][3].id)!.state).to.be.equal(VectorTileBackendState.PRELOADED);
        expect(stubStorage.get(tiles[4][3].id)!.state).to.be.equal(VectorTileBackendState.PRELOADED);
        expect(stubStorage.get(tiles[5][3].id)!.state).to.be.equal(VectorTileBackendState.PRELOADED);
    });

    it.skip('should emit updates of the viewport', async () => {
        const tiles = createTiles(3, TILE_ID_PREFIX);
        const stubStorageCommunicator = createPairOfCommunicators<SyncStorageMessages<IdentifiedTileItem, string>>();
        const stubStorage = new TileStorageBackend(stubStorageCommunicator.worker);
        const stubCommunicators = createPairOfCommunicators<ViewportMessages>();
        const viewportBackend = new TileViewportBackend(stubCommunicators.worker, stubStorage);

        stubCommunicators.master.sendMessage({type: ViewportMessageType.TO_BACKEND_SETUP, ...SETUP_MESSAGE}, true);

        const update1 = promisifyEventEmitter(viewportBackend.onViewportChanged).then(([{
            visibleAdded,
            visibleRemoved,
            preloadedAdded,
            preloadedRemoved
        }]) => {
            expect(visibleAdded.sort(identifiedTilesComparator)).to.be.deep.equal([
                tiles[1][1],
                tiles[1][2],
                tiles[2][1],
                tiles[2][2]
            ]);
            expect(visibleRemoved).to.be.deep.eq([]);
            expect(preloadedAdded.sort(identifiedTilesComparator)).to.be.deep.equal([
                tiles[0][0],
                tiles[0][1],
                tiles[0][2],
                tiles[0][3],
                tiles[1][0],
                tiles[1][3],
                tiles[2][0],
                tiles[2][3],
                tiles[3][0],
                tiles[3][1],
                tiles[3][2],
                tiles[3][3]
            ]);
            expect(preloadedRemoved).to.be.deep.equal([]);
        });

        stubCommunicators.master.sendMessage({
            type: ViewportMessageType.TO_BACKEND_UPDATE_VIEWPORT,
            visibleTilesToAdd: [
                tiles[1][1],
                tiles[2][1],
                tiles[1][2],
                tiles[2][2]
            ],
            visibleTilesToRemove: [],
            cameraPosition: {x: 0, y: 0, zoom: 3}
        }, true);

        await update1;

        const update2 = promisifyEventEmitter(viewportBackend.onViewportChanged).then(([{
            visibleAdded,
            visibleRemoved,
            preloadedAdded,
            preloadedRemoved
        }]) => {
            expect(visibleAdded.sort(identifiedTilesComparator)).to.be.deep.equal([
                tiles[0][0],
                tiles[0][1],
                tiles[1][0]
            ]);
            expect(visibleRemoved).to.be.deep.eq([
                tiles[1][2],
                tiles[2][1],
                tiles[2][2]
            ]);

            expect(preloadedAdded.sort(identifiedTilesComparator)).to.be.deep.equal([
                tiles[1][2],
                tiles[2][1],
                tiles[2][2],
                tiles[7][0],
                tiles[7][1],
                tiles[7][2]
            ]);
            expect(preloadedRemoved.sort(identifiedTilesComparator)).to.be.deep.equal([
                tiles[0][0],
                tiles[0][1],
                tiles[0][3],
                tiles[1][0],
                tiles[1][3],
                tiles[2][3],
                tiles[3][0],
                tiles[3][1],
                tiles[3][2],
                tiles[3][3]
            ]);
        });

        stubCommunicators.master.sendMessage({
            type: ViewportMessageType.TO_BACKEND_UPDATE_VIEWPORT,
            visibleTilesToAdd: [
                tiles[0][0],
                tiles[0][1],
                tiles[1][0]
            ],
            visibleTilesToRemove: [
                tiles[1][2],
                tiles[2][1],
                tiles[2][2]
            ],
            cameraPosition: {x: 0, y: 0, zoom: 3}
        }, true);

        await update2;
    });

});

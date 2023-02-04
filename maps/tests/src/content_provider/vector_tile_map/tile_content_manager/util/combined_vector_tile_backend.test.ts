import {expect} from 'chai';
import {CombinedVectorTileBackend} from '../../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile_content_manager/util/combined_vector_tile_backend';
import {
    VectorTileBackend,
    VectorTileBackendState
} from '../../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile/vector_tile_backend';
import {promisifyEventEmitter} from '../../../../util/event_emitter';

describe('CombinedVectorTileBackend', () => {
    it('should be in proper state according to tiles added', () => {
        const tile = new CombinedVectorTileBackend();
        expect(tile.state).to.be.equal(VectorTileBackendState.CACHED);

        tile.addTile(new VectorTileBackend({id: '#1', x: 1, y: 1, zoom: 1}, VectorTileBackendState.PRELOADED));
        expect(tile.state).to.be.equal(VectorTileBackendState.PRELOADED);

        tile.addTile(new VectorTileBackend({id: '#2', x: 2, y: 1, zoom: 1}, VectorTileBackendState.VISIBLE));
        expect(tile.state).to.be.equal(VectorTileBackendState.VISIBLE);

        tile.addTile(new VectorTileBackend({id: '#3', x: 3, y: 1, zoom: 1}, VectorTileBackendState.PRELOADED));
        expect(tile.state).to.be.equal(VectorTileBackendState.VISIBLE);
    });

    it('should update state along its sub tiles', () => {
        const tile = new CombinedVectorTileBackend();
        const subTile = new VectorTileBackend({id: '#1', x: 1, y: 1, zoom: 1}, VectorTileBackendState.PRELOADED);

        tile.addTile(subTile);
        expect(tile.state).to.be.equal(VectorTileBackendState.PRELOADED);

        subTile.state = VectorTileBackendState.VISIBLE;
        expect(tile.state).to.be.equal(VectorTileBackendState.VISIBLE);

        subTile.state = VectorTileBackendState.CACHED;
        expect(tile.state).to.be.equal(VectorTileBackendState.CACHED);

        subTile.state = VectorTileBackendState.VISIBLE;
        expect(tile.state).to.be.equal(VectorTileBackendState.VISIBLE);
    });

    it('should emit state updates', async () => {
        const tile = new CombinedVectorTileBackend();
        const subTile = new VectorTileBackend({id: '#1', x: 1, y: 1, zoom: 1}, VectorTileBackendState.PRELOADED);

        tile.addTile(subTile);

        const update1 = promisifyEventEmitter(tile.onStateChanged).then(([tile_, prevState, newState]) => {
            expect(prevState).to.be.equal(VectorTileBackendState.PRELOADED);
            expect(newState).to.be.equal(VectorTileBackendState.VISIBLE);
        });
        subTile.state = VectorTileBackendState.VISIBLE;
        await update1;

        const update2 = promisifyEventEmitter(tile.onStateChanged).then(([tile_, prevState, newState]) => {
            expect(newState).to.be.equal(VectorTileBackendState.CACHED);
        });
        subTile.state = VectorTileBackendState.CACHED;
        await update2;

        expect(tile.state).to.be.equal(VectorTileBackendState.CACHED);

    });
});

import {expect} from 'chai';
import Camera, {CenterWrapMode} from '../../../../../src/vector_render_engine/camera';
import {
    DEFAULT_OPTIONS,
    TileViewport
} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/viewport/tile_viewport';
import {
    ViewportMessages,
    ViewportMessageType
} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/viewport/tile_viewport_messages';
import {createPairOfCommunicators} from '../util/backend_communicator';
import {promisifyEventEmitter} from '../../../util/event_emitter';
import {convertToIdentified} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/util/identified_tile_item';
import {VisibleTilesCalculatorStub} from './util/visible_tiles_calculator_stub';

describe('TileViewport', () => {
    it('should submit TO_BACKEND_SETUP message', async () => {
        const WRAP_X = CenterWrapMode.CLAMP_TO_EDGE;
        const WRAP_Y = CenterWrapMode.NONE;
        const PRELOADED_TILE_BELT_WIDTH = 12;
        const TILE_ID_PREFIX = 'test_tile_prefix';
        const camera = new Camera({wrapModeX: WRAP_X, wrapModeY: WRAP_Y});
        const tilesCalculator = new VisibleTilesCalculatorStub();
        const stubCommunicators = createPairOfCommunicators<ViewportMessages>();

        const done = promisifyEventEmitter(stubCommunicators.worker.onMessage).then(([message]) => {
            if (message.type === ViewportMessageType.TO_BACKEND_SETUP) {
                expect(message.tileIdPrefix).to.be.equal(TILE_ID_PREFIX);
                expect(message.wrapModeX).to.be.equal(WRAP_X);
                expect(message.wrapModeY).to.be.equal(WRAP_Y);
                expect(message.preloadedTilesBeltWidth).to.be.equal(PRELOADED_TILE_BELT_WIDTH);
            } else {
                throw new Error('not TO_BACKEND_SETUP message caught');
             }
        });

        new TileViewport(
            stubCommunicators.master,
            camera,
            {...DEFAULT_OPTIONS, preloadedTilesBeltWidth: PRELOADED_TILE_BELT_WIDTH},
            tilesCalculator,
            TILE_ID_PREFIX
        );

        await done;
    });

    it('should emit updates and sync it with backend', async () => {
        const WRAP_X = CenterWrapMode.CLAMP_TO_EDGE;
        const WRAP_Y = CenterWrapMode.NONE;
        const TILE_ID_PREFIX = 'test_tile_prefix';
        const TILE_0_0 = {x: 0, y: 0, zoom: 1};
        const TILE_0_0_IDED = convertToIdentified(TILE_0_0, TILE_ID_PREFIX);
        const TILE_0_1 = {x: 0, y: 1, zoom: 1};
        const TILE_0_1_IDED = convertToIdentified(TILE_0_1, TILE_ID_PREFIX);
        const TILE_1_1 = {x: 1, y: 1, zoom: 1};
        const TILE_1_1_IDED = convertToIdentified(TILE_1_1, TILE_ID_PREFIX);

        const camera = new Camera({wrapModeX: WRAP_X, wrapModeY: WRAP_Y});
        const tilesCalculator = new VisibleTilesCalculatorStub();
        tilesCalculator.visibleTiles.add(TILE_0_0);
        tilesCalculator.visibleTiles.add(TILE_0_1);
        const stubCommunicators = createPairOfCommunicators<ViewportMessages>();
        const viewport = new TileViewport(
            stubCommunicators.master,
            camera,
            DEFAULT_OPTIONS,
            tilesCalculator,
            TILE_ID_PREFIX
        );

        const backendFirstUpdateSync = promisifyEventEmitter(stubCommunicators.worker.onMessage).then(([message]) => {
            if (message.type === ViewportMessageType.TO_BACKEND_UPDATE_VIEWPORT) {
                expect(message.visibleTilesToAdd).to.be.deep.equal([TILE_0_0_IDED, TILE_0_1_IDED]);
                expect(message.visibleTilesToRemove).to.be.deep.equal([]);
            } else {
                throw new Error('not TO_BACKEND_UPDATE_VIEWPORT message caught');
            }
        });
        const emitFirstUpdate = promisifyEventEmitter(viewport.onViewportChanged).then(([{
            visibleAdded,
            visibleRemoved
        }]) => {
            expect(visibleAdded).to.be.deep.equal([TILE_0_0_IDED, TILE_0_1_IDED]);
            expect(visibleRemoved).to.be.deep.eq([]);
        });

        camera.center.x = 0.0001;
        camera.flushUpdates();

        await backendFirstUpdateSync;
        await emitFirstUpdate;

        const backendSecondUpdateSync = promisifyEventEmitter(stubCommunicators.worker.onMessage).then(([message]) => {
            if (message.type === ViewportMessageType.TO_BACKEND_UPDATE_VIEWPORT) {
                expect(message.visibleTilesToAdd).to.be.deep.equal([TILE_1_1_IDED]);
                expect(message.visibleTilesToRemove).to.be.deep.equal([TILE_0_0_IDED]);
            } else {
                throw new Error('not TO_BACKEND_UPDATE_VIEWPORT message caught');
            }
        });
        const emitSecondUpdate = promisifyEventEmitter(viewport.onViewportChanged).then(([{
            visibleAdded,
            visibleRemoved
        }]) => {
            expect(visibleAdded).to.be.deep.equal([TILE_1_1_IDED]);
            expect(visibleRemoved).to.be.deep.eq([TILE_0_0_IDED]);
        });

        tilesCalculator.visibleTiles.clear();
        tilesCalculator.visibleTiles.add(TILE_0_1);
        tilesCalculator.visibleTiles.add(TILE_1_1);

        camera.center.x = 0.0002;
        camera.flushUpdates();

        await backendSecondUpdateSync;
        await emitSecondUpdate;
    });

});

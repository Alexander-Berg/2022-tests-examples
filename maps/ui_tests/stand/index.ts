import {Camera, DEFAULT_CAMERA_OPTIONS} from '../../engine/camera';
import {POV} from '../../engine/data/pov';
import {Engine, DEFAULT_ENGINE_OPTIONS} from '../../engine/engine';
import {getVisibleTiles} from '../../engine/panorama/visible_tiles';
import {allOfIterable} from '../../webgl_toolkit/util/iterable';
import * as utils from './utils';

const camera = new Camera({
    ...DEFAULT_CAMERA_OPTIONS,
    tilt: 0,
    azimuth: 0
});

const container = document.querySelector('#panorama-container') as HTMLCanvasElement;

const engine = new Engine({
    ...DEFAULT_ENGINE_OPTIONS,
    container,
    panoramaEngineRequiredTime: performance.now()
}, camera);

(window as any).camera = camera;
(window as any).engine = engine;
(window as any).container = container;
(window as any).utils = utils;

document.body.setAttribute('status', 'INITIALIZED');
document.getElementById('status')!.className = 'INITIALIZED';

(engine as any)._positionController.onMoveIntention.addListener(() => {
    document.body.setAttribute('status', 'LOADING');
    document.getElementById('status')!.className = 'LOADING';
});

function checkLoading(): void {
    const pov = camera.targetPOV;
    if (pov) {
        const visibleTiles = getVisibleTiles(camera.state, camera.targetPOV!).tiles;
        if (allOfIterable(visibleTiles, (tile) => tile.isReady)) {
            document.body.setAttribute('status', 'LOADED');
            document.getElementById('status')!.className = 'LOADED';
        } else {
            document.body.setAttribute('status', 'LOADING');
            document.getElementById('status')!.className = 'LOADING';
        }
    } else {
        document.body.setAttribute('status', 'UNKNOWN');
        document.getElementById('status')!.className = 'UNKNOWN';
    }
}
let pov: POV;
camera.onChange.addListener(() => {
    checkLoading();
    const state = camera.state;
    if (pov !== state.targetPOV && state.targetPOV) {
        if (pov) {
            pov.onChange.removeListener(checkLoading);
        }
        pov = state.targetPOV;
        pov.onChange.addListener(checkLoading);
        checkLoading();
    }
});

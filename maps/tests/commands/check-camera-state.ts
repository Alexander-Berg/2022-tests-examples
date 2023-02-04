import {wrapAsyncCommand} from '../lib/commands-utils';
import {isEqual} from 'lodash';

interface CameraState {
    zoom: number;
    tilt: number;
    azimuth: number;
}

interface CheckCameraStateOption {
    timeout: number;
}

async function checkCameraState(
    this: WebdriverIO.Browser,
    expectedCameraState: CameraState,
    {timeout = 500}: Partial<CheckCameraStateOption> = {}
): Promise<void> {
    let cameraState = await getCameraState(this);

    try {
        await this.waitUntil(
            async () => {
                cameraState = await getCameraState(this);
                return isEqual(cameraState, expectedCameraState);
            },
            timeout,
            'TIMEOUT'
        );
    } catch (e) {
        if (e.message.includes('TIMEOUT')) {
            throw new Error(
                `Ожидалось, что ${JSON.stringify(cameraState)} будет равен ${JSON.stringify(expectedCameraState)}`
            );
        }
        throw e;
    }
}

async function getCameraState(browser: WebdriverIO.Browser): Promise<CameraState> {
    const cameraState = await browser.execute(() => {
        return {
            zoom: window.yandex_map?._camera.zoom,
            tilt: window.yandex_map?._camera.tilt,
            azimuth: window.yandex_map?._camera.azimuth
        };
    });

    return cameraState;
}

export default wrapAsyncCommand(checkCameraState);

import {wrapAsyncCommand} from '../lib/commands-utils';
import {OpenPageOptions} from '../../tests/commands/open-page';
import {isEqual} from 'lodash';

interface MapState {
    center: Point;
    zoom: number;
}

interface CheckMapStateOption {
    fractionDigits: number;
    timeout: number;
}

async function checkMapState(
    this: WebdriverIO.Browser,
    expectedMapState: MapState,
    {timeout = 5000, fractionDigits = 6}: Partial<CheckMapStateOption> = {}
): Promise<void> {
    let mapState = await getMapState(this, fractionDigits);

    expectedMapState.center = expectedMapState.center.map((point: number) =>
        Number(point.toFixed(fractionDigits))
    ) as Point;

    try {
        await this.waitUntil(
            async () => {
                mapState = await getMapState(this, fractionDigits);
                return isEqual(mapState, expectedMapState);
            },
            timeout,
            'TIMEOUT'
        );
    } catch (e) {
        if (e.message.includes('TIMEOUT')) {
            throw new Error(
                `Ожидалось, что ${JSON.stringify(mapState)} будет равен ${JSON.stringify(expectedMapState)}`
            );
        }
        throw e;
    }
}

async function getMapState(browser: WebdriverIO.Browser, fractionDigits: number): Promise<MapState> {
    const mapState = await browser.execute(() => {
        return {
            zoom: window.yandex_map?.zoom,
            center: window.yandex_map?.center
        };
    });

    mapState.center = mapState.center.map((point: number) => Number(point.toFixed(fractionDigits)));

    return mapState;
}

export default wrapAsyncCommand(checkMapState);
export {OpenPageOptions};

import {wrapAsyncCommand} from '../lib/commands-utils';

const TILE_3D_DATA_SOURCE = 'tile3d:buildings';
type Tile3dState = 'loading' | 'loaded' | 'ready' | null;

interface CheckTileStateOption {
    timeout: number;
}

async function checkTileState(
    this: WebdriverIO.Browser,
    tileUrl: string,
    expectedTile3dStateState: Tile3dState,
    {timeout = 500}: Partial<CheckTileStateOption> = {}
): Promise<void> {
    let tile3dState = await getTile3dState(this, tileUrl);

    try {
        await this.waitUntil(
            async () => {
                tile3dState = await getTile3dState(this, tileUrl);
                return expectedTile3dStateState === tile3dState;
            },
            timeout,
            'TIMEOUT'
        );
    } catch (e) {
        if (e.message.includes('TIMEOUT')) {
            throw new Error(
                `Ожидалось, что состояние загрузки 3D макета ${tileUrl} будет ${expectedTile3dStateState}, а оказалось ${tile3dState}`
            );
        }
        throw e;
    }
}

async function getTile3dState(browser: WebdriverIO.Browser, tileUrl: string): Promise<Tile3dState> {
    const tileState = await browser.execute(
        (tile3dDataSource, tileUrl) => {
            return window.yandex_map?.getTile3dState(tile3dDataSource, tileUrl);
        },
        TILE_3D_DATA_SOURCE,
        tileUrl
    );

    return tileState;
}

export default wrapAsyncCommand(checkTileState);

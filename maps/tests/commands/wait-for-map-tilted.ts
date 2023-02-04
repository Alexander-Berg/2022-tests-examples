import {wrapAsyncCommand} from '../lib/commands-utils';

async function waitForMapTilted(this: WebdriverIO.Browser, ms = this.options.waitforTimeout, reverse = false) {
    const TIMEOUT_MSG = 'TIMEOUT';
    const ERROR_MSG = `map still ${reverse ? '' : 'not '}tilted after ${ms}ms`;
    const initialTilt = await getMapTilt(this);

    if (reverse === Boolean(initialTilt)) {
        await this.waitUntil(
            async () => {
                const tilt = await getMapTilt(this);
                return reverse !== Boolean(tilt);
            },
            ms,
            ERROR_MSG
        );
    } else {
        try {
            await this.waitUntil(
                async () => {
                    const tilt = await getMapTilt(this);
                    return reverse === Boolean(tilt);
                },
                ms,
                TIMEOUT_MSG
            );
            throw new Error(ERROR_MSG);
        } catch (error) {
            if (error.message !== TIMEOUT_MSG) {
                throw new Error(error);
            }
        }
    }
}

async function getMapTilt(browser: WebdriverIO.Browser): Promise<number> {
    const mapTilt = await browser.execute(() => {
        return window.yandex_map._camera.tilt;
    });

    return mapTilt;
}

export default wrapAsyncCommand(waitForMapTilted);

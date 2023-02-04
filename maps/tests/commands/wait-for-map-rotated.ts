import {wrapAsyncCommand} from '../lib/commands-utils';

async function waitForMapRotated(this: WebdriverIO.Browser, ms = this.options.waitforTimeout, reverse = false) {
    const TIMEOUT_MSG = 'TIMEOUT';
    const ERROR_MSG = `map still ${reverse ? '' : 'not '}rotated after ${ms}ms`;
    const initialAzimuth = await getMapAzimuth(this);

    if (reverse === Boolean(initialAzimuth)) {
        await this.waitUntil(
            async () => {
                const azimuth = await getMapAzimuth(this);
                return reverse !== Boolean(azimuth);
            },
            ms,
            ERROR_MSG
        );
    } else {
        try {
            await this.waitUntil(
                async () => {
                    const azimuth = await getMapAzimuth(this);
                    return reverse === Boolean(azimuth);
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

async function getMapAzimuth(browser: WebdriverIO.Browser): Promise<number> {
    const mapAzimuth = await browser.execute(() => {
        return window.yandex_map._camera.azimuth;
    });

    return mapAzimuth;
}

export default wrapAsyncCommand(waitForMapRotated);

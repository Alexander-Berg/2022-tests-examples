import {wrapAsyncCommand} from '../lib/commands-utils';

async function waitForLoadedAdvert(this: WebdriverIO.Browser): Promise<void> {
    await this.setTimeout({script: 6000});

    const advertLoaded = await this.executeAsync(waitForAdvertScript);

    if (!advertLoaded) {
        throw new Error('Реклама не загрузились');
    }
}

/* eslint-disable */
function waitForAdvertScript(done: (success: boolean) => void) {
    if (window.yaContextCb) {
        var timeout = setTimeout(function () {
            done(false);
        }, 5000);

        function onAdvertManagerLoad() {
            clearTimeout(timeout);
            done(true);
        }

        window.yaContextCb.push(onAdvertManagerLoad);
    } else {
        done(true);
    }
}
/* eslint-enable */

export default wrapAsyncCommand(waitForLoadedAdvert);

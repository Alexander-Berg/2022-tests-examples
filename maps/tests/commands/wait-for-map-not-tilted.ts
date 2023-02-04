import {wrapAsyncCommand} from '../lib/commands-utils';

async function waitForMapNotTilted(this: WebdriverIO.Browser, ms = this.options.waitforTimeout) {
    await this.waitForMapTilted(ms, true);
}

export default wrapAsyncCommand(waitForMapNotTilted);

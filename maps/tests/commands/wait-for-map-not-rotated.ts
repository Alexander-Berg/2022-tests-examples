import {wrapAsyncCommand} from '../lib/commands-utils';

async function waitForMapNotRotated(this: WebdriverIO.Browser, ms = this.options.waitforTimeout) {
    await this.waitForMapRotated(ms, true);
}

export default wrapAsyncCommand(waitForMapNotRotated);

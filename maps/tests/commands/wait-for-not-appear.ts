import {wrapAsyncCommand} from '../lib/commands-utils';

async function waitForNotAppear(this: WebdriverIO.Browser, selector: string, milliseconds?: number) {
    try {
        await this.waitForVisible(selector, milliseconds || 5000);
        throw new Error(`Element "${selector}" mustn't have appeared`);
    } catch (e) {
        if (!e.message.includes(`element ("${selector}") still not visible`)) {
            throw new Error(e);
        }
        await this.waitForHidden(selector);
    }
}

export default wrapAsyncCommand(waitForNotAppear);

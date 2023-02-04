import {wrapAsyncCommand} from '../lib/commands-utils';

async function waitAndHover(this: WebdriverIO.Browser, selector: string, x?: number, y?: number): Promise<void> {
    const element = await this.$(selector);
    await element.waitForDisplayed();
    await element.moveTo({xOffset: x, yOffset: y});
}

export default wrapAsyncCommand(waitAndHover);

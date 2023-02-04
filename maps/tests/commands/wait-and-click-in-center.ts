import cssSelectors from '../common/css-selectors';
import {wrapAsyncCommand} from '../lib/commands-utils';

interface ClickOptions {
    rightClick?: boolean;
    middleClick?: boolean;
    simulateClick?: boolean;
    doubleClick?: boolean;
}

async function waitAndClickInCenter(
    this: WebdriverIO.Browser,
    selector: string = cssSelectors.appReady,
    options: ClickOptions = {}
): Promise<void> {
    const element = await this.$(selector);
    await this.waitForVisible(selector);
    const {width, height} = await element.getSize();
    const x = Math.round(width / 2);
    const y = Math.round(height / 2);
    if (options.rightClick) {
        await element.click({button: 'right'});
    } else if (options.middleClick) {
        await element.click({button: 'middle'});
    } else if (options.simulateClick) {
        await this.simulateClick({selector, x, y, description: ''});
    } else if (options.doubleClick) {
        await this.simulateDoubleClick({selector, x, y, description: ''});
    } else {
        await element.click();
    }
}

export default wrapAsyncCommand(waitAndClickInCenter);
export {ClickOptions};

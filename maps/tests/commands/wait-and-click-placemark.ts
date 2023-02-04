import {wrapAsyncCommand} from '../lib/commands-utils';
import isMobileBrowser from '../lib/func/is-mobile-browser';

const WIDTH_DESKTOP_SIDEBAR = 380;
const HEIGHT_TOUCH_HEADER = 60;

async function waitAndClickPlacemark(this: WebdriverIO.Browser, selector: string): Promise<void> {
    const marginTop = isMobileBrowser(this) ? HEIGHT_TOUCH_HEADER : 0;
    const marginLeft = isMobileBrowser(this) ? 0 : WIDTH_DESKTOP_SIDEBAR;

    await this.waitForVisible(selector);
    const result = await this.$$(selector);
    const placemarks = await Promise.all(result.map((item) => item.getLocation()));
    const placemarksInViewport = placemarks.filter((value) => value && value.y > marginTop && value.x > marginLeft);

    if (placemarksInViewport.length === 0) {
        throw new Error(`В видимой области нет ${selector}`);
    }

    const {x, y} = placemarksInViewport[0];
    await this.simulateClick({x, y, description: ''});
}

export default wrapAsyncCommand(waitAndClickPlacemark);

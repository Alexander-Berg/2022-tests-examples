import {wrapAsyncCommand} from '../lib/commands-utils';
import isMobileBrowser from '../lib/func/is-mobile-browser';
import cssSelectors from '../common/css-selectors';

const PANEL_WIDTH = 384;
const WIDE_PANEL_WIDTH = 800;
const TOUCH_HEADER_HEIGHT = 68;
const TOUCH_FOOTER_HEIGHT = 100;

async function getMapCenter(
    this: WebdriverIO.Browser,
    params?: {excludeSidebarHeight?: boolean}
): Promise<[number, number]> {
    const isMobile = isMobileBrowser(this);
    const application = await this.getMeta('application');
    const isWide = await this.isVisible(cssSelectors.sidebar.wide);
    const defaultLeft = isMobile ? 0 : PANEL_WIDTH;
    const panelWidth = isWide ? WIDE_PANEL_WIDTH : defaultLeft;
    const [top, right, bottom, left] =
        application === 'map-widget'
            ? [50, 70, 72, 50]
            : isMobile
            ? await getMobileMapSize(this, params?.excludeSidebarHeight)
            : [70, 50, 50, 50 + panelWidth];

    const {width, height} = await this.getViewportSize();
    return [left / 2 + (width - right) / 2, top / 2 + (height - bottom) / 2];
}

async function getMobileMapSize(
    browser: WebdriverIO.Browser,
    excludeSidebarHeight?: boolean
): Promise<[number, number, number, number]> {
    const element = await browser.$(cssSelectors.sidebar.visibleCard);
    const footerHeight = excludeSidebarHeight ? await element.getSize('height') : TOUCH_FOOTER_HEIGHT;
    return [TOUCH_HEADER_HEIGHT, 50, footerHeight, 10];
}
export default wrapAsyncCommand(getMapCenter);

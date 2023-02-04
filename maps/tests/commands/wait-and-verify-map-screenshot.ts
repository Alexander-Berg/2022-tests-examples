import {wrapAsyncCommand} from '../lib/commands-utils';
import {ScreenshotOptions} from '../types/index';
import isMobileBrowser from '../lib/func/is-mobile-browser';

interface MapScreenshotOptions extends ScreenshotOptions {
    printPage?: boolean;
    isSerpHeader?: boolean;
}

async function waitAndVerifyMapScreenshot(this: WebdriverIO.Browser, name: string, options: MapScreenshotOptions = {}) {
    const application = await this.getMeta('application');

    const isMobile = isMobileBrowser(this);
    let screenshotRect = [70, 60, 160, 400];

    if (application === 'maps' && isMobile) {
        screenshotRect = [options.isSerpHeader ? 132 : 70, 64, 220, 64];
    } else if (application === 'metro' && isMobile) {
        screenshotRect = [60, 52, 75, 52];
    } else if (application === 'map-widget') {
        screenshotRect = [46, 40, 52, 40];
    } else if (options.printPage) {
        screenshotRect = [100, 70, 110, 10];
    }

    const [top, right, bottom, left] = screenshotRect;

    const style = `
            position: absolute;
            top: ${top}px;
            right: ${right}px;
            bottom: ${bottom}px;
            left: ${left}px;
        `;

    await this.execute(createMapCover, style, options);
    await this.waitForExist('div#map-cover');
    await this.waitAndVerifyScreenshot('div#map-cover', name, options);
    return this.execute(removeMapCover, options);
}

/* eslint-disable prefer-arrow-callback,no-undef,no-var */
function createMapCover(style: string, options: MapScreenshotOptions) {
    var element = window.document.createElement('div');
    element.setAttribute('id', 'map-cover');
    element.setAttribute('style', style);
    var container = options.printPage ? window.document.querySelector('.print-map-view') : window.document.body;
    container!.appendChild(element);
}

function removeMapCover(options: MapScreenshotOptions) {
    var element = window.document.getElementById('map-cover');
    var container = options.printPage ? window.document.querySelector('.print-map-view') : window.document.body;

    container!.removeChild(element!);
}
/* eslint-enable prefer-arrow-callback,no-undef,no-var */

export default wrapAsyncCommand(waitAndVerifyMapScreenshot);

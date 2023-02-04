import {wrapAsyncCommand} from '../lib/commands-utils';
import {ScreenshotOptions} from '../types/index';
import isMobileBrowser from '../lib/func/is-mobile-browser';

type PanoramaScreenshotOptions = ScreenshotOptions;

async function waitAndVerifyPanoramaScreenshot(
    this: WebdriverIO.Browser,
    name: string,
    options: PanoramaScreenshotOptions = {}
) {
    const [top, right, bottom, left] = isMobileBrowser(this) ? [60, 0, 120, 0] : [70, 100, 250, 100];
    const style = `
            position: absolute;
            top: ${top}px;
            right: ${right}px;
            bottom: ${bottom}px;
            left: ${left}px;
        `;

    await this.execute(createPanoramaCover, style);
    await this.waitForExist('div#panorama-cover');
    await this.waitAndVerifyScreenshot('div#panorama-cover', name, options);
    return this.execute(removePanoramaCover);
}

/* eslint-disable prefer-arrow-callback,no-undef,no-var */
function createPanoramaCover(style: string) {
    var element = window.document.createElement('div');
    element.setAttribute('id', 'panorama-cover');
    element.setAttribute('style', style);
    var container = window.document.body;
    container.appendChild(element);
}

function removePanoramaCover() {
    var element = window.document.getElementById('panorama-cover');
    var container = window.document.body;

    container.removeChild(element!);
}
/* eslint-enable prefer-arrow-callback,no-undef,no-var */

export default wrapAsyncCommand(waitAndVerifyPanoramaScreenshot);

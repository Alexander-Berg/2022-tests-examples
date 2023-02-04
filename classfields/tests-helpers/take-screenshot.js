/* global page */

import wait from './wait';
import { reportGateHistory } from './gate-history';

const { IS_DEBUG } = process.env;

/**
 * Обертка над стандартной функцией снятия скриншотов в puppeteer
 * https://github.com/puppeteer/puppeteer/blob/master/docs/api.md#pagescreenshotoptions
 * в дефолтном поведении перед снятием скриншота уводит курсор в левый верхний угол viewPort
 *
 * @param {Object} opts
 * @param {Boolean} opts.keepCursor - флаг, отключающий увод курсора перед снятием скриншота
 * @returns {Promise<void>}
 */

export default async(opts = {}) => {
    const { keepCursor, fullPageByClip, ...screenShotOpts } = opts;

    await reportGateHistory();

    if (! keepCursor) {
        await page.mouse.move(0, 0);
    }

    // fullPage неправильно работает если используются vh юниты
    // https://github.com/puppeteer/puppeteer/issues/703#issuecomment-366041479
    if (fullPageByClip) {
        const bodyHandle = await page.$('body');
        const { width, height } = await bodyHandle.boundingBox();

        screenShotOpts.clip = {
            x: 0,
            y: 0,
            width,
            height
        };
    }

    if (IS_DEBUG) {
        return wait(10005000);
    }

    await page.evaluateHandle('document.fonts.ready');

    return page.screenshot(screenShotOpts);
};

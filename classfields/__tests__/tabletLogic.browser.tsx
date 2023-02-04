import React from 'react';
import { render } from 'jest-puppeteer-react';
import { advanceTo } from 'jest-date-mock';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import * as stores from './store';
import { WIDTHS, viewports, selectors } from './constants';
import { Component } from './component';

advanceTo(new Date('2020-12-31 23:59:59'));

describe('MainMenu', () => {
    describe('Логика закрытия открытия и закрытия на планшете', () => {
        const renderOptions = { viewport: viewports[WIDTHS.NARROW], hasTouch: true };

        it('Всплывающее меню открывается и закрывается по клику по вкладке', async () => {
            await render(<Component store={stores.tabletStore} />, renderOptions);

            await page.addStyleTag({ content: 'body{padding: 0}' });

            await page.click(selectors.tabSelectorFactory(1));

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(selectors.tabSelectorFactory(1));

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Переключаются вкладки по клику', async () => {
            await render(<Component store={stores.tabletStore} />, renderOptions);

            await page.addStyleTag({ content: 'body{padding: 0}' });

            await page.click(selectors.tabSelectorFactory(1));

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(selectors.tabSelectorFactory(2));

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Всплывающее меню закрывается при клике вне меню', async () => {
            await render(<Component store={stores.tabletStore} />, renderOptions);

            await page.addStyleTag({ content: 'body{padding: 0}' });

            await page.click(selectors.tabSelectorFactory(1));

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.mouse.click(0, 0);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Всплывающее меню закрывается при попытке скрола', async () => {
            await render(<Component store={stores.tabletStore} />, renderOptions);

            await page.addStyleTag({ content: 'body{padding: 0}' });

            await page.click(selectors.tabSelectorFactory(1));

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.evaluate(() => {
                const touchstart = new Touch({
                    identifier: Date.now(),
                    target: window.document.body,
                    pageX: 210,
                    pageY: 148,
                    screenX: 210,
                    screenY: 148,
                    clientX: 210,
                    clientY: 148,
                });

                const touchmove = new Touch({
                    identifier: Date.now(),
                    target: window.document.body,
                    pageX: 10,
                    pageY: 148,
                    screenX: 10,
                    screenY: 148,
                    clientX: 10,
                    clientY: 148,
                });

                const touchMoveEvent = new TouchEvent('touchmove', {
                    cancelable: true,
                    bubbles: true,
                    touches: [touchstart, touchmove],
                    changedTouches: [touchstart, touchmove],
                });

                window.document.body.dispatchEvent(touchMoveEvent);
            });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});

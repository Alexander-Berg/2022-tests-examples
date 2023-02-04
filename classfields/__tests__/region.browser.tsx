import React from 'react';
import { render } from 'jest-puppeteer-react';
import { advanceTo } from 'jest-date-mock';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import * as stores from './store';
import { WIDTHS, translations, viewports, selectors } from './constants';
import { Component } from './component';

advanceTo(new Date('2020-12-31 23:59:59'));

describe('MainMenu', () => {
    [WIDTHS.NARROW, WIDTHS.MEDIUM, WIDTHS.WIDE].forEach((width) => {
        describe(`${translations[width]} экран`, () => {
            const renderOptions = { viewport: viewports[width] };
            const { regionTypes, getStoreWithDifferentRegions } = stores;

            describe(`${regionTypes.REGION}`, () => {
                const storeWithSpecificRegion = getStoreWithDifferentRegions(regionTypes.REGION);

                it('Базовое со всеми пунктами', async () => {
                    await render(<Component store={storeWithSpecificRegion} />, renderOptions);

                    await page.addStyleTag({ content: 'body{padding: 0}' });

                    expect(await takeScreenshot()).toMatchImageSnapshot();
                });

                it('Пункты вкладки "Купить"', async () => {
                    await render(<Component store={storeWithSpecificRegion} />, renderOptions);

                    await page.addStyleTag({ content: 'body{padding: 0}' });

                    await page.hover(selectors.tabSelectorFactory(1));

                    expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
                });

                it('Пункты вкладки "Снять"', async () => {
                    await render(<Component store={storeWithSpecificRegion} />, renderOptions);

                    await page.addStyleTag({ content: 'body{padding: 0}' });

                    await page.hover(selectors.tabSelectorFactory(2));

                    expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
                });

                it('Пункты вкладки "Новостройки"', async () => {
                    await render(<Component store={storeWithSpecificRegion} />, renderOptions);

                    await page.addStyleTag({ content: 'body{padding: 0}' });

                    await page.hover(selectors.tabSelectorFactory(3));

                    expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
                });

                it('Пункты вкладки "Коммерческая"', async () => {
                    await render(<Component store={storeWithSpecificRegion} />, renderOptions);

                    await page.addStyleTag({ content: 'body{padding: 0}' });

                    await page.hover(selectors.tabSelectorFactory(4));

                    expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
                });

                it('Пункты вкладки "Ипотека"', async () => {
                    await render(<Component store={storeWithSpecificRegion} />, renderOptions);

                    await page.addStyleTag({ content: 'body{padding: 0}' });

                    await page.hover(selectors.tabSelectorFactory(5));

                    expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
                });

                it('Пункты вкладки "Профессионалам"', async () => {
                    await render(<Component store={storeWithSpecificRegion} />, renderOptions);

                    await page.addStyleTag({ content: 'body{padding: 0}' });

                    await page.hover(selectors.tabSelectorFactory(6));

                    expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
                });
            });
        });
    });
});

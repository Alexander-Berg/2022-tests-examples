import React from 'react';
import { render as _render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { PriceStatisticsType } from 'realty-core/types/priceStatistics';

import { CardSitePriceStatistics } from '../';

import {
    priceStatisticsDown,
    priceStatisticsUp,
    priceStatisticsNeutral,
    priceStatistics,
    priceStatisticsWithBlankData,
} from './mocks';

const render = async (priceStatistics: PriceStatisticsType) => {
    await _render(
        <AppProvider
            fakeTimers={{
                now: new Date('2020-06-02T03:00:00.111Z').getTime(),
            }}
        >
            <CardSitePriceStatistics pageType="" pageParams={{}} priceStatistics={priceStatistics} />
        </AppProvider>,
        { viewport: { width: 360, height: 600 } }
    );
};

describe('CardSitePriceStatistics', () => {
    it('Баннер - цена падает', async () => {
        await render(priceStatisticsDown);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Баннер - цена растет', async () => {
        await render(priceStatisticsUp);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Баннер - цена не меняется', async () => {
        await render(priceStatisticsNeutral);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Модалка - типы в правильном порядке', async () => {
        await render(priceStatistics);

        await page.click('.Button');
        await page.mouse.move(0, 0);

        await page.waitForSelector('.recharts-responsive-container');
        // анимация графика
        await customPage.tick(1000);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Модалка - только один тип доступен', async () => {
        await render(priceStatisticsDown);

        await page.click('.Button');
        await page.mouse.move(0, 0);

        await page.waitForSelector('.recharts-responsive-container');
        // анимация графика
        await customPage.tick(1000);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Модалка - ховер на график', async () => {
        await render(priceStatistics);

        await page.click('.Button');

        await page.waitForSelector('.recharts-responsive-container');
        // анимация графика
        await customPage.tick(1000);

        await page.mouse.move(150, 500);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('Модалка - ховер на график с пропущенными данными', async () => {
        await render(priceStatisticsWithBlankData);

        await page.click('.Button');

        await page.waitForSelector('.recharts-responsive-container');
        // анимация графика
        await customPage.tick(1000);

        await page.mouse.move(150, 500);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });
});

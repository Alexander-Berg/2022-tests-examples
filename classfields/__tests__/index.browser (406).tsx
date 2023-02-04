import React from 'react';
import { render as _render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { CardSitePriceStatistics } from '../';

import { priceStatisticsSingle, priceStatistics, priceStatisticsWithBlankData } from './mocks';

const render = async (component: React.ReactElement) => {
    await _render(<AppProvider>{component}</AppProvider>, {
        viewport: { width: 1400, height: 700 },
    });
};

describe('CardSitePriceStatistics', () => {
    it('Только один тип доступен', async () => {
        await render(<CardSitePriceStatistics priceStatistics={priceStatisticsSingle} />);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Ховер на график близко к левому краю', async () => {
        await render(<CardSitePriceStatistics priceStatistics={priceStatistics} />);

        await page.mouse.move(150, 300);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('Ховер на график близко к правому краю', async () => {
        await render(<CardSitePriceStatistics priceStatistics={priceStatistics} />);

        await page.mouse.move(1250, 300);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('Ховер на график посередине', async () => {
        await render(<CardSitePriceStatistics priceStatistics={priceStatistics} />);

        await page.mouse.move(600, 300);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('Ховер на график c пропущенными данными', async () => {
        await render(<CardSitePriceStatistics priceStatistics={priceStatisticsWithBlankData} />);

        await page.mouse.move(600, 300);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });
});

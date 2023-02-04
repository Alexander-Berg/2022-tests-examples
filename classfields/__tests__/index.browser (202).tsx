import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import chartStyles from 'realty-core/view/react/common/components/AvgPriceChartReport/styles.module.css';

import { EGRNPaidReportAvgPriceChartBlock, IProps } from '../';

const priceRange = {
    max: 1000000,
    min: 200000,
    median: 500000,
    percentile25: 350000,
    percentile75: 900000,
    currentOfferPrice: 650000,
    offersCount: 10,
};

const renderComponent = (props: IProps) =>
    render(<EGRNPaidReportAvgPriceChartBlock {...props} />, { viewport: { width: 360, height: 600 } });

describe('EGRNPaidReportAvgPriceChartBlock', () => {
    it('рендерится', async () => {
        await renderComponent({ priceRange });
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится с открытой подсказкой по медианной цене', async () => {
        await renderComponent({ priceRange });

        await page.click(`.${chartStyles.hintAnchor}`);

        await page.waitFor(200);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится без цены текущего оффера', async () => {
        await renderComponent({
            priceRange: {
                max: 1000000,
                min: 200000,
                median: 500000,
                percentile25: 350000,
                percentile75: 900000,
                offersCount: 10,
            },
        });
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});

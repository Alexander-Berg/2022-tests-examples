import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { EGRNPaidReportTaxBlock } from '../index';

describe('EGRNPaidReportTaxBlock', () => {
    it('рендер блока про налог при наличии налога', async() => {
        await render(
            <EGRNPaidReportTaxBlock taxAmount={10000} />,
            { viewport: { width: 940, height: 250 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендер блока про налог при нулевом налоге', async() => {
        await render(
            <EGRNPaidReportTaxBlock taxAmount={0} />,
            { viewport: { width: 940, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});

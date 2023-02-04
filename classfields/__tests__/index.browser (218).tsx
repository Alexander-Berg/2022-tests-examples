import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { EGRNPaidReportTaxBlock } from '../';

const OPTIONS = { viewport: { width: 350, height: 650 } };

describe('EGRNPaidReportTaxBlock', () => {
    it(`рендерится`, async () => {
        await render(<EGRNPaidReportTaxBlock taxAmount={500000.5} />, OPTIONS);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
    it(`рендерится с нулевым налогом`, async () => {
        await render(<EGRNPaidReportTaxBlock taxAmount={0} />, OPTIONS);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});

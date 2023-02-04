import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { EGRNPaidReportErrorScreen } from '../';

const OPTIONS = { viewport: { width: 350, height: 600 } };

describe('EGRNPaidReportErrorScreen', () => {
    it('рендерится', async () => {
        await render(<EGRNPaidReportErrorScreen />, OPTIONS);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});

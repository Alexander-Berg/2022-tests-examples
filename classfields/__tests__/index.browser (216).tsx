import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { EGRNPaidReportReminderBlocks } from '../';

describe('EGRNPaidReportReminderBlocks', () => {
    it('рендерится в свёрнутом виде', async () => {
        await render(<EGRNPaidReportReminderBlocks />, {
            viewport: { width: 350, height: 1400 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится в развёрнутом виде', async () => {
        await render(<EGRNPaidReportReminderBlocks />, {
            viewport: { width: 350, height: 4600 },
        });

        await page.click('button');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});

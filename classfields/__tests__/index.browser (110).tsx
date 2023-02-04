import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { EGRNPaidReportVoteRadioGroup } from '../';

describe('EGRNPaidReportVoteRadioGroup', () => {
    it('рендерится с выбранной реакцией', async () => {
        await render(<EGRNPaidReportVoteRadioGroup value="negative" onChange={() => void 0} />, {
            viewport: { width: 300, height: 130 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится без выбранной реакции', async () => {
        await render(<EGRNPaidReportVoteRadioGroup value={null} onChange={() => void 0} />, {
            viewport: { width: 300, height: 130 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится с ховером по одной из реакций', async () => {
        await render(<EGRNPaidReportVoteRadioGroup value={null} onChange={() => void 0} />, {
            viewport: { width: 300, height: 130 },
        });

        await page.hover('button');
        await page.waitFor(200);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });
});

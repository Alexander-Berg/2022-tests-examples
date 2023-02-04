import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { OfferEGRNReportFreeBrief } from '../';

describe('OfferEGRNReportFreeBrief', () => {
    it('рендерится', async () => {
        await render(
            <AppProvider>
                <OfferEGRNReportFreeBrief />
            </AppProvider>,
            { viewport: { width: 850, height: 200 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});

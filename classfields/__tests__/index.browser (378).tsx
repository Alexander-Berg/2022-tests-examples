import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { OfferEGRNReportPurchasedTileComponent } from '../';

describe('OfferEGRNReportPurchasedTile', () => {
    it('рендерится', async () => {
        await render(
            <OfferEGRNReportPurchasedTileComponent link={() => ''} reportDate="2020-07-27" paidReportId="1" />,
            { viewport: { width: 1100, height: 250 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});

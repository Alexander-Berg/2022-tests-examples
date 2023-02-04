import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { ICardPaidReport } from 'realty-core/view/react/common/types/egrnPaidReport';

import { OfferCardEGRNReportPurchasedTileComponent } from '../';

describe('OfferCardEGRNReportPurchasedTile', () => {
    it('рендерится на узком экране', async () => {
        await render(
            <OfferCardEGRNReportPurchasedTileComponent
                paidReport={
                    {
                        paidReportId: '123',
                        reportDate: '2020-10-10',
                    } as ICardPaidReport
                }
                link={() => ''}
            />,
            {
                viewport: { width: 320, height: 130 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится на широком экране', async () => {
        await render(
            <OfferCardEGRNReportPurchasedTileComponent
                paidReport={
                    {
                        paidReportId: '123',
                        reportDate: '2020-10-10',
                    } as ICardPaidReport
                }
                link={() => ''}
            />,
            {
                viewport: { width: 450, height: 130 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});

import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { IOtherOffers } from 'realty-core/view/react/common/types/egrnPaidReport';

import { EGRNPaidReportOffersInBuildingBlock } from '../';

const otherOffers: IOtherOffers = {
    firstOfferDay: '2010-10-10',
    daysInExposition: 329,
    totalOffers: 456,
    studios: 5,
    rooms1: 56,
    rooms2: 66,
    rooms3: 234,
    rooms4AndMore: 45,
};

describe('EGRNPaidReportOffersInBuildingBlock', () => {
    it('рендерится', async () => {
        await render(
            <EGRNPaidReportOffersInBuildingBlock
                offersArchiveLink="https://realty.yandex.ru"
                otherOffers={otherOffers}
            />,
            { viewport: { width: 350, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится на узком экране с переносом ячеек', async () => {
        await render(
            <EGRNPaidReportOffersInBuildingBlock
                offersArchiveLink="https://realty.yandex.ru"
                otherOffers={otherOffers}
            />,
            { viewport: { width: 330, height: 600 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится на более широком экране', async () => {
        await render(
            <EGRNPaidReportOffersInBuildingBlock
                offersArchiveLink="https://realty.yandex.ru"
                otherOffers={otherOffers}
            />,
            { viewport: { width: 550, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});

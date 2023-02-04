import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { IOfferCard, OfferType } from 'realty-core/types/offerCard';

import { OfferEGRNReportFreeExtended } from '../';

import { offer } from './excerptMock';

const spoilerButtonSelector = 'button[class^="Spoiler__button"]';

const OPTIONS = { viewport: { width: 850, height: 800 } };

describe('OfferEGRNReportFreeExtended', () => {
    it('рендерится', async () => {
        await render(<OfferEGRNReportFreeExtended offer={offer} />, OPTIONS);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится без предыдущих владельцев для арендного оффера', async () => {
        await render(<OfferEGRNReportFreeExtended offer={{ ...offer, offerType: OfferType.RENT }} />, OPTIONS);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится с раскрытым спойлером предыдущих владельцев', async () => {
        await render(<OfferEGRNReportFreeExtended offer={offer} />, OPTIONS);

        await page.click(spoilerButtonSelector);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится без прав собственности', async () => {
        const modifiedOffer = {
            ...offer,
            excerptReport: {
                ...offer.excerptReport,
                flatReport: {
                    ...offer.excerptReport?.flatReport,
                    currentRights: [],
                    previousRights: [],
                },
            },
        } as IOfferCard;

        await render(<OfferEGRNReportFreeExtended offer={modifiedOffer} />, OPTIONS);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});

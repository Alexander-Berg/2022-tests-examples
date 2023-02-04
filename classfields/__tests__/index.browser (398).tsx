import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { OfferCardTextDescription } from '..';

import { offer, offerWithShortDescription } from './mocks';

describe('OfferCardTextDescription', function () {
    it('Рисует свернутый блок', async () => {
        await render(<OfferCardTextDescription offer={offer} />, { viewport: { width: 1024, height: 500 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует развернутый блок', async () => {
        await render(<OfferCardTextDescription offer={offer} />, { viewport: { width: 1024, height: 500 } });

        await page.click('.Link');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует блок с коротким текстом', async () => {
        await render(<OfferCardTextDescription offer={offerWithShortDescription} />, {
            viewport: { width: 1024, height: 500 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});

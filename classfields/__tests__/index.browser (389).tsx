import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { OfferCardPriceInput } from '../index';

import { offer } from './mocks';

describe('OfferCardPriceInput', () => {
    it('Базовая отрисовка', async () => {
        await render(<OfferCardPriceInput price={offer.price} onEditFinished={() => null} savePrice={() => null} />, {
            viewport: { width: 400, height: 100 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});

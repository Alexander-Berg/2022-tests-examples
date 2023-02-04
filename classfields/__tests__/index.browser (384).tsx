import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { OfferCardHighlights } from '../index';

import { newbuildingOffer, secondaryOffer, garageOffer, commercialOffer } from './mocks';

describe('OfferCardHighlights', function () {
    it('Отрисовка для новостройки', async () => {
        await render(<OfferCardHighlights offer={newbuildingOffer} />, {
            viewport: { width: 800, height: 150 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка для вторички', async () => {
        await render(<OfferCardHighlights offer={secondaryOffer} />, {
            viewport: { width: 800, height: 150 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка для гаража', async () => {
        await render(<OfferCardHighlights offer={garageOffer} />, {
            viewport: { width: 800, height: 150 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка для коммерческой недвижимости', async () => {
        await render(<OfferCardHighlights offer={commercialOffer} />, {
            viewport: { width: 800, height: 150 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});

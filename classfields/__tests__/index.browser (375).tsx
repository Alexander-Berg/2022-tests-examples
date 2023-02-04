import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { IOfferCard } from 'realty-core/types/offerCard';
import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { OfferCardDetailsFeaturesContainer } from '../container';

import { houseOffer, newbuildingOffer, rentOfferWithAllFeatures, state } from './mocks';

const Component: React.FC<{ offer: IOfferCard }> = ({ offer }) => (
    <AppProvider initialState={state}>
        <OfferCardDetailsFeaturesContainer offer={offer} />
    </AppProvider>
);

describe('OfferCardDetailsFeaturesContainer', () => {
    it('Отрисовка с двумя удобствами', async () => {
        await render(<Component offer={newbuildingOffer} />, {
            viewport: { width: 800, height: 400 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка со многими удобствами', async () => {
        await render(<Component offer={houseOffer} />, {
            viewport: { width: 800, height: 400 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('После клика на "ещё удобства" должны показаться все удобства', async () => {
        await render(<Component offer={houseOffer} />, {
            viewport: { width: 800, height: 450 },
        });

        await page.click('[role=button]');
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка с удобствами аренды', async () => {
        await render(<Component offer={rentOfferWithAllFeatures} />, {
            viewport: { width: 800, height: 200 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});

import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { IOfferCard } from 'realty-core/types/offerCard';
import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { OfferCardBuildingSubscription } from '../';

import { newbuildingOffer } from '../../__tests__/stubs/offer';

const Component: React.FC<{ offer: IOfferCard }> = ({ offer }) => (
    <AppProvider>
        <OfferCardBuildingSubscription offer={offer} />
    </AppProvider>
);

describe('OfferCardBuildingSubscription', () => {
    it('Базовый вид', async () => {
        await render(<Component offer={newbuildingOffer} />, {
            viewport: { width: 800, height: 300 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});

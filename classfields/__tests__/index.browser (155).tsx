import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { IOfferCard } from 'realty-core/types/offerCard';

import { OfferAddToFavoritesContainer } from '../container';
import { IRequiredStore } from '../types';

import { stateWithOfferInFavorites, state, offer, getStats } from './mocks';

const Component: React.FC<{
    offer: IOfferCard;
    state: IRequiredStore;
}> = ({ offer, state }) => (
    <AppProvider initialState={state}>
        <OfferAddToFavoritesContainer
            offer={offer}
            eventPlace="blank"
            favoritedFrom="blank"
            getOfferAddFavoritesStats={getStats}
            getOfferRemoveFavoritesStats={getStats}
        />
    </AppProvider>
);

const renderComponent = (element: JSX.Element) => render(element, { viewport: { width: 80, height: 80 } });

describe('OfferAddToFavorites', () => {
    it('рендерится в дефолтном состоянии', async () => {
        await renderComponent(<Component offer={offer} state={state} />);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится в случае, когда оффер добавлен в избранное', async () => {
        await renderComponent(<Component offer={offer} state={stateWithOfferInFavorites} />);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});

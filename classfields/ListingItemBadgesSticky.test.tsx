import React from 'react';
import { render } from '@testing-library/react';
import { Provider } from 'react-redux';

import { OfferStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import offerMock from 'auto-core/react/dataDomain/listing/mocks/listingOffer.cars.mock';

import ListingItemBadgesSticky from './ListingItemBadgesSticky';

import '@testing-library/jest-dom';

const ContextProvider = createContextProvider(contextMock);

const state = {
    config: {},
};

const store = mockStore(state);

it('не должен отрендерить бейджи если оффер не активен', () => {
    render(
        <ContextProvider>
            <Provider store={ store }>
                <ListingItemBadgesSticky offer={{
                    ...offerMock,
                    status: OfferStatus.INACTIVE,
                }} hasPanorama={ false }/>
            </Provider>
        </ContextProvider>,
    );

    const component = document.querySelector('.ListingItemTagsDesktop');

    expect(component).toBeNull();
});

jest.mock('./hooks/useMarketplaceFilterFab', () => () => ({
    closeFiltersPopup: () => {},
    isFabVisible: true,
    isFiltersVisible: false,
    mmmInfo: [],
    onFiltersSubmit: () => {},
    searchTagDictionary: { data: [] },
    showFiltersPopup: () => {},
}));

import React from 'react';
import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import { PageType } from 'auto-core/lib/getListingPageType';

import MarketplaceFilterFab from './MarketplaceFilterFab';

const ContextProvider = createContextProvider(contextMock);

it('отправит метрику по клику на фаб', () => {
    render(
        <ContextProvider>
            <MarketplaceFilterFab page={ PageType.MARKETPLACE_MODEL }/>
        </ContextProvider>,
    );
    const fab = document.querySelector('.Button') as Element;
    userEvent.click(fab);
    expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ PageType.MARKETPLACE_MODEL, 'filters-fab', 'click' ]);
});

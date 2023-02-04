import React from 'react';
import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import applyUseSelectorMock from 'autoru-frontend/jest/unit/applyUseSelectorMock';

import state from './mocks/state.mock';
import MarketplaceOffersBlock from './MarketplaceOffersBlock';

const ContextProvider = createContextProvider({ ...contextMock, pageParams: {
    section: 'new',
    category: 'cars',
    catalog_filter: [ {
        mark: 'renault',
        model: 'renault',
    } ],
} });

it('отправит метрику по клику на Показать все предложения', () => {
    const { mockUseSelector } = applyUseSelectorMock();
    mockUseSelector(state);
    render(
        <ContextProvider>
            <MarketplaceOffersBlock
                title="Kalina"
            />
        </ContextProvider>,
    );
    const button = document.querySelector('.MarketplaceOffersBlock__button') as Element;
    userEvent.click(button);
    expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'marketplace-model', 'blocks', 'offers', 'listing-link', 'click' ]);
});

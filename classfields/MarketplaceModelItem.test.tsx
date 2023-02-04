jest.mock('www-mobile/react/hooks/useMarketplaceCompare');

import React from 'react';
import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import useMarketplaceCompare from 'www-mobile/react/hooks/useMarketplaceCompare';

import state from '../mocks/state.mock';

import MarketplaceModelItem from './MarketplaceModelItem';

const Context = createContextProvider(contextMock);
const useMarketplaceCompareMock = useMarketplaceCompare as jest.MockedFunction<typeof useMarketplaceCompare>;

const hookDefaultReturnValue = {
    isComparePending: false,
    isOfferInCompare: false,
    addOfferToCompare: () => {},
};

it('отправит метрику по клику на оффер', () => {
    renderComponent();
    const model = document.querySelector('.MarketplaceModelItem__link') as Element;
    userEvent.click(model);
    expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'marketplace-index', 'blocks', 'models', 'model', 'click' ]);
});

it('отправит метрику по клику на добавление в сравнение', () => {
    renderComponent();
    const compare = document.querySelector('.MarketplaceModelItem__compare') as Element;
    userEvent.click(compare);
    expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'marketplace-index', 'blocks', 'models', 'compare', 'add', 'click' ]);
});

it('отправит метрику по клику на переход в сравнение', () => {
    renderComponent({ ...hookDefaultReturnValue, isOfferInCompare: true });
    const compare = document.querySelector('.MarketplaceModelItem__compare') as Element;
    userEvent.click(compare);
    expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'marketplace-index', 'blocks', 'models', 'compare', 'show', 'click' ]);
});

function renderComponent(hookReturnValue = hookDefaultReturnValue) {
    useMarketplaceCompareMock.mockReturnValue(hookReturnValue);
    return render(
        <Context>
            <MarketplaceModelItem offer={ state.listing.data.offers[0] }/>
        </Context>,
    );
}

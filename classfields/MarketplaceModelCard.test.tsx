jest.mock('www-mobile/react/hooks/useMarketplaceCompare');

import userEvent from '@testing-library/user-event';
import React from 'react';
import { render } from '@testing-library/react';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import useMarketplaceCompare from 'www-mobile/react/hooks/useMarketplaceCompare';

import model from './mocks/model.mock';
import MarketplaceModelCard from './MarketplaceModelCard';

const ContextProvider = createContextProvider(contextMock);

const useMarketplaceCompareMock = useMarketplaceCompare as jest.MockedFunction<typeof useMarketplaceCompare>;

const hookDefaultReturnValue = {
    isComparePending: false,
    isOfferInCompare: false,
    addOfferToCompare: () => {},
};

it('отправит метрику при добавлении в сравнение', () => {
    renderComponent();
    const compare = document.querySelector('.MarketplaceModelCard__button') as Element;
    userEvent.click(compare);
    expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'marketplace-mark', 'blocks', 'models', 'compare', 'add', 'click' ]);
});

it('отправит метрику при переходе в сравнение', () => {
    renderComponent({ ...hookDefaultReturnValue, isOfferInCompare: true });
    const compare = document.querySelector('.MarketplaceModelCard__button') as Element;
    userEvent.click(compare);
    expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'marketplace-mark', 'blocks', 'models', 'compare', 'show', 'click' ]);
});

function renderComponent(hookReturn = hookDefaultReturnValue) {
    useMarketplaceCompareMock.mockReturnValue(hookReturn);
    return render(
        <ContextProvider>
            <MarketplaceModelCard model={ model }/>
        </ContextProvider>,
    );
}

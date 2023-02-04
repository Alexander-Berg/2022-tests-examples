jest.mock('www-mobile/react/components/PageMarketplaceModel/MarketplaceSameClassBlock/hooks/useMarketplaceSameClassBlock');

import React from 'react';
import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';

import useMarketplaceSameClassBlock
    from 'www-mobile/react/components/PageMarketplaceModel/MarketplaceSameClassBlock/hooks/useMarketplaceSameClassBlock';

import state from './mocks/state.mock';
import MarketplaceSameClassBlock from './MarketplaceSameClassBlock';

const ContextProvider = createContextProvider(contextMock);

const useMarketplaceSameClassBlockMock = useMarketplaceSameClassBlock as jest.MockedFunction<typeof useMarketplaceSameClassBlock>;

const hookReturnValue = {
    isRelatedGroupsPending: false,
    changePriceTo: () => {},
    priceRanges: [],
    currentPrice: 10000,
    groups: state.relatedGroups.data.offers,
    isModelsInCompare: false,
    isComparePending: false,
    addModelsToCompare: () => {},
};

it('отправит метрику по клику на оффер', () => {
    useMarketplaceSameClassBlockMock.mockReturnValue(hookReturnValue);
    renderComponent();
    const offer = document.querySelector('.MarketplaceSameClassBlock__offer:nth-child(1)') as Element;
    userEvent.click(offer);
    expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'marketplace-model', 'blocks', 'same-class', 'model', 'click' ]);
});

it('отправит метрику при добавлении в сравнение', () => {
    useMarketplaceSameClassBlockMock.mockReturnValue(hookReturnValue);
    renderComponent();
    const compareAll = document.querySelector('.MarketplaceSameClassBlock__button') as Element;
    userEvent.click(compareAll);
    expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'marketplace-model', 'blocks', 'same-class', 'compare-all', 'add', 'click' ]);
});

it('отправит метрику при переходе в сравнение', () => {
    useMarketplaceSameClassBlockMock.mockReturnValue({
        ...hookReturnValue, isModelsInCompare: true,
    });
    renderComponent();
    const compareAll = document.querySelector('.MarketplaceSameClassBlock__button') as Element;
    userEvent.click(compareAll);
    expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'marketplace-model', 'blocks', 'same-class', 'compare-all', 'show', 'click' ]);
});

function renderComponent() {
    return render(
        <ContextProvider>
            <MarketplaceSameClassBlock
                title="Kalina"
            />
        </ContextProvider>,
    );
}

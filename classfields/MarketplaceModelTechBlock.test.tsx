import React from 'react';
import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import applyUseSelectorMock from 'autoru-frontend/jest/unit/applyUseSelectorMock';

import catalogSubtreeMock from 'auto-core/react/dataDomain/catalogConfigurationsSubtree/mocks/subtree';

import MarketplaceModelTechBlock from './MarketplaceModelTechBlock';

const ContextProvider = createContextProvider(contextMock);

const state = {
    averageRating: {
        ratings: [ { name: 'total', value: 4.6 } ],
    },
    catalogConfigurationsSubtree: catalogSubtreeMock,
};

it('отправит метрику по клику на субтитл', () => {
    renderComponent();
    const subtitle = document.querySelector('.MarketplaceModelTechBlock__subtitle') as Element;
    userEvent.click(subtitle);
    expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'marketplace-model', 'blocks', 'tth', 'listing-link', 'click' ]);
});

it('отправит метрику по клику на кнопку Подробнее о модели', () => {
    renderComponent();
    const button = document.querySelector('.MarketplaceModelTechBlock__button') as Element;
    userEvent.click(button);
    expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'marketplace-model', 'blocks', 'tth', 'catalog-link', 'click' ]);
});

function renderComponent() {
    const { mockUseSelector } = applyUseSelectorMock();
    mockUseSelector(state);
    return render(
        <ContextProvider>
            <MarketplaceModelTechBlock
                title="Kalina"
                subtitle="361 предложение от 3 055 000 ₽"
            />
        </ContextProvider>,
    );
}

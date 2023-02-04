import React from 'react';
import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import mockStore from 'autoru-frontend/mocks/mockStore';
import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import applyUseSelectorMock from 'autoru-frontend/jest/unit/applyUseSelectorMock';

import state from './mocks/state.mock';
import MarketplaceModels from './MarketplaceModels';

const store = mockStore(state);

const Context = createContextProvider(contextMock);

it('отправит метрику по клику на Показать все модели', () => {
    const { mockUseDispatch, mockUseSelector } = applyUseSelectorMock();
    mockUseDispatch(store);
    mockUseSelector(state);
    render(
        <Context>
            <MarketplaceModels/>
        </Context>,
    );
    const button = document.querySelector('.MarketplaceModels__button') as Element;
    userEvent.click(button);
    expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'marketplace-index', 'blocks', 'models', 'listing-link', 'click' ]);
});

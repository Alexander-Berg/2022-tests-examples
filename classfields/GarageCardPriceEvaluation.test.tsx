import React from 'react';
import { render, screen } from '@testing-library/react';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import garageCardMock from 'auto-core/models/garageCard/mocks/mockChain';

import GarageCardPriceEvaluation from './GarageCardPriceEvaluation';

const Context = createContextProvider(contextMock);

const CARD = garageCardMock.withPricePredict().value();

it(`убедиться, что фильтр обмена в ссылке не потерялся`, () => {
    render(
        <Context>
            <GarageCardPriceEvaluation garageCard={ CARD }/>
        </Context>,
    );

    const link = screen.getByRole('link', { name: /к\sобъявлениям/i });

    expect(link.getAttribute('href')).toBe('link/listing/?exchange_group=POSSIBLE');
});

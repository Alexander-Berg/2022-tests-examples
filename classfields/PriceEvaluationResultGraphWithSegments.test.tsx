import React from 'react';
import { render } from '@testing-library/react';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';

import garageCardMock from 'auto-core/models/garageCard/mocks/mockChain';

import PriceEvaluationResultGraphWithSegments from './PriceEvaluationResultGraphWithSegments';

const data = garageCardMock.withPriceStats().value().price_stats?.price_distribution;

const Context = createContextProvider(contextMock);

it('должен отрендерить ссылку в сегменте', () => {
    const { getByText } = render(
        <Context>
            <PriceEvaluationResultGraphWithSegments
                flagPrice={ 1 }
                priceStatsData={ data }
                showLinks={ true }
            />
        </Context>,
    );
    const link = getByText(/339/);

    expect(link.getAttribute('href')).toMatchSnapshot();
});

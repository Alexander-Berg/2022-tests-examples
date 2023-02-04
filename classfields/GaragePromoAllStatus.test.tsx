import React from 'react';
import { render } from '@testing-library/react';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import GaragePromoAllStatus from './GaragePromoAllStatus';

const Context = createContextProvider(contextMock);

it('отправит метрику ошибки открытия Всех акций', () => {
    render(
        <Context>
            <GaragePromoAllStatus status="ERROR"/>
        </Context>,
    );

    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'promo_all', 'error' ]);
});

it('отправит метрику незагрузки Всех акций', () => {
    render(
        <Context>
            <GaragePromoAllStatus status="EMPTY"/>
        </Context>,
    );

    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'promo_all', 'empty' ]);
});

import React from 'react';
import { render } from '@testing-library/react';

import type { PartnerPromo } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/garage/garage_api_model';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import GaragePromoAll from './GaragePromoAll';

const Context = createContextProvider(contextMock);

const promos: Array<PartnerPromo> = [];
const pageParams = {};

describe('отправляет 2 метрики открытия Всех акций', () => {
    it('по дефолту', () => {
        render(
            <Context>
                <GaragePromoAll isGarageEmpty={ true } garagePromos={ promos } pageParams={ pageParams }/>
            </Context>,
        );

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(2);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'promo_all', 'open', 'user', 'no_auth' ]);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'promo_all', 'open', 'source', 'organic' ]);
    });

    it('указан сорс в строке', () => {
        render(
            <Context>
                <GaragePromoAll isGarageEmpty={ true } garagePromos={ promos } pageParams={{ source: 'dream_card' }}/>
            </Context>,
        );

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(2);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'promo_all', 'open', 'user', 'no_auth' ]);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'promo_all', 'open', 'source', 'dream_card' ]);
    });

    it('юзер авторизован', () => {
        render(
            <Context>
                <GaragePromoAll isAuth={ true } isGarageEmpty={ true } garagePromos={ promos } pageParams={ pageParams }/>
            </Context>,
        );

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(2);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'promo_all', 'open', 'user', 'auth' ]);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'promo_all', 'open', 'source', 'organic' ]);
    });
});

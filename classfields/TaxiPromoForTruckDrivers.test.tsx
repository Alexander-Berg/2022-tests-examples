/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import 'jest-enzyme';
import React from 'react';
import { Provider } from 'react-redux';
import { shallow, mount } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import mockStore from 'autoru-frontend/mocks/mockStore';

const Context = createContextProvider(contextMock);

import TaxiPromoForTruckDrivers from './TaxiPromoForTruckDrivers';

it('не должен отрисовать баннер, если пользователь скрыл его', async() => {
    const store = mockStore({
        cookies: {
            taxiPromoBeenViewed: 'close',
        },
    });

    const tree = shallow(
        <Provider store={ store }>
            <Context>
                <TaxiPromoForTruckDrivers/>
            </Context>
        </Provider>,
    ).dive().dive().dive();

    expect(tree).toBeEmptyRender();
});

it('должен отправить метрики при переходе с баннера', async() => {
    const tree = mount(
        <Provider store={ mockStore({ cookies: {} }) }>
            <Context>
                <TaxiPromoForTruckDrivers/>
            </Context>
        </Provider>,
    );

    tree.find('Button[children="Заполнить заявку"]').simulate('click');

    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'taxi-driver-offer', 'click' ]);
});

it('должен отправить метрики при скрытии баннера', async() => {
    const tree = shallow(
        <Provider store={ mockStore({ cookies: {} }) }>
            <Context>
                <TaxiPromoForTruckDrivers/>
            </Context>
        </Provider>,
    ).dive().dive().dive();

    tree.find('Button[children="Скрыть"]').simulate('click');

    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'taxi-driver-offer', 'close' ]);
});

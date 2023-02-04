/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { Provider } from 'react-redux';
import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import mockStore from 'autoru-frontend/mocks/mockStore';

import type { StateConfig } from 'auto-core/react/dataDomain/config/StateConfig';
import garagePromoMock from 'auto-core/react/dataDomain/garagePromoAll/mocks';
import mockAuth from 'auto-core/react/dataDomain/user/mocks';

import type { ReduxState } from './PageGarageDesktop';
import PageGarageDesktop from './PageGarageDesktop';

import '@testing-library/jest-dom';

const Context = createContextProvider(contextMock);

const bunkerData = {
    bunker: {},
};

const PROMOS = [
    garagePromoMock.value(),
    garagePromoMock.withId('1').value(),
    garagePromoMock.withId('2').value(),
    garagePromoMock.withId('3').value(),
];

beforeEach(() => {
    jest.spyOn(global.location, 'assign').mockImplementation(() => { });
});

afterEach(() => {
    jest.restoreAllMocks();
});

describe('componentDidMount', () => {
    it('должен отправить метрику tab,listing, если пользователь авторизован', () => {
        const state: ReduxState = {
            bunker: bunkerData,
            config: { data: { pageParams: {} } } as StateConfig,
            garagePromoAll: { partner_promos: PROMOS },
            user: mockAuth.withAuth(true).value(),
            vinCheckInput: { value: '' },
        };

        render(
            <Context>
                <Provider store={ mockStore(state) }>
                    <PageGarageDesktop/>
                </Provider>
            </Context>,
        );

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'tab', 'listing' ]);
    });

    it('не должен отправить метрику tab,listing, если пользователь не авторизован', () => {
        const state: ReduxState = {
            bunker: bunkerData,
            config: { data: { pageParams: {} } } as StateConfig,
            garagePromoAll: { partner_promos: PROMOS },
            user: mockAuth.withAuth(false).value(),
            vinCheckInput: { value: '' },
        };

        render(
            <Context>
                <Provider store={ mockStore(state) }>
                    <PageGarageDesktop/>
                </Provider>
            </Context>,
        );

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(0);
    });
});

describe('редирект на добавление с дурацких баннеров на лендинге', () => {
    it('если нет авторизации, правильный урл страницы авторизации', () => {
        const state: ReduxState = {
            bunker: bunkerData,
            config: { data: { pageParams: {} } } as StateConfig,
            garagePromoAll: { partner_promos: PROMOS },
            user: mockAuth.withAuth(false).value(),
            vinCheckInput: { value: '' },
        };

        render(
            <Context>
                <Provider store={ mockStore(state) }>
                    <PageGarageDesktop/>
                </Provider>
            </Context>,
        );

        const button = document.querySelector('.PageGarageInsurance button');
        button && userEvent.click(button);

        expect(global.location.assign).toHaveBeenCalledTimes(1);
        expect(global.location.assign).toHaveBeenCalledWith(
            'https://autoru_frontend.auth_domain/login/?r=https%3A%2F%2Flink%2Fgarage-add-card-conditional%2F%3F',
        );
    });

    it('если есть авторизация, правильный урл страницы добавления', () => {
        const state: ReduxState = {
            bunker: bunkerData,
            config: { data: { pageParams: {} } } as StateConfig,
            garagePromoAll: { partner_promos: PROMOS },
            user: mockAuth.withAuth(true).value(),
            vinCheckInput: { value: '' },
        };

        render(
            <Context>
                <Provider store={ mockStore(state) }>
                    <PageGarageDesktop/>
                </Provider>
            </Context>,
        );

        const button = document.querySelector('.PageGarageInsurance button');
        button && userEvent.click(button);

        expect(global.location.assign).toHaveBeenCalledTimes(1);
        expect(global.location.assign).toHaveBeenCalledWith('link/garage-add-card/?');
    });
});

describe('verified owner', () => {
    const renderComponent = (pageParams?: StateConfig['data']['pageParams']) => {
        const state: ReduxState = {
            bunker: bunkerData,
            config: { data: { pageParams: pageParams || {} } } as StateConfig,
            garagePromoAll: { partner_promos: PROMOS },
            user: mockAuth.withAuth(true).value(),
            vinCheckInput: { value: '' },
        };

        render(
            <Context>
                <Provider store={ mockStore(state) }>
                    <PageGarageDesktop/>
                </Provider>
            </Context>,
        );
    };

    it('не покажет попап в общем случае', () => {
        renderComponent();

        const modal = document.querySelector('.OwnerCheckModal');

        expect(modal).toBeInTheDocument();
        expect(modal?.className.includes('Modal_visible')).toBe(false);
    });

    it('покажет попап', () => {
        renderComponent({ popup: 'owner' });

        const modal = document.querySelector('.OwnerCheckModal');

        expect(modal).toBeInTheDocument();
        expect(modal?.className.includes('Modal_visible')).toBe(true);
    });
});

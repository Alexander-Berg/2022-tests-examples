/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { Provider } from 'react-redux';
import { render } from '@testing-library/react';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import mockStore from 'autoru-frontend/mocks/mockStore';

import type { StateConfig } from 'auto-core/react/dataDomain/config/StateConfig';
import garagePromoMock from 'auto-core/react/dataDomain/garagePromoAll/mocks';
import mockAuth from 'auto-core/react/dataDomain/user/mocks';

import PageGarageMobile from './PageGarageMobile';
import type { ReduxState } from './PageGarageMobile';

import '@testing-library/jest-dom';

const Context = createContextProvider(contextMock);

const bunkerData = {
    bunker: {},
};

const garagePromoAll = {
    partner_promos: [ garagePromoMock.value() ],
};

describe('componentDidMount', () => {
    it('должен отправить метрику tab,listing, если пользователь авторизован', () => {
        const state = {
            garagePromoAll,
            user: mockAuth.withAuth(true).value(),
            vinCheckInput: { value: '' },
            bunker: bunkerData,
        } as unknown as ReduxState;

        render(
            <Context>
                <Provider store={ mockStore(state) }>
                    <PageGarageMobile/>
                </Provider>
            </Context>,
        );

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'tab', 'listing' ]);
    });

    it('не должен отправить метрику tab,listing, если пользователь не авторизован', () => {
        const state = {
            garagePromoAll,
            user: mockAuth.withAuth(false).value(),
            vinCheckInput: { value: '' },
            bunker: bunkerData,
        } as unknown as ReduxState;

        render(
            <Context>
                <Provider store={ mockStore(state) }>
                    <PageGarageMobile/>
                </Provider>
            </Context>,
        );

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(0);
    });
});

describe('verified owner', () => {
    const renderComponent = (pageParams?: StateConfig['data']['pageParams']) => {
        const state: ReduxState = {
            bunker: bunkerData,
            config: { data: { pageParams: pageParams || {} } } as StateConfig,
            garagePromoAll,
            user: mockAuth.withAuth(true).value(),
            vinCheckInput: { value: '' },
        };

        render(
            <Context>
                <Provider store={ mockStore(state) }>
                    <PageGarageMobile/>
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

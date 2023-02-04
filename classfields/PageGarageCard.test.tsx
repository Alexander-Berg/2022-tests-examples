/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/components/common/GarageCardArticlesAndReviews/GarageCardArticlesAndReviews', () => jest.fn(() => null));

import React from 'react';
import { Provider } from 'react-redux';
import { render } from '@testing-library/react';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import mockStore from 'autoru-frontend/mocks/mockStore';

import mockConfig from 'auto-core/react/dataDomain/config/mock';
import type { TStateLenta } from 'auto-core/react/dataDomain/lenta/TStateLenta';
import type { StateConfig } from 'auto-core/react/dataDomain/config/StateConfig';
import type { StateUser } from 'auto-core/react/dataDomain/user/types';

import fixturesCard from 'auto-core/server/resources/publicApiGarage/methods/getCard.fixtures';
import fixturesListing from 'auto-core/server/resources/publicApiGarage/methods/getCardsListing.fixtures';

import type { ReduxState } from './PageGarageCard';
import PageGarageCard from './PageGarageCard';

import '@testing-library/jest-dom';

const Context = createContextProvider(contextMock);

let state: ReduxState;
beforeEach(() => {
    state = {
        bunker: {},
        config: mockConfig.value(),
        garage: {
            data: fixturesListing.response200(),
            pending: false,
        },
        garageCard: {
            data: fixturesCard.response200(),
            pending: false,
            state: 'VIEW',
        },
        garagePromoAll: {
            partner_promos: [],
        },
        state: {
            authModal: {},
        },
        lenta: {} as TStateLenta,
        user: { data: {}, pending: false } as StateUser,
    } as ReduxState;
});

describe('componentDidMount', () => {
    it('должен отправить метрику tab,open_cards, если есть ?metrika=true в урле', () => {
        state.config = mockConfig.withPageParams({ metrika: 'true' }).value();

        render(
            <Context>
                <Provider store={ mockStore(state) }>
                    <PageGarageCard/>
                </Provider>
            </Context>,
        );

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'tab', 'open_cards' ]);
    });

    it('должен отправить метрику tab,open_cards, если нет ?metrika=true в урле', () => {
        render(
            <Context>
                <Provider store={ mockStore(state) }>
                    <PageGarageCard/>
                </Provider>
            </Context>,
        );

        expect(contextMock.metrika.sendPageEvent).not.toHaveBeenCalledWith([ 'tab', 'open_cards' ]);
    });
});

// Тут может быть два модала на одной странице:
// 1) в блоке с предложением подтвердить владение тачкой на карточке текущей тачки,
// 2) на странице для всех типов карточек, чтобы показать, если в адресе есть ?popup=owner.
// Поэтому в данном тесте смотрим на последний всегда.
describe('verified owner', () => {
    const renderComponent = (pageParams?: StateConfig['data']['pageParams']) => {
        if (pageParams) {
            state.config.data.pageParams = pageParams;
        }

        return render(
            <Context>
                <Provider store={ mockStore(state) }>
                    <PageGarageCard/>
                </Provider>
            </Context>,
        );
    };

    it('не покажет попап в общем случае', () => {
        renderComponent();

        const modals = document.querySelectorAll('.OwnerCheckModal');
        const modal = modals[1];

        expect(modal).toBe(undefined);
    });

    it('покажет попап', () => {
        renderComponent({ popup: 'owner' });

        const modals = document.querySelectorAll('.OwnerCheckModal');
        const modal = modals[1];

        expect(modal).toBeInTheDocument();
        expect(modal?.className.includes('Modal_visible')).toBe(true);
    });
});

/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { Provider } from 'react-redux';
import { render } from '@testing-library/react';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import mockStore from 'autoru-frontend/mocks/mockStore';

import mockConfig from 'auto-core/react/dataDomain/config/mock';
import defaultJournalArticlesStateMock from 'auto-core/react/dataDomain/journalArticles/mocks/defaultState.mock';
import type { TStateLenta } from 'auto-core/react/dataDomain/lenta/TStateLenta';
import type { StateConfig } from 'auto-core/react/dataDomain/config/StateConfig';

import fixturesCard from 'auto-core/server/resources/publicApiGarage/methods/getCard.fixtures';
import fixturesListing from 'auto-core/server/resources/publicApiGarage/methods/getCardsListing.fixtures';

import type { TReviewsData } from 'auto-core/types/TReviews';

import type { ReduxState } from './PageGarageCardMobile';
import PageGarageCardMobile from './PageGarageCardMobile';

import '@testing-library/jest-dom';

jest.mock('auto-core/react/components/common/GarageCardArticlesAndReviews/GarageCardArticlesAndReviews', () => jest.fn(() => null));

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
        journalArticles: defaultJournalArticlesStateMock,
        reviews: {} as TReviewsData,
        lenta: {
            totalItemsCount: 10,
        } as TStateLenta,
        user: { data: {} },
    } as unknown as ReduxState;
});

describe('componentDidMount', () => {

    it('???????????? ?????????????????? ?????????????? tab,open_cards, ???????? ???????? ?metrika=true ?? ????????', () => {
        state.config = mockConfig.withPageParams({ metrika: 'true' }).value();

        render(
            <Context>
                <Provider store={ mockStore(state) }>
                    <PageGarageCardMobile/>
                </Provider>
            </Context>,
        );

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'tab', 'open_cards' ]);
    });

    it('???????????? ?????????????????? ?????????????? tab,open_cards, ???????? ?????? ?metrika=true ?? ????????', () => {
        render(
            <Context>
                <Provider store={ mockStore(state) }>
                    <PageGarageCardMobile/>
                </Provider>
            </Context>,
        );

        expect(contextMock.metrika.sendPageEvent).not.toHaveBeenCalledWith([ 'tab', 'open_cards' ]);
    });
});

// ?????? ?????????? ???????? ?????? ???????????? ???? ?????????? ????????????????:
// 1) ?? ?????????? ?? ???????????????????????? ?????????????????????? ???????????????? ???????????? ???? ???????????????? ?????????????? ??????????,
// 2) ???? ???????????????? ?????? ???????? ?????????? ????????????????, ?????????? ????????????????, ???????? ?? ???????????? ???????? ?popup=owner.
// ?????????????? ?? ???????????? ?????????? ?????????????? ???? ?????????????????? ????????????.
describe('verified owner', () => {
    const renderComponent = (pageParams?: StateConfig['data']['pageParams']) => {
        if (pageParams) {
            state.config.data.pageParams = pageParams;
        }

        return render(
            <Context>
                <Provider store={ mockStore(state) }>
                    <PageGarageCardMobile/>
                </Provider>
            </Context>,
        );
    };

    it('???? ?????????????? ?????????? ?? ?????????? ????????????', () => {
        renderComponent();

        const modals = document.querySelectorAll('.OwnerCheckModal');
        const modal = modals[1];

        expect(modal).toBe(undefined);
    });

    it('?????????????? ??????????', () => {
        renderComponent({ popup: 'owner' });

        const modals = document.querySelectorAll('.OwnerCheckModal');
        const modal = modals[1];

        expect(modal).toBeInTheDocument();
        expect(modal?.className.includes('Modal_visible')).toBe(true);
    });
});

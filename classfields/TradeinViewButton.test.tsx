/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import statApi from 'auto-core/lib/event-log/statApi';

import userStateMock from 'auto-core/react/dataDomain/user/mocks';
import type { ReduxState } from 'auto-core/react/components/common/TradeinAbstract/TradeinAbstract';

import TradeinViewButton from './TradeinViewButton';

jest.mock('auto-core/react/lib/offer/getRedemptionAvailable', () => () => true);
jest.mock('auto-core/lib/event-log/statApi');

let state: ReduxState;
beforeEach(() => {
    state = {
        card: cloneOfferWithHelpers({}).value(),
        tradein: {
            offers: [],
            tradeinPrice: { data: null, error: true, pending: false },
        },
        user: userStateMock.withAuth(true).value(),
        searchID: {
            searchID: 'searchID',
            parentSearchId: 'parentSearchID',
        },
    };
});

it('должен отправить statlog при открытии', () => {
    const offer = cloneOfferWithHelpers({
        category: 'cars',
        saleId: '123-abc',
        section: 'used',
    }).withRedemptionAvailable(true).value();
    state.card = offer;
    const component = shallow(
        <TradeinViewButton
            isMobile={ false }
            offer={ offer }
        />,
        {
            context: {
                ...contextMock,
                store: mockStore(state),
            },
        },
    );

    component.dive().find('TradeinButton').simulate('click');

    expect(statApi.logImmediately).toHaveBeenCalledWith({
        trade_in_request_click_event: {
            card_id: '123-abc',
            category: 'CARS',
            context_page: 'PAGE_CARD',
            group_size: 0,
            grouping_id: '',
            index: 0,
            page_type: 'CARD',
            search_position: 0,
            self_type: 'TYPE_SINGLE',
            section: 'USED',
            phone_number: '',
            name: '',
            search_query_id: 'searchID',
        },
    });
});

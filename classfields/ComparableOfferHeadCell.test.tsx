/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/lib/event-log/statApi');

import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';
import comparableOffersMock from 'autoru-frontend/mockData/compare/offersBase';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import { getBunkerMock } from 'autoru-frontend/mockData/state/bunker.mock';

import statApi from 'auto-core/lib/event-log/statApi';

import offerCarsMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';
import configMock from 'auto-core/react/dataDomain/config/mock';
import userWithAuthMock from 'auto-core/react/dataDomain/user/mocks/withAuth.mock';

import type { OfferCompareData } from 'auto-core/types/proto/auto/api/compare_model';

import ComparableOfferHeadCell from './ComparableOfferHeadCell';

const store = mockStore({
    bunker: getBunkerMock([ 'common/metrics' ]),
    config: configMock.withPageType('compare').value(),
    user: userWithAuthMock,
});

it('отправит событие chat_init_event во фронтлог', async() => {
    type VertisChat = typeof window.vertis_chat;
    window.vertis_chat = { open_chat_for_offer: jest.fn() } as Partial<VertisChat> as VertisChat;

    const comparableOfferMock = {
        ...comparableOffersMock[0],
        summary: cloneOfferWithHelpers(offerCarsMock)
            .withChatOnly()
            .withIsOwner(false)
            .value(),
    };

    const wrapper = shallow(
        <ComparableOfferHeadCell
            index={ 10 }
            offer={ comparableOfferMock as OfferCompareData }
            handleOfferRemove={ jest.fn() }
        />,
        { context: { ...contextMock, store } },
    ).dive();

    wrapper.find('Connect(OpenChatByOffer)').dive().dive().simulate('click');

    expect(statApi.logImmediately).toHaveBeenCalledTimes(1);
    expect(statApi.logImmediately).toHaveBeenCalledWith({
        chat_init_event: {
            card_from: 'SERP',
            card_id: '1085562758-1970f439',
            category: 'CARS',
            context_block: 'BLOCK_COMPARE',
            context_page: 'PAGE_COMPARE',
            search_query_id: '',
            section: 'USED',
            self_type: 'TYPE_SINGLE',
            trade_in_allowed: false,
        },
    });
});

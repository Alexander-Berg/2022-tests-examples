/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/lib/event-log/statApi');

import React from 'react';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';

import { OfferStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import { getBunkerMock } from 'autoru-frontend/mockData/state/bunker.mock';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import statApi from 'auto-core/lib/event-log/statApi';

import configMock from 'auto-core/react/dataDomain/config/mock';
import userWithAuthMock from 'auto-core/react/dataDomain/user/mocks/withAuth.mock';
import cardStateMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import type { StateUser } from 'auto-core/react/dataDomain/user/types';
import type { StateConfig } from 'auto-core/react/dataDomain/config/StateConfig';
import type { StateBunker } from 'auto-core/react/dataDomain/bunker/StateBunker';

import CardChatPresets from './CardChatPresets';
import type { OwnProps } from './CardChatPresets';

let props: OwnProps;
interface AppState {
    bunker: StateBunker;
    user: StateUser;
    config: StateConfig;
}

let initialState: AppState;
beforeEach(() => {
    props = {
        offer: cloneOfferWithHelpers(cardStateMock).withIsOwner(false).withSellerChatsEnabled(true).value(),
    };
    initialState = {
        bunker: getBunkerMock([ 'common/chat_preset_messages', 'common/metrics' ]),
        user: userWithAuthMock,
        config: configMock.withPageType('card').value(),
    };

    contextMock.logVasEvent.mockClear();
});

it('покажется при соблюдении всех условий', () => {
    const page = shallowRenderComponent({ props, initialState });
    expect(page.isEmptyRender()).toBe(false);
});

it('отправит событие chat_init_event во фронтлог', () => {
    type VertisChat = typeof window.vertis_chat;
    window.vertis_chat = { open_chat_for_offer: jest.fn() } as Partial<VertisChat> as VertisChat;
    contextMock.hasExperiment.mockImplementationOnce(() => true);

    const page = shallowRenderComponent({ props, initialState });
    page.find('Connect(OpenChatByOffer)').first().dive().dive().simulate('click');

    expect(statApi.logImmediately).toHaveBeenCalledTimes(1);
    expect(statApi.logImmediately).toHaveBeenCalledWith({
        chat_init_event: {
            card_from: 'SERP',
            card_id: '1085562758-1970f439',
            category: 'CARS',
            context_block: 'BLOCK_ASK_SELLER',
            context_page: 'PAGE_CARD',
            search_query_id: '',
            section: 'USED',
            self_type: 'TYPE_SINGLE',
            trade_in_allowed: false,
        },
    });
});

describe('не покажется', () => {
    it('дилеру', () => {
        props.offer = cloneOfferWithHelpers(cardStateMock)
            .withSellerTypeCommercial()
            .value();
        const page = shallowRenderComponent({ props, initialState });
        expect(page.isEmptyRender()).toBe(true);
    });

    it('владельцу', () => {
        props.offer = cloneOfferWithHelpers(cardStateMock)
            .withIsOwner(true)
            .value();
        const page = shallowRenderComponent({ props, initialState });
        expect(page.isEmptyRender()).toBe(true);
    });

    it('если чаты отключены', () => {
        props.offer = cloneOfferWithHelpers(cardStateMock)
            .withIsOwner(true)
            .withSellerChatsEnabled(false)
            .value();
        const page = shallowRenderComponent({ props, initialState });
        expect(page.isEmptyRender()).toBe(true);
    });

    it('если объява неактивна', () => {
        props.offer = cloneOfferWithHelpers(cardStateMock)
            .withStatus(OfferStatus.INACTIVE)
            .value();
        const page = shallowRenderComponent({ props, initialState });
        expect(page.isEmptyRender()).toBe(true);
    });
});

function shallowRenderComponent({ initialState, props }: { props: OwnProps; initialState: AppState }) {
    const ContextProvider = createContextProvider(contextMock);
    const store = mockStore(initialState);

    const page = shallow(
        <ContextProvider>
            <Provider store={ store }>
                <CardChatPresets { ...props }/>
            </Provider>
        </ContextProvider>,
    );

    return page.dive().dive().dive();
}

/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResourcePublicApi: jest.fn(),
    };
});
jest.mock('auto-core/lib/event-log/statApi');

import React from 'react';
import { shallow } from 'enzyme';

import { OfferStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import contextMock from 'autoru-frontend/mocks/contextMock';
import type { ThunkMockStore } from 'autoru-frontend/mocks/mockStore';
import mockStore from 'autoru-frontend/mocks/mockStore';

import statApi from 'auto-core/lib/event-log/statApi';

import userStateMock from 'auto-core/react/dataDomain/user/mocks';
import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import gateApi from 'auto-core/react/lib/gateApi';

import type { TOfferPriceInfo } from 'auto-core/types/proto/auto/api/api_offer_model';

import type { ReduxState } from './ButtonFavorite';
import ButtonFavorite from './ButtonFavorite';

const getResourcePublicApi = gateApi.getResourcePublicApi as jest.MockedFunction<typeof gateApi.getResourcePublicApi>;

let store: ThunkMockStore<ReduxState>;

it('должен корректно обработать удаление из избранного', () => {
    store = mockStore({
        user: userStateMock.withAuth(true).value(),
    });

    const offer = {
        ...offerMock,
        id: 'foo',
        is_favorite: true,
    };
    const expectedActions = [
        { type: 'FAVORITES_DELETE_ITEM_RESOLVED', payload: { offer } },
        { type: 'NOTIFIER_SHOW_MESSAGE', payload: { message: 'Удалено из избранного', view: 'success' } },
    ];
    const tree = shallow(
        <ButtonFavorite
            offer={ offer }
        />, { context: { ...contextMock, store } },
    ).dive();
    const p = Promise.resolve({ status: 'SUCCESS' });
    getResourcePublicApi.mockImplementation(() => p);
    tree.find('Button').simulate('click', { nativeEvent: { stopImmediatePropagation: jest.fn() } });
    return p.then(() => {
        expect(store.getActions()).toEqual(expectedActions);
        expect(contextMock.metrika.reachGoal).not.toHaveBeenCalledWith('IN_FAVORITES');
        expect(statApi.log).toHaveBeenCalledWith({
            favourite_remove_event: {
                card_from: 'SERP',
                card_id: 'foo-1970f439',
                category: 'CARS',
                context_block: 'BLOCK_UNDEFINED',
                context_page: 'PAGE_UNDEFINED',
                index: 0,
                region_id: [ '213', '1', '3', '225', '10001', '10000' ],
                search_query_id: '',
                section: 'USED',
                self_type: 'TYPE_SINGLE',
                trade_in_allowed: false,
            } });
    });
});

it('не должен корректно удалить из избранного если оффер неактивный и передано preventDelete', () => {
    store = mockStore({
        user: userStateMock.withAuth(true).value(),
    });

    const offer = {
        ...offerMock,
        id: 'foo',
        is_favorite: true,
        status: OfferStatus.REMOVED,
    };
    const tree = shallow(
        <ButtonFavorite
            offer={ offer }
            preventDelete={ true }
        />, { context: { ...contextMock, store } },
    ).dive();
    const p = Promise.resolve({ status: 'SUCCESS' });
    getResourcePublicApi.mockImplementation(() => p);
    tree.find('Button').simulate('click', { nativeEvent: { stopImmediatePropagation: jest.fn() } });
    return p.then(() => {
        expect(store.getActions()).toEqual([]);
        expect(contextMock.metrika.reachGoal).not.toHaveBeenCalledWith('IN_FAVORITES');
    });
});

it('должен корректно обработать добавление в избранное', () => {
    store = mockStore({
        user: userStateMock.withAuth(true).value(),
        config: {
            data: {
                pageParams: {},
            },
        },
    });

    const offer = {
        ...offerMock,
        id: 'foo',
        is_favorite: false,
        price_info: {} as TOfferPriceInfo,
    };
    const expectedActions = [
        { type: 'FAVORITES_ADD_ITEM_RESOLVED', payload: { offer } },
        { type: 'NOTIFIER_SHOW_MESSAGE', payload: { message: expect.any(Function), view: 'success' } },
    ];
    const tree = shallow(
        <ButtonFavorite
            offer={ offer }
        />, { context: { ...contextMock, store } },
    ).dive();
    const p = Promise.resolve({ status: 'SUCCESS' });
    getResourcePublicApi.mockImplementation(() => p);
    tree.find('Button').simulate('click', { nativeEvent: { stopImmediatePropagation: jest.fn() } });
    return p.then(() => {
        expect(store.getActions()).toEqual(expectedActions);
        expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('IN_FAVORITES');
        expect(statApi.log).toHaveBeenCalledWith({
            favourite_add_event: {
                card_from: 'SERP',
                card_id: 'foo-1970f439',
                category: 'CARS',
                context_block: 'BLOCK_UNDEFINED',
                context_page: 'PAGE_UNDEFINED',
                index: 0,
                region_id: [ '213', '1', '3', '225', '10001', '10000' ],
                search_query_id: '',
                section: 'USED',
                self_type: 'TYPE_SINGLE',
                trade_in_allowed: false,
            } });
    });
});

it('не будет отправлять event log, если есть проп disableEventsLog', () => {
    store = mockStore({
        user: userStateMock.withAuth(true).value(),
        config: {
            data: {
                pageParams: {},
            },
        },
    });

    const offer = {
        ...offerMock,
        id: 'foo',
        is_favorite: false,
        price_info: {} as TOfferPriceInfo,
    };
    const tree = shallow(
        <ButtonFavorite
            offer={ offer }
            disableEventsLog
        />, { context: { ...contextMock, store } },
    ).dive();

    const p = Promise.resolve({ status: 'SUCCESS' });
    getResourcePublicApi.mockImplementation(() => p);
    tree.find('Button').simulate('click', { nativeEvent: { stopImmediatePropagation: jest.fn() } });
    return p.then(() => {
        expect(statApi.log).not.toHaveBeenCalled();
    });
});

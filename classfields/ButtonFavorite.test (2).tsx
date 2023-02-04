/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import type { ThunkMockStore } from 'autoru-frontend/mocks/mockStore';
import mockStore from 'autoru-frontend/mocks/mockStore';

import statApi from 'auto-core/lib/event-log/statApi';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import gateApi from 'auto-core/react/lib/gateApi';
import userStateMock from 'auto-core/react/dataDomain/user/mocks';
import configStateMock from 'auto-core/react/dataDomain/config/mock';
import type { TStateListing } from 'auto-core/react/dataDomain/listing/TStateListing';
import openAuthModalWithCallback from 'auto-core/react/dataDomain/state/actions/authModalWithCallbackOpen';

import type { Offer, TOfferPriceInfo } from 'auto-core/types/proto/auto/api/api_offer_model';

import type { ReduxState } from './ButtonFavorite';
import ButtonFavorite from './ButtonFavorite';

jest.mock('auto-core/react/dataDomain/state/actions/authModalWithCallbackOpen', () => jest.fn(
    () => ({ type: '' }),
));

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResourcePublicApi: jest.fn(),
    };
});
jest.mock('auto-core/lib/event-log/statApi');

const getResourcePublicApi = gateApi.getResourcePublicApi as jest.MockedFunction<typeof gateApi.getResourcePublicApi>;
let store: ThunkMockStore<ReduxState>;

it('должен отправить статистику при удалении оффера из избранного', () => {
    store = mockStore({
        config: configStateMock.value(),
        listing: { data: { offers: ([] as Array<Offer>) } } as TStateListing,
        user: userStateMock.withAuth(true).value(),
    });

    const offer = {
        ...offerMock,
        id: 'foo',
        is_favorite: true,
    };

    const tree = shallow(
        <ButtonFavorite
            offer={ offer }
        />, { context: { ...contextMock, store } },
    ).dive();
    const p = Promise.resolve({ status: 'SUCCESS' });
    getResourcePublicApi.mockImplementation(() => p);
    tree.find('Button').simulate('click', { preventDefault: jest.fn() });
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

it('должен отправить статистику при добавлении оффера в избранное', () => {
    store = mockStore({
        config: configStateMock.value(),
        listing: { data: { offers: ([] as Array<Offer>) } } as TStateListing,
        user: userStateMock.withAuth(true).value(),
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
        />, { context: { ...contextMock, store } },
    ).dive();
    const p = Promise.resolve({ status: 'SUCCESS' });
    getResourcePublicApi.mockImplementation(() => p);
    tree.find('Button').simulate('click', { preventDefault: jest.fn() });
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

it('должен открыть модал авторизации для незалогина', () => {
    store = mockStore({
        config: configStateMock.value(),
        listing: { data: { offers: ([] as Array<Offer>) } } as TStateListing,
        user: userStateMock.withAuth(false).value(),
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
        />, { context: { ...contextMock, store } },
    ).dive();
    const p = Promise.resolve({ status: 'SUCCESS' });
    getResourcePublicApi.mockImplementation(() => p);
    tree.find('Button').simulate('click', { preventDefault: jest.fn() });
    expect(openAuthModalWithCallback).toHaveBeenCalled();
});

it('не должен открыть модал авторизации для залогина', () => {
    store = mockStore({
        config: configStateMock.value(),
        listing: { data: { offers: ([] as Array<Offer>) } } as TStateListing,
        user: userStateMock.withAuth(true).value(),
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
        />, { context: { ...contextMock, store } },
    ).dive();
    const p = Promise.resolve({ status: 'SUCCESS' });
    getResourcePublicApi.mockImplementation(() => p);
    tree.find('Button').simulate('click', { preventDefault: jest.fn() });
    expect(openAuthModalWithCallback).not.toHaveBeenCalled();
});

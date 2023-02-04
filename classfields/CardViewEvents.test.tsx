import _ from 'lodash';
import React from 'react';
import { shallow } from 'enzyme';

import type { OfferGrouppingInfo } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';
import { OfferStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';
import type { PriceInfo } from '@vertis/schema-registry/ts-types-snake/auto/api/common_model';

import contextMock from 'autoru-frontend/mocks/contextMock';

import statApi from 'auto-core/lib/event-log/statApi';

import cardMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';

import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import CardViewEvents from './CardViewEvents';

jest.mock('auto-core/lib/event-log/statApi');

jest.mock('auto-core/react/lib/localStatData', () => {
    return {
        getContextByOfferID: ({ id }: { id: string }) => {
            return id === 'with_context' ? {
                block: 'BLOCK_LISTING',
                page: 'PAGE_LISTING',
                selfType: 'TYPE_SINGLE',
            } : {};
        },
    };
});

let offer: Offer;
beforeEach(() => {
    jest.useFakeTimers();
    offer = _.cloneDeep(cardMock);
});

afterEach(() => {
    jest.clearAllTimers();
    jest.resetAllMocks();
});

describe('метрика', () => {
    it('не должен отправлять события', () => {
        shallow(
            <CardViewEvents offer={ offer } searchID="searchID"/>,
            { context: { ...contextMock } },
        );

        jest.runAllTimers();

        expect(contextMock.metrika.reachGoal).not.toHaveBeenCalled();
    });

    it('должен отправить событие VIEW_SOLD_CAR_CARD для проданной карточки', () => {
        offer.additional_info!.is_owner = false;
        offer.status = OfferStatus.INACTIVE;

        shallow(
            <CardViewEvents offer={ offer } searchID="searchID"/>,
            { context: { ...contextMock } },
        );

        jest.runAllTimers();

        expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('VIEW_SOLD_CAR_CARD');
    });
});

describe('retargeting', () => {
    beforeEach(() => {
        offer.additional_info!.is_owner = false;
        offer.category = 'cars';
        offer.state!.image_urls = Array(1);
        offer.state!.state_not_beaten = true;
        offer.status = OfferStatus.ACTIVE;
    });

    it('должен отправить событие для активного небитого объявления с фото', () => {
        shallow(
            <CardViewEvents offer={ offer } searchID="searchID"/>,
            { context: { ...contextMock } },
        );

        jest.runAllTimers();

        expect(contextMock.metrika.sendEcommerce).toHaveBeenCalledWith('pageview', offer);
    });

    it('не должен отправлять события для битого', () => {
        offer.state!.state_not_beaten = false;

        shallow(
            <CardViewEvents offer={ offer } searchID="searchID"/>,
            { context: { ...contextMock } },
        );

        jest.runAllTimers();

        expect(contextMock.metrika.params).not.toHaveBeenCalled();
    });

    it('не должен отправлять события для объявлений без фото', () => {
        offer.state!.image_urls = [];

        shallow(
            <CardViewEvents offer={ offer } searchID="searchID"/>,
            { context: { ...contextMock } },
        );

        jest.runAllTimers();

        expect(contextMock.metrika.params).not.toHaveBeenCalled();
    });
});

describe('tskv', () => {
    it('должен отправить событие для обычного объявления', () => {
        offer.additional_info!.redemption_available = false;
        offer.id = 'without_context';
        offer.hash = 'a';
        offer.saleId = 'without_context-a';
        offer.section = 'used';

        shallow(
            <CardViewEvents offer={ offer } searchID="searchID"/>,
            { context: { ...contextMock } },
        );

        jest.runAllTimers();

        expect(statApi.logImmediately).toHaveBeenCalledWith({
            card_view_event: {
                card_from: 'SERP',
                card_id: 'without_context-a',
                category: 'CARS',
                context_block: 'BLOCK_UNDEFINED',
                context_page: 'PAGE_UNDEFINED',
                section: 'USED',
                self_type: 'TYPE_SINGLE',
                trade_in_allowed: false,
                search_query_id: 'searchID',
            },
        });
    });

    it('должен отправить событие для обычного объявления и добавить контекст', () => {
        offer.additional_info!.redemption_available = false;
        offer.id = 'with_context';
        offer.hash = 'a';
        offer.saleId = 'with_context-a';
        offer.section = 'used';

        shallow(
            <CardViewEvents offer={ offer } searchID="searchID"/>,
            { context: { ...contextMock } },
        );

        jest.runAllTimers();

        expect(statApi.logImmediately).toHaveBeenCalledWith({
            card_view_event: {
                card_from: 'SERP',
                card_id: 'with_context-a',
                category: 'CARS',
                context_block: 'BLOCK_LISTING',
                context_page: 'PAGE_LISTING',
                section: 'USED',
                self_type: 'TYPE_SINGLE',
                trade_in_allowed: false,
                search_query_id: 'searchID',
            },
        });
    });

    jest.mock('auto-core/react/lib/localStatData', () => {
        return {
            getReferrerContextParams: () => ({}),
        };
    });

    it('должен отправить событие для нового объявления с grouping_id', () => {
        offer.additional_info!.redemption_available = false;
        offer.id = 'without_context';
        offer.hash = 'a';
        offer.saleId = 'without_context-a';
        offer.section = 'new';
        offer.groupping_info = {
            price_from: { rur_price: 100 } as unknown as PriceInfo,
            price_to: { rur_price: 200 } as unknown as PriceInfo,
        } as OfferGrouppingInfo;

        shallow(
            <CardViewEvents
                groupingId="123-456"
                groupSize={ 5 } offer={ offer } searchID="searchID"/>,
            { context: { ...contextMock } },
        );

        jest.runAllTimers();

        expect(statApi.logImmediately).toHaveBeenCalledWith({
            card_view_event: {
                card_from: 'SERP',
                card_id: 'without_context-a',
                category: 'CARS',
                context_block: 'BLOCK_UNDEFINED',
                context_page: 'PAGE_UNDEFINED',
                group_size: 5,
                grouping_id: '123-456',
                index: 0,
                section: 'NEW',
                self_type: 'TYPE_SINGLE',
                trade_in_allowed: false,
                search_query_id: 'searchID',
                price_from: '100',
                price_to: '200',
            },
        });
    });
});

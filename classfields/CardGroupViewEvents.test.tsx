jest.mock('auto-core/lib/event-log/statApi');

jest.mock('auto-core/react/lib/localStatData', () => {
    return {
        getContextByPageLifeTime: jest.fn(() => {
            return {};
        }),
    };
});

import _ from 'lodash';
import React from 'react';
import { shallow } from 'enzyme';

import { OfferStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import contextMock from 'autoru-frontend/mocks/contextMock';

import statApi from 'auto-core/lib/event-log/statApi';

import cardMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';
import localStatData from 'auto-core/react/lib/localStatData';

import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import CardGroupViewEvents from './CardGroupViewEvents';

const getContextByPageLifeTime = localStatData.getContextByPageLifeTime as jest.MockedFunction<typeof localStatData.getContextByPageLifeTime>;

let offer: Offer;
beforeEach(() => {
    jest.useFakeTimers();
    offer = _.cloneDeep(cardMock);
});

afterEach(() => {
    jest.clearAllTimers();
    jest.clearAllMocks();
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
            <CardGroupViewEvents
                category="cars"
                offer={ offer }
                groupingId="groupingId"
                groupSize={ 10 }
                searchID="searchID"
                section="new"
            />,
            { context: { ...contextMock } },
        );

        jest.runAllTimers();

        expect(contextMock.metrika.sendEcommerce).toHaveBeenCalledWith('pageview', offer);
        expect(contextMock.metrika.params).toHaveBeenCalledWith({
            remarketing: {
                cars: {
                    pageview: {
                        mark: { ford: { seller: { user: {} }, status: { used: {} } } },
                        model: { ecosport: { seller: { user: {} }, status: { used: {} } } },
                    },
                },
            },
        });
    });

    it('не должен отправлять события для страницы без оффера', () => {
        shallow(
            <CardGroupViewEvents
                category="cars"
                groupingId="groupingId"
                groupSize={ 10 }
                searchID="searchID"
                section="new"
            />,
            { context: { ...contextMock } },
        );

        jest.runAllTimers();

        expect(contextMock.metrika.params).not.toHaveBeenCalled();
        expect(contextMock.metrika.sendEcommerce).not.toHaveBeenCalled();
    });
});

describe('tskv', () => {
    it('должен отправить событие для группы', () => {
        offer.additional_info!.redemption_available = false;
        offer.id = 'without_context';
        offer.hash = 'a';
        offer.section = 'new';

        shallow(
            <CardGroupViewEvents
                category="cars"
                offer={ offer }
                groupingId="123-456"
                groupSize={ 5 }
                searchID="searchID"
                section="new"
            />,
            { context: { ...contextMock } },
        );

        jest.runAllTimers();

        expect(statApi.logImmediately).toHaveBeenCalledWith({
            card_view_event: {
                card_from: 'SERP',
                category: 'CARS',
                context_block: 'BLOCK_UNDEFINED',
                context_page: 'PAGE_UNDEFINED',
                group_size: 5,
                grouping_id: '123-456',
                index: 0,
                section: 'NEW',
                self_type: 'TYPE_GROUP',
                search_query_id: 'searchID',
            },
        });
    });

    it('должен отправить событие для группы и добавить контекст', () => {
        getContextByPageLifeTime.mockImplementation(() => {
            return {
                block: 'BLOCK_LISTING',
                page: 'PAGE_LISTING',
                selfType: 'TYPE_SINGLE',
            };
        });

        offer.additional_info!.redemption_available = false;
        offer.id = 'with_context';
        offer.hash = 'a';
        offer.section = 'used';

        shallow(
            <CardGroupViewEvents
                category="cars"
                offer={ offer }
                groupingId="123-456"
                groupSize={ 5 }
                searchID="searchID"
                section="new"
            />,
            { context: { ...contextMock } },
        );

        jest.runAllTimers();

        expect(statApi.logImmediately).toHaveBeenCalledWith({
            card_view_event: {
                card_from: 'SERP',
                category: 'CARS',
                context_block: 'BLOCK_LISTING',
                context_page: 'PAGE_LISTING',
                section: 'NEW',
                group_size: 5,
                grouping_id: '123-456',
                index: 0,
                self_type: 'TYPE_GROUP',
                search_query_id: 'searchID',
            },
        });
    });
});

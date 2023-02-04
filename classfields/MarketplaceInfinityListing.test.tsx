jest.mock('auto-core/lib/event-log/statApi');

import React from 'react';
import { shallow } from 'enzyme';

import { ContextBlock, ContextPage } from '@vertis/schema-registry/ts-types-snake/auto/api/stat_events';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import statApi from 'auto-core/lib/event-log/statApi';

import offerMock from 'auto-core/react/dataDomain/listing/mocks/listingOffer.cars.mock';

import MarketplaceInfinityListing from './MarketplaceInfinityListing';

const log = statApi.log as jest.MockedFunction<typeof statApi.log>;

const initialState = {
    personalizedOffersFeed: {
        data: {
            offers: [
                offerMock,
            ],
            pagination: {
                current: 1,
            },
        },
    },
};

const store = mockStore(initialState);

it('отправляет лог c правильным context_block и context_page', () => {
    const wrapper = shallow(
        <MarketplaceInfinityListing/>,
        { context: { ...contextMock, store } },
    ).dive().find('MarketplaceInfinityListingItem').dive();

    wrapper.find('InView').simulate('change', true);

    expect(log).toHaveBeenCalledWith(expect.objectContaining({
        card_show_event: expect.objectContaining({
            context_block: ContextBlock.BLOCK_LISTING,
            context_page: ContextPage.UNRECOGNIZED,
        }),
    }));
});

it('должен отправить эвентлог, когда блок нарисован', function() {
    shallow(
        <MarketplaceInfinityListing/>,
        { context: { ...contextMock, store } },
    ).dive();

    expect(statApi.log).toHaveBeenCalledTimes(1);
});

import type { TStateListing } from 'auto-core/react/dataDomain/listing/TStateListing';

import getFilteredOrTotalGroupsCount from './getFilteredOrTotalGroupsCount';

let state: { listing: TStateListing };
beforeEach(() => {
    state = {
        listing: {
            data: {
                groupsCount: 1,
                grouping: {
                    groups_count: 2,
                },
                offers: [],
                pagination: {
                    current: 1,
                    page: 1,
                    page_size: 10,
                    total_offers_count: 2,
                    total_page_count: 1,
                    from: 1,
                    to: 2,
                },
                request_id: '456',
                search_id: '123',
                search_parameters: {},
                shouldShowListingBestPriceBlock: false,
            },
            searchID: '456',
        },
    };
});

it('должен вернуть groupsCount, если он есть', () => {
    expect(getFilteredOrTotalGroupsCount(state)).toEqual(1);
});

it('должен вернуть groupsCount, если он есть и равен  0', () => {
    state.listing.data.groupsCount = 0;
    expect(getFilteredOrTotalGroupsCount(state)).toEqual(0);
});

it('должен вернуть groups_count, если нет groupsCount', () => {
    delete state.listing.data.groupsCount;
    expect(getFilteredOrTotalGroupsCount(state)).toEqual(2);
});

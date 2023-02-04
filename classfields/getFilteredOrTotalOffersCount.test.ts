import type { TStateListing } from 'auto-core/react/dataDomain/listing/TStateListing';

import getFilteredOrTotalOffersCount from './getFilteredOrTotalOffersCount';

let state: { listing: TStateListing };
beforeEach(() => {
    state = {
        listing: {
            data: {
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
            filteredOffersCount: 1,
        },
    };
});

it('должен вернуть filteredOffersCount, если он есть', () => {
    expect(getFilteredOrTotalOffersCount(state)).toEqual(1);
});

it('должен вернуть filteredOffersCount, если он есть и равен  0', () => {
    state.listing.filteredOffersCount = 0;
    expect(getFilteredOrTotalOffersCount(state)).toEqual(0);
});

it('должен вернуть totalOffersCount, если нет filteredOffersCount', () => {
    delete state.listing.filteredOffersCount;
    expect(getFilteredOrTotalOffersCount(state)).toEqual(2);
});

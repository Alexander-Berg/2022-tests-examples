import type { TCounter } from 'auto-core/react/dataDomain/listingLocatorCounters/TStateListingLocatorCounters';
import type { RegionWithLinguistics } from 'auto-core/react/dataDomain/geo/StateGeo';

import type { TOfferCategory, TOfferSection } from 'auto-core/types/proto/auto/api/api_offer_model';

import type { Params } from './getSearchParamsForNextPage';
import getSearchParamsForNextPage from './getSearchParamsForNextPage';

let rings: Array<TCounter>;
let params: Params;

beforeEach(() => {
    rings = [
        { count: 52, radius: 600 },
        { count: 68, radius: 800 },
    ];
    params = {
        searchParameters: {
            catalog_filter: [ { mark: 'AUDI' } ],
            section: 'new' as TOfferSection,
            category: 'cars' as TOfferCategory,
        },
        pagination: {
            total_page_count: 2,
            total_offers_count: 47,
            page: 2,
            page_size: 37,
            from: 38,
            to: 74,
            current: 2,
        },
        gidsInfo: [ { id: 17 } as RegionWithLinguistics ],
        geoRadius: 0,
        rings,
        currentRing: rings[0],
    };
});

describe('должен правильно сформировать параметры для запроса следующей страницы БЛ', () => {
    it('следующая страница текущего кольца', () => {
        params.pagination.current = 1;
        params.pagination.page = 1;
        params.pagination.from = 1;
        params.pagination.to = 37;

        expect(getSearchParamsForNextPage(params)).toEqual({
            newSearchParameters: {
                catalog_filter: [ { mark: 'AUDI' } ],
                category: 'cars',
                exclude_geo_radius: 0,
                exclude_rid: 17,
                geo_radius: 600,
                infinite_listing: true,
                page: 2,
                page_size: 37,
                rid: [ 17 ],
                section: 'new',
            },
            newCurrentRing: { count: 52, radius: 600 },
        });
    });

    it('первая страница следующего кольца', () => {
        expect(getSearchParamsForNextPage(params)).toEqual({
            newSearchParameters: {
                catalog_filter: [ { mark: 'AUDI' } ],
                category: 'cars',
                exclude_geo_radius: 600,
                exclude_rid: 17,
                geo_radius: 800,
                infinite_listing: true,
                page: 1,
                page_size: 37,
                rid: [ 17 ],
                section: 'new',
            },
            newCurrentRing: { count: 68, radius: 800 },
        });
    });

    it('для следующего кольца "Россия"', () => {
        rings = [
            { count: 79, radius: 1000 },
            { count: 101, radius: 1100 },
        ];
        params.rings = rings;
        params.currentRing = rings[0];

        expect(getSearchParamsForNextPage(params)).toEqual({
            newSearchParameters: {
                catalog_filter: [ { mark: 'AUDI' } ],
                category: 'cars',
                exclude_geo_radius: 1000,
                exclude_rid: 166,
                geo_radius: undefined,
                infinite_listing: true,
                page: 1,
                page_size: 37,
                rid: [ 17 ],
                section: 'new',
            },
            newCurrentRing: rings[1],
        });
    });

    it('не должен отдать параметры если у нас уже последняя страница последнего кольца', () => {
        params.currentRing = rings[1];

        expect(getSearchParamsForNextPage(params)).toEqual({
            newSearchParameters: null,
            newCurrentRing: { count: 68, radius: 800 },
        });
    });
});

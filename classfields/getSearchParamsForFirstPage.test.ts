import type { RegionWithLinguistics } from 'auto-core/react/dataDomain/geo/StateGeo';

import type { TOfferCategory, TOfferSection } from 'auto-core/types/proto/auto/api/api_offer_model';

import type { Params } from './getSearchParamsForFirstPage';
import getSearchParamsForFirstPage from './getSearchParamsForFirstPage';

let params: Params;

const GEO_ID = {
    RF: 225,
    MSK: 213,
    SPB: 2,
    SOME_CITY: 17,
};

beforeEach(() => {
    params = {
        searchParameters: {
            catalog_filter: [ { mark: 'AUDI' } ],
            section: 'new' as TOfferSection,
            category: 'cars' as TOfferCategory,
        },
        gidsInfo: [ { id: GEO_ID.SOME_CITY } as RegionWithLinguistics ],
        geoRadius: 0,
        firstRing: { count: 52, radius: 600 },
    };
});

it('параметры для первой страницы БЛ для гео без радиуса', () => {
    expect(getSearchParamsForFirstPage(params)).toEqual({
        ...params.searchParameters,
        exclude_geo_radius: 0,
        exclude_rid: 17,
        geo_radius: 600,
        infinite_listing: true,
        rid: [ 17 ],
    });
});

it('параметры для первой страницы БЛ для гео с радиусом', () => {
    params.geoRadius = 200;

    expect(getSearchParamsForFirstPage(params)).toEqual({
        ...params.searchParameters,
        exclude_geo_radius: 200,
        exclude_rid: 17,
        geo_radius: 600,
        infinite_listing: true,
        rid: [ 17 ],
    });
});

it('параметры для первой страницы БЛ для МСК без радиуса', () => {
    params.gidsInfo[0].id = GEO_ID.MSK;

    expect(getSearchParamsForFirstPage(params)).toEqual({
        ...params.searchParameters,
        exclude_geo_radius: 25,
        exclude_rid: 213,
        geo_radius: 600,
        infinite_listing: true,
        rid: [ 213 ],
    });
});

it('параметры для первой страницы БЛ для МСК с радиусом', () => {
    params.gidsInfo[0].id = GEO_ID.MSK;
    params.geoRadius = 200;

    expect(getSearchParamsForFirstPage(params)).toEqual({
        ...params.searchParameters,
        exclude_geo_radius: 200,
        exclude_rid: 213,
        geo_radius: 600,
        infinite_listing: true,
        rid: [ 213 ],
    });
});

it('параметры для первой страницы БЛ для СПБ без радиуса', () => {
    params.gidsInfo[0].id = GEO_ID.SPB;

    expect(getSearchParamsForFirstPage(params)).toEqual({
        ...params.searchParameters,
        exclude_geo_radius: 25,
        exclude_rid: 2,
        geo_radius: 600,
        infinite_listing: true,
        rid: [ 2 ],
    });
});

it('параметры для первой страницы БЛ для СПБ с радиусом', () => {
    params.gidsInfo[0].id = GEO_ID.SPB;
    params.geoRadius = 200;

    expect(getSearchParamsForFirstPage(params)).toEqual({
        ...params.searchParameters,
        exclude_geo_radius: 200,
        exclude_rid: 2,
        geo_radius: 600,
        infinite_listing: true,
        rid: [ 2 ],
    });
});

it('параметры для первой страницы БЛ c расширением сразу до России', () => {
    params.geoRadius = 1000;
    params.firstRing = { count: 52, radius: 1100 };

    expect(getSearchParamsForFirstPage(params)).toEqual({
        ...params.searchParameters,
        exclude_geo_radius: 1000,
        exclude_rid: 166,
        geo_radius: undefined,
        infinite_listing: true,
        rid: [ 17 ],
    });
});

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResourcePublicApi: jest.fn(() => new Promise(() => {})),
        getResource: jest.fn(() => new Promise(() => {})),
    };
});

jest.mock('auto-core/react/dataDomain/breadcrumbsPublicApi/actions/fetchWithFilters', () => {
    return jest.fn(() => () => {});
});

jest.mock('auto-core/react/dataDomain/breadcrumbsPublicApi/actions/fetchForDealer', () => {
    return jest.fn(() => () => {});
});

jest.mock('auto-core/react/dataDomain/listingGeoRadiusCounters/actions/fetch', () => {
    return jest.fn(() => () => {});
});

import { cloneDeep, noop } from 'lodash';
import React from 'react';
import type { ShallowWrapper } from 'enzyme';
import { shallow } from 'enzyme';
import 'jest-enzyme';

import mockStore from 'autoru-frontend/mocks/mockStore';
import listingMock from 'autoru-frontend/mockData/state/listing';
import listingDealer from 'autoru-frontend/mockData/state/listingDealer';

import breadcrumbsPublicApiMock from 'auto-core/react/dataDomain/breadcrumbsPublicApi/mocks/breadcrumbsPublicApi.mock';
import gateApi from 'auto-core/react/lib/gateApi';
import type { TStateListing } from 'auto-core/react/dataDomain/listing/TStateListing';
import type { StateBreadcrumbsPublicApi } from 'auto-core/react/dataDomain/breadcrumbsPublicApi/types';
import type { Props as MiniProps } from 'auto-core/react/components/mobile/MiniFilter/MiniFilter';
import fetchBreadcrumbs from 'auto-core/react/dataDomain/breadcrumbsPublicApi/actions/fetchWithFilters';
import fetchBreadcrumbsForDealer from 'auto-core/react/dataDomain/breadcrumbsPublicApi/actions/fetchForDealer';

import vendorEnum from 'auto-core/data/dicts/vendorEnum.json';
import type { TSearchParameters } from 'auto-core/types/TSearchParameters';

import type { Props, State } from 'www-mobile/react/components/MMMMultiFilterCore/MMMMultiFilterCore';

import MMMMultiFilter from './MMMMultiFilterListing';

const getResourcePublicApi = gateApi.getResourcePublicApi as jest.MockedFunction<typeof gateApi.getResource>;
const getResource = gateApi.getResource as jest.MockedFunction<typeof gateApi.getResource>;

let wrapper: ShallowWrapper<Props, State>;
let store: {
    breadcrumbsPublicApi: StateBreadcrumbsPublicApi;
    listing: TStateListing;
};
beforeEach(() => {
    getResourcePublicApi.mockClear();
    getResource.mockClear();
    store = {
        breadcrumbsPublicApi: cloneDeep(breadcrumbsPublicApiMock),
        listing: cloneDeep(listingMock),
    };
});

it('должен запросить каунтер объявлений и обновить стейт компонента при изменении фильтра', () => {
    const p = Promise.resolve({ pagination: { total_offers_count: 5 } });
    getResourcePublicApi.mockImplementation(() => p);
    wrapper = shallow(
        <MMMMultiFilter
            mmmInfo={ [ {} ] }
            onChange={ noop }
            withGenerations
        />, { context: { store: mockStore(store), hasExperiment: jest.fn() } },
    ).dive();
    wrapper.find('MiniFilter').simulate('requestOpen', { index: 0, popupState: 'MARK' });
    wrapper.find('ListingMmmPopup').simulate('change', [ {} ], {});
    expect(getResourcePublicApi).toHaveBeenCalledWith(
        'listingCount',
        {
            catalog_filter: [ {} ],
            category: 'cars',
            price_to: 50000,
            section: 'all',
            sort: 'fresh_relevance_1-desc',
        },
    );
    return p.then(() => {
        expect(wrapper.state().offersCount).toBe(5);
    });
});

it('должен обновить стейт компонента при изменении пропов offersCount и mmmInfo', () => {
    const newMMM = [ { mark: 'AUDI' } ];
    wrapper = shallow(
        <MMMMultiFilter
            mmmInfo={ [ {} ] }
            onChange={ noop }
            withGenerations
        />, { context: { store: mockStore(store), hasExperiment: jest.fn() } },
    ).dive();
    wrapper.find('MiniFilter').simulate('requestOpen', { index: 0, popupState: 'MARK' });
    wrapper.find('ListingMmmPopup').simulate('change', [ {} ], {});
    wrapper.setProps({ offersCount: 2, mmmInfo: newMMM });
    expect(wrapper.state().offersCount).toBe(2);
    expect(wrapper.state().mmmInfoSelect).toEqual(newMMM);
});

it('должен перезапросить крошки при изменении МММ', () => {
    wrapper = shallow(
        <MMMMultiFilter
            mmmInfo={ [ {} ] }
            onChange={ noop }
            withGenerations
        />, { context: { store: mockStore(store), hasExperiment: jest.fn() } },
    ).dive();
    wrapper.find('MiniFilter').simulate('requestOpen', { index: 0, popupState: 'MARK' });
    wrapper.find('ListingMmmPopup').simulate('change', [ { mark: 'VAZ', models: [ { id: 'KALINA' } ] }, { mark: 'AUDI' } ], { fetchBreadcrumbs: true });
    expect(fetchBreadcrumbs).toHaveBeenCalledWith({
        catalog_filter: [ { mark: 'VAZ', model: 'KALINA' }, { mark: 'AUDI' } ],
        category: 'cars',
        price_to: 50000,
        section: 'all',
        sort: 'fresh_relevance_1-desc',
    });
});

it('должен перезапросить дилерские крошки при изменении МММ в листинге дилера', () => {
    store.listing = listingDealer;
    wrapper = shallow(
        <MMMMultiFilter
            mmmInfo={ [ {} ] }
            onChange={ noop }
            withGenerations
        />, { context: { store: mockStore(store), hasExperiment: jest.fn() } },
    ).dive();
    wrapper.find('MiniFilter').simulate('requestOpen', { index: 0, popupState: 'MARK' });
    wrapper.find('ListingMmmPopup').simulate('change', [ { mark: 'VAZ', models: [ { id: 'KALINA' } ] }, { mark: 'AUDI' } ], { fetchBreadcrumbs: true });
    expect(fetchBreadcrumbs).not.toHaveBeenCalled();
    expect(fetchBreadcrumbsForDealer).toHaveBeenCalledWith({
        catalog_filter: [ { mark: 'VAZ', model: 'KALINA' }, { mark: 'AUDI' } ],
        dealer_id: '21464512',
        dealer_code: 'plaza_sankt_peterburg',
        category: 'cars',
        section: 'all',
    });
});

it('должен перезапросить крошки при изменении МММ (исключение)', () => {
    wrapper = shallow(
        <MMMMultiFilter
            mmmInfo={ [ {} ] }
            onChange={ noop }
            withGenerations
        />, { context: { store: mockStore(store), hasExperiment: jest.fn() } },
    ).dive();
    wrapper.find('MiniFilter').simulate('requestOpen', { index: 0, popupState: 'MARK' });
    wrapper.find('ListingMmmPopup').simulate('change',
        [ { mark: 'VAZ', models: [ { id: 'KALINA' } ] }, { mark: 'AUDI', exclude: true } ],
        { fetchBreadcrumbs: true },
    );
    expect(fetchBreadcrumbs).toHaveBeenCalledWith({
        catalog_filter: [ { mark: 'VAZ', model: 'KALINA' } ],
        category: 'cars',
        exclude_catalog_filter: [ { mark: 'AUDI' } ],
        price_to: 50000,
        section: 'all',
        sort: 'fresh_relevance_1-desc',
    });
});

it('должен правильно передать info с вендором в minifilter', () => {
    wrapper = shallow(
        <MMMMultiFilter
            mmmInfo={ [ { vendor: 'VENDOR1' } ] }
            onChange={ noop }
            withGenerations
        />, { context: { store: mockStore(store), hasExperiment: jest.fn() } },
    ).dive();
    expect((wrapper.find('MiniFilter') as ShallowWrapper<MiniProps>).props().info)
        .toEqual({ vendor: vendorEnum.RU_VENDOR });
});

it('должен правильно передать info с маркой в minifilter', () => {
    wrapper = shallow(
        <MMMMultiFilter
            mmmInfo={ [ { mark: 'FORD' } ] }
            onChange={ noop }
            withGenerations
        />, { context: { store: mockStore(store), hasExperiment: jest.fn() } },
    ).dive();
    expect((wrapper.find('MiniFilter') as ShallowWrapper<MiniProps>).props().info.mark)
        .toMatchObject({ name: 'Ford' });
});

it('при изменении пропов должен отсортировать ммм в стейте в том же порядке, как у предыдущего', () => {
    wrapper = shallow(
        <MMMMultiFilter
            mmmInfo={ [ { mark: 'AUDI' } ] }
            onChange={ noop }
            withGenerations
        />, { context: { store: mockStore(store), hasExperiment: jest.fn() } },
    ).dive();
    wrapper.setProps({ mmmInfo: [ { mark: 'BMW' }, { mark: 'AUDI' } ] });
    expect(wrapper.state().mmmInfo).toEqual([ { mark: 'AUDI' }, { mark: 'BMW' } ]);
    expect(wrapper.state().propsMmmInfo).toEqual([ { mark: 'BMW' }, { mark: 'AUDI' } ]);
});

it.each([
    [ 'cars', true ],
    [ 'moto', false ],
    [ 'trucks', false ],
])('должен нарисовать список марок с вендорами для %s', (category: string, expected: boolean) => {
    store.listing.data.search_parameters.category = category as TSearchParameters['category'];
    wrapper = shallow(
        <MMMMultiFilter
            mmmInfo={ [ {} ] }
            onChange={ noop }
            withGenerations
        />, { context: { store: mockStore(store), hasExperiment: jest.fn() } },
    ).dive();
    wrapper.find('MiniFilter').simulate('requestOpen', { index: 0, popupState: 'MARK' });

    expect(wrapper.find('ListingMmmPopup')).toHaveProp('renderVendors', expected);
});

it('должен нарисовать список марок без вендоров для поиска по дилеру', () => {
    store.listing.data.search_parameters.dealer_id = '123';
    wrapper = shallow(
        <MMMMultiFilter
            mmmInfo={ [ {} ] }
            onChange={ noop }
            withGenerations
        />, { context: { store: mockStore(store), hasExperiment: jest.fn() } },
    ).dive();
    wrapper.find('MiniFilter').simulate('requestOpen', { index: 0, popupState: 'MARK' });

    expect(wrapper.find('ListingMmmPopup')).toHaveProp('renderVendors', false);
});

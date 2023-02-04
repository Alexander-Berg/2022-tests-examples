/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/dataDomain/listing/actions/setSearchParameters', () => {
    return jest.fn((...args) => ({ type: '_action_setSearchParameters', args }));
});
jest.mock('auto-core/react/dataDomain/breadcrumbsPublicApi/actions/fetchWithFilters', () => {
    const functionToTest = () => async(dispatch: any) => {
        dispatch(jest.fn());
    };
    return functionToTest;
});
jest.mock('auto-core/react/dataDomain/breadcrumbsPublicApi/actions/fetchForDealer', () => {
    const functionToTest = () => async(dispatch: any) => {
        dispatch(jest.fn());
    };
    return functionToTest;
});
jest.mock('auto-core/react/dataDomain/listing/actions/fetchCount', () => {
    const functionToTest = () => async(dispatch: any) => {
        dispatch(jest.fn());
    };
    return functionToTest;
});
jest.mock('auto-core/react/dataDomain/listingGeoRadiusCounters/actions/fetch', () => {
    const functionToTest = () => async(dispatch: any) => {
        dispatch(jest.fn());
    };
    return functionToTest;
});
jest.mock('auto-core/react/dataDomain/config/actions/updateData', () => {
    return jest.fn().mockReturnValue(jest.fn);
});

jest.mock('auto-core/lib/util/asyncDebounce', () => ({
    __esModule: true,
    'default': (func: any) => func,
}));

import { cloneDeep, noop } from 'lodash';
import React from 'react';
import type { ShallowWrapper } from 'enzyme';
import { shallow } from 'enzyme';

import mockStore from 'autoru-frontend/mocks/mockStore';
import contextMock from 'autoru-frontend/mocks/contextMock';
import flushPromises from 'autoru-frontend/jest/unit/flushPromises';
import listingMock from 'autoru-frontend/mockData/state/listing';

import breadcrumbsPublicApiMock from 'auto-core/react/dataDomain/breadcrumbsPublicApi/mocks/breadcrumbsPublicApi.mock';
import setSearchParameters from 'auto-core/react/dataDomain/listing/actions/setSearchParameters';
import updateData from 'auto-core/react/dataDomain/config/actions/updateData';
import type { StateUser } from 'auto-core/react/dataDomain/user/types';
import type { StateGeo } from 'auto-core/react/dataDomain/geo/StateGeo';
import type { TStateListing } from 'auto-core/react/dataDomain/listing/TStateListing';
import type { StateBreadcrumbsPublicApi } from 'auto-core/react/dataDomain/breadcrumbsPublicApi/types';
import geoMock from 'auto-core/react/dataDomain/geo/mocks/geo.mock';
import userMock from 'auto-core/react/dataDomain/user/mocks/withAuth.mock';

import ListingFiltersPopup from './ListingFiltersPopup';
import type { Props, State } from './ListingFiltersPopup';

let store: {
    breadcrumbsPublicApi: StateBreadcrumbsPublicApi;
    geo: StateGeo;
    listing: TStateListing;
    user: StateUser;
};
beforeEach(() => {
    store = {
        breadcrumbsPublicApi: cloneDeep(breadcrumbsPublicApiMock),
        geo: cloneDeep(geoMock),
        listing: cloneDeep(listingMock),
        user: userMock as unknown as StateUser,
    };
});

afterEach(() => {
    jest.resetModules();
});

let wrapper: ShallowWrapper<Props, State>;

it('должен запросить обновить стейт при изменении фильтра ммм', () => {
    wrapper = shallow(
        <ListingFiltersPopup
            mmmInfo={ [ {} ] }
            onRequestHide={ noop }
            onSubmit={ noop }
            showAddMmm={ false }
            searchTagsDictionary={ [] }
            withGenerations
        />, { context: { ...contextMock, store: mockStore(store) } },
    ).dive();
    wrapper.find('Connect(MMMMultiFilterCore)').simulate('change', {}, 5);
    expect(wrapper.state().offersCount).toBe(5);
});

it('должен обновить стейт компонента при изменении пропов offersCount и mmmInfo', () => {
    const newMMM = [ { mark: 'AUDI' } ];
    wrapper = shallow(
        <ListingFiltersPopup
            mmmInfo={ [ {} ] }
            onRequestHide={ noop }
            onSubmit={ noop }
            showAddMmm={ false }
            searchTagsDictionary={ [] }
            withGenerations
        />, { context: { ...contextMock, store: mockStore(store) } },
    ).dive();
    wrapper.find('Connect(MMMMultiFilterCore)').simulate('change', {}, 5);
    wrapper.setProps({ offersCount: 2, mmmInfo: newMMM });
    expect(wrapper.state().offersCount).toBe(2);
    expect(wrapper.state().mmmInfoSelect).toEqual(newMMM);
});

it('должен нарисовать фильтр категорий для фильтров листинга', () => {
    store.listing.data.search_parameters = {
        section: 'all',
        category: 'cars',
    };
    wrapper = shallow(
        <ListingFiltersPopup
            mmmInfo={ [ {} ] }
            onRequestHide={ noop }
            onSubmit={ noop }
            showAddMmm={ false }
            searchTagsDictionary={ [] }
            withGenerations
        />, { context: { ...contextMock, store: mockStore(store) } },
    ).dive();

    expect(wrapper.find('FilterCategorySelector')).toHaveLength(1);
});

it('не должен нарисовать фильтр категорий для фильтров дилера', () => {
    store.listing.data.search_parameters = {
        dealer_code: 'code',
        section: 'all',
        category: 'cars',
    };
    wrapper = shallow(
        <ListingFiltersPopup
            mmmInfo={ [ {} ] }
            onRequestHide={ noop }
            onSubmit={ noop }
            showAddMmm={ false }
            searchTagsDictionary={ [] }
            withGenerations
        />, { context: { ...contextMock, store: mockStore(store) } },
    ).dive();

    expect(wrapper.find('FilterCategorySelector')).toHaveLength(0);
});

it('должен сбросить все параметры при смене категории', async() => {
    store.listing.data.search_parameters = {
        price_to: 50000,
        catalog_filter: [
            { mark: 'VAZ', model: 'KALINA' },
        ],
        section: 'all',
        category: 'cars',
        sort: 'fresh_relevance_1-desc',
    };
    wrapper = shallow(
        <ListingFiltersPopup
            mmmInfo={ [ {} ] }
            onRequestHide={ noop }
            onSubmit={ noop }
            showAddMmm={ false }
            searchTagsDictionary={ [] }
            withGenerations
        />, { context: { ...contextMock, store: mockStore(store) } },
    ).dive();

    wrapper.find('FilterCategorySelector').simulate('change', { category: 'cars' });
    await flushPromises();
    expect(contextMock.pushState).toHaveBeenCalledWith('link/listing/?category=cars');
});

it('должен построить ссылку на листинг, если фильтр открыт из листинга и обновить ссылку для перехода на десктоп', async() => {
    wrapper = shallow(
        <ListingFiltersPopup
            mmmInfo={ [ {} ] }
            onRequestHide={ noop }
            onSubmit={ noop }
            showAddMmm={ false }
            searchTagsDictionary={ [] }
            withGenerations
        />, { context: { ...contextMock, store: mockStore(store) } },
    ).dive();

    wrapper.find('ListingFiltersCars').simulate('change', 100000, { name: 'price_to' });
    await flushPromises();
    expect(contextMock.pushState).toHaveBeenCalledWith('link/listing/?price_to=100000&catalog_filter=mark%3DVAZ%2Cmodel%3DKALINA&section=all&category=cars');
    expect(updateData).toHaveBeenCalledWith({
        desktopUrl: 'linkDesktop/listing/?price_to=100000&catalog_filter=mark%3DVAZ%2Cmodel%3DKALINA&section=all&category=cars&nomobile=true',
    });
});

it('должен построить ссылку на страницу дилера, если фильтр открыт из страницы дилера', async() => {
    store.listing.data.search_parameters.dealer_code = 'code';
    store.listing.data.search_parameters.dealer_id = '12345';
    wrapper = shallow(
        <ListingFiltersPopup
            mmmInfo={ [ {} ] }
            onRequestHide={ noop }
            onSubmit={ noop }
            routeName="dealer-page"
            showAddMmm={ false }
            searchTagsDictionary={ [] }
            withGenerations
        />, { context: { ...contextMock, store: mockStore(store) } },
    ).dive();

    wrapper.find('ListingFiltersCars').simulate('change', 100000, { name: 'price_to' });
    await flushPromises();
    expect(contextMock.pushState).toHaveBeenCalledWith(
        'link/dealer-page/?price_to=100000&catalog_filter=mark%3DVAZ%2Cmodel%3DKALINA&section=all&category=cars&dealer_code=code&dealer_id=12345',
    );
});

it('не должен менять урл, если фильтр открыт с главной', async() => {
    wrapper = shallow(
        <ListingFiltersPopup
            mmmInfo={ [ {} ] }
            onRequestHide={ noop }
            onSubmit={ noop }
            routeName="index"
            showAddMmm={ false }
            searchTagsDictionary={ [] }
            withGenerations
        />, { context: { ...contextMock, store: mockStore(store) } },
    ).dive();

    wrapper.find('ListingFiltersCars').simulate('change', 100000, { name: 'price_to' });
    await flushPromises();
    expect(contextMock.pushState).toHaveBeenCalledTimes(0);
    expect(contextMock.replaceState).toHaveBeenCalledTimes(0);
});

it('должен отображаться список марок при пустом ммм', () => {
    wrapper = shallow(
        <ListingFiltersPopup
            mmmInfo={ [ {} ] }
            onRequestHide={ noop }
            onSubmit={ noop }
            showAddMmm={ false }
            searchTagsDictionary={ [] }
            withGenerations
        />, { context: { ...contextMock, store: mockStore(store) } },
    ).dive();
    expect(wrapper.find('.ListingFiltersPopup__mark')).not.toHaveProperty('style');
    expect(wrapper.find('.ListingFiltersPopup__mmm').prop('style')).toEqual({ display: 'none' });
});

it('не должен отображаться список марок, если есть марка', () => {
    wrapper = shallow(
        <ListingFiltersPopup
            mmmInfo={ [ { mark: 'AUDI' } ] }
            onRequestHide={ noop }
            onSubmit={ noop }
            showAddMmm={ false }
            searchTagsDictionary={ [] }
            withGenerations
        />, { context: { ...contextMock, store: mockStore(store) } },
    ).dive();
    expect(wrapper.find('.ListingFiltersPopup__mmm')).not.toHaveProperty('style');
    expect(wrapper.find('.ListingFiltersPopup__mark').prop('style')).toEqual({ display: 'none' });
});

it('не должен отображаться список марок, если есть вендор', () => {
    wrapper = shallow(
        <ListingFiltersPopup
            mmmInfo={ [ { vendor: 'VENDOR1' } ] }
            onRequestHide={ noop }
            onSubmit={ noop }
            showAddMmm={ false }
            searchTagsDictionary={ [] }
            withGenerations
        />, { context: { ...contextMock, store: mockStore(store) } },
    ).dive();
    expect(wrapper.find('.ListingFiltersPopup__mmm')).not.toHaveProperty('style');
    expect(wrapper.find('.ListingFiltersPopup__mark').prop('style')).toEqual({ display: 'none' });
});

describe('закрытие фильтра и сброс параметров', () => {
    it('должен сбросить параметры при закрытии, если они поменялись', () => {
        wrapper = shallow(
            <ListingFiltersPopup
                mmmInfo={ [ {} ] }
                onRequestHide={ noop }
                onSubmit={ noop }
                routeName="index"
                showAddMmm={ false }
                searchTagsDictionary={ [] }
                withGenerations
            />, { context: { ...contextMock, store: mockStore(store) } },
        ).dive();

        const expectedInitialParams = {
            ...cloneDeep(wrapper.state('initialSearchParameters')),
            price_to: 100000,
        };

        // делаем вид, что у нас другие searchParameters (это проще, чем менять стор)
        wrapper.setState({
            initialSearchParameters: expectedInitialParams,
        });
        wrapper.find('FiltersPopup').simulate('close');

        expect(setSearchParameters).toHaveBeenCalledTimes(1);
        expect(setSearchParameters).toHaveBeenCalledWith(expectedInitialParams);
    });

    it('не должен сбросить параметры при закрытии, если они не поменялись', () => {
        wrapper = shallow(
            <ListingFiltersPopup
                mmmInfo={ [ {} ] }
                onRequestHide={ noop }
                onSubmit={ noop }
                routeName="index"
                showAddMmm={ false }
                searchTagsDictionary={ [] }
                withGenerations
            />, { context: { ...contextMock, store: mockStore(store) } },
        ).dive();

        wrapper.find('FiltersPopup').simulate('close');

        expect(setSearchParameters).toHaveBeenCalledTimes(0);
    });
});

it('должен обновить урл при сабмите фильтра', async() => {
    wrapper = shallow(
        <ListingFiltersPopup
            mmmInfo={ [ {} ] }
            onRequestHide={ noop }
            onSubmit={ noop }
            routeName="index"
            showAddMmm={ false }
            searchTagsDictionary={ [] }
            withGenerations
        />, { context: { ...contextMock, store: mockStore(store) } },
    ).dive();

    wrapper.find('FiltersPopup').simulate('done');
    await flushPromises();
    expect(contextMock.pushState).toHaveBeenCalledTimes(1);
    expect(contextMock.replaceState).toHaveBeenCalledTimes(0);
});

describe('ABT_VS_678_PESSIMIZATION_BEATEN', () => {
    it('должен построить ссылку без дефолтных параметров экспа ABT_VS_678_PESSIMIZATION_BEATEN', async() => {
        const context = {
            ...contextMock,
            hasExperiment: (exp: string) => exp === 'ABT_VS_678_PESSIMIZATION_BEATEN',
            store: mockStore(store),
        };
        store.listing.data.search_parameters.damage_group = 'ANY';
        store.listing.data.search_parameters.customs_state_group = 'DOESNT_MATTER';
        store.listing.data.search_parameters.category = 'cars';
        store.listing.data.search_parameters.section = 'used';

        wrapper = shallow(
            <ListingFiltersPopup
                mmmInfo={ [ {} ] }
                onRequestHide={ noop }
                onSubmit={ noop }
                routeName="listing"
                showAddMmm={ false }
                searchTagsDictionary={ [] }
                withGenerations
            />, { context },
        ).dive();

        wrapper.find('ListingFiltersCars').simulate('change', 100000, { name: 'price_to' });
        await flushPromises();
        expect(contextMock.pushState).toHaveBeenCalledWith(
            'link/listing/?price_to=100000&catalog_filter=mark%3DVAZ%2Cmodel%3DKALINA&section=used&category=cars',
        );
    });

    it('должен построить со всеми параметрами вне экспа ABT_VS_678_PESSIMIZATION_BEATEN', async() => {
        const context = {
            ...contextMock,
            store: mockStore(store),
        };
        store.listing.data.search_parameters.damage_group = 'ANY';
        store.listing.data.search_parameters.customs_state_group = 'DOESNT_MATTER';
        store.listing.data.search_parameters.category = 'cars';
        store.listing.data.search_parameters.section = 'used';

        wrapper = shallow(
            <ListingFiltersPopup
                mmmInfo={ [ {} ] }
                onRequestHide={ noop }
                onSubmit={ noop }
                routeName="listing"
                showAddMmm={ false }
                searchTagsDictionary={ [] }
                withGenerations
            />, { context },
        ).dive();

        wrapper.find('ListingFiltersCars').simulate('change', 100000, { name: 'price_to' });
        await flushPromises();
        expect(contextMock.pushState).toHaveBeenCalledWith(
            // eslint-disable-next-line max-len
            'link/listing/?price_to=100000&catalog_filter=mark%3DVAZ%2Cmodel%3DKALINA&section=used&category=cars&damage_group=ANY&customs_state_group=DOESNT_MATTER',
        );
    });
});

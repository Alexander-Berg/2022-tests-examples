import React from 'react';
import { shallow } from 'enzyme';

import type { ThunkMockStore } from 'autoru-frontend/mocks/mockStore';
import mockStore from 'autoru-frontend/mocks/mockStore';
import contextMock from 'autoru-frontend/mocks/contextMock';

import type {
    TBreadcrumbsGeneration,
    TBreadcrumbsGenerationLevel,
    TBreadcrumbsMark,
    TBreadcrumbsMarkLevel,
    TBreadcrumbsModel,
    TBreadcrumbsModelLevel,
} from 'auto-core/types/TBreadcrumbs';
import { TBreadcrumbsLevelId } from 'auto-core/types/TBreadcrumbs';
import type { TOfferListing } from 'auto-core/types/TOfferListing';

import type { ReduxState } from './ListingTitle';
import ListingTitle from './ListingTitle';

let store: ThunkMockStore<ReduxState>;
let state: ReduxState;
beforeEach(() => {
    state = {
        breadcrumbsPublicApi: {
            data: [
                {
                    meta_level: TBreadcrumbsLevelId.MARK_LEVEL,
                    entities: [
                        {
                            id: 'AUDI',
                            name: 'Audi',
                        } as TBreadcrumbsMark,
                    ],
                } as TBreadcrumbsMarkLevel,
                {
                    meta_level: TBreadcrumbsLevelId.MODEL_LEVEL,
                    mark: { id: 'AUDI', name: 'Audi' },
                    entities: [
                        {
                            id: 'A5',
                            name: 'A5',
                            nameplates: [
                                {
                                    name: 'g-tron',
                                    semantic_url: 'g_tron',
                                },
                            ],
                        } as TBreadcrumbsModel,
                    ],
                } as TBreadcrumbsModelLevel,
                {
                    meta_level: TBreadcrumbsLevelId.GENERATION_LEVEL,
                    mark: { id: 'AUDI', name: 'Audi' },
                    model: { id: 'A5', name: 'A5' },
                    entities: [
                        {
                            id: '21745628',
                            name: 'II (F5) Рестайлинг',
                        } as TBreadcrumbsGeneration,
                    ],
                } as TBreadcrumbsGenerationLevel,
            ],
            status: 'SUCCESS',
        },
        listing: {
            data: {
                search_parameters: {
                    catalog_filter: [
                        {
                            mark: 'AUDI',
                            model: 'A5',
                            nameplate_name: 'g_tron',
                            generation: '21745628',
                        },
                    ],
                    section: 'all',
                    category: 'cars',
                },
            } as TOfferListing,
            searchID: '',
        },
    };
});

it('должен вывести марку, модель + шильд, поколение', () => {
    store = mockStore(state);

    const tree = shallow(
        <ListingTitle className="myClass"/>,
        { context: { ...contextMock, store } },
    ).dive();
    expect(tree.find('div.myClass').text()).toMatchSnapshot();
});

it('должен вывести марку и шильд', () => {
    state = {
        listing: {
            data: {
                search_parameters: {
                    catalog_filter: [
                        { mark: 'DODGE', model: 'CARAVAN', nameplate_name: 'grand_caravan' },
                    ],
                    category: 'cars',
                },
            } as TOfferListing,
            searchID: '',
        },
        breadcrumbsPublicApi: {
            data: [
                {
                    meta_level: TBreadcrumbsLevelId.MARK_LEVEL,
                    entities: [
                        {
                            id: 'DODGE',
                            name: 'Dodge',
                        } as TBreadcrumbsMark,
                    ],
                } as TBreadcrumbsMarkLevel,
                {
                    meta_level: TBreadcrumbsLevelId.MODEL_LEVEL,
                    mark: { id: 'DODGE', name: 'Dodge' },
                    entities: [
                        {
                            id: 'CARAVAN',
                            name: 'Caravan',
                            nameplates: [
                                {
                                    name: 'Grand Caravan',
                                    semantic_url: 'grand_caravan',
                                    no_model: true,
                                },
                            ],
                        } as TBreadcrumbsModel,
                    ],
                } as TBreadcrumbsModelLevel,
            ],
            status: 'SUCCESS',
        },
    };
    store = mockStore(state);

    const tree = shallow(
        <ListingTitle className="myClass"/>,
        { context: { ...contextMock, store } },
    ).dive();
    expect(tree.find('div.myClass').text()).toMatchSnapshot();
});

it('должен вывести "Легковые автомобили" для категории "авто", если нет поисковых параметров', () => {
    state.listing.data.search_parameters = { category: 'cars' };
    store = mockStore(state);

    const tree = shallow(
        <ListingTitle className="myClass"/>,
        { context: { ...contextMock, store } },
    ).dive();
    expect(tree.find('div.myClass').text()).toMatchSnapshot();
});

it('должен вывести название категории для мото, если нет поисковых параметров', () => {
    state.listing.data.search_parameters = { category: 'moto', moto_category: 'ATV' };
    store = mockStore(state);

    const tree = shallow(
        <ListingTitle className="myClass"/>,
        { context: { ...contextMock, store } },
    ).dive();
    expect(tree.find('div.myClass').text()).toMatchSnapshot();
});

it('должен вывести название категории для коммерческих, если нет поисковых параметров', () => {
    state.listing.data.search_parameters = { category: 'trucks', trucks_category: 'LCV' };
    store = mockStore(state);

    const tree = shallow(
        <ListingTitle className="myClass"/>,
        { context: { ...contextMock, store } },
    ).dive();
    expect(tree.find('div.myClass').text()).toMatchSnapshot();
});

it('должен вывести название с ценой, если есть параметр do без МММ', () => {
    state.listing.data.search_parameters = { category: 'cars', 'do': 500000 };
    store = mockStore(state);

    const tree = shallow(
        <ListingTitle className="myClass"/>,
        { context: { ...contextMock, store } },
    ).dive();
    expect(tree.find('h1.myClass').text()).toMatchSnapshot();
});

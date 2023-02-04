import 'jest-enzyme';
import { shallow } from 'enzyme';
import React from 'react';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import ListingHead from './ListingHead';

const store = mockStore({
    bunker: {
        'banners/index-marketing-banners': {
            electro: {
                inUse: true,
            },
        },
    },
    listing: {
        data: {
            search_parameters: {
                catalog_filter: [ { mark: 'ALPINA', model: 'SHERPA' } ],
                category: 'moto',
                moto_category: 'SNOWMOBILE',
                section: 'all',
            },
        },
    },
    breadcrumbsPublicApi: {
        data: [
            {
                entities: [ {
                    count: 1,
                    id: 'SHERPA',
                    itemFilterParams: { model: 'SHERPA' },
                    name: 'Sherpa',
                    nameplates: [],
                    reviews_count: 0,
                } ],
                level: 'MODEL_LEVEL',
                levelFilterParams: { mark: 'ALPINA' },
                mark: {
                    id: 'ALPINA',
                    logo: {
                        name: 'mark-logo',
                        sizes: {
                            'black-logo': '//avatars.mds.yandex.net/get-verba/1540742/2a0000017a13f86b3fb51f39265b8dd61242/logo',
                            logo: '//avatars.mds.yandex.net/get-verba/787013/2a0000016090425a48d4ab1a6c39dc3dab2d/logo',
                        },
                    },
                    name: 'Alpina',
                },
                meta_level: 'MODEL_LEVEL',
            },
            {
                entities: [ {
                    count: 1,
                    id: 'ALPINA',
                    itemFilterParams: { mark: 'ALPINA' },
                    logo: '//avatars.mds.yandex.net/get-verba/787013/2a0000016090425a48d4ab1a6c39dc3dab2d/logo',
                    name: 'Alpina',
                    numeric_id: null,
                    popular: false,
                    reviews_count: 0,
                } ],
                level: 'MARK_LEVEL',
                levelFilterParams: {},
                meta_level: 'MARK_LEVEL',
            },
        ],
    },
});

it('отрендерилась ссылка с категорией Moto Snowmobile', () => {
    const searchParams = {
        catalog_filter: [ { mark: 'ALPINA', model: 'SHERPA' } ],
        category: 'moto',
        moto_category: 'SNOWMOBILE',
        section: 'all',
    };
    const context = contextMock;
    const wrapper = shallow(
        <ListingHead
            searchParams={ searchParams }
        />,
        { context: { ...context, store } },
    ).dive();
    expect((wrapper.find('.ListingHead__H1breadcrumbs').dive().dive().find('Link').props() as { url: string }).url)
        .toBe('link/moto-listing/?category=&moto_category=SNOWMOBILE&trucks_category=&section=all&mark=ALPINA');
});

it('отрендерилась ссылка с категорией Commercial и LCV', () => {
    const searchParams = {
        catalog_filter: [ { mark: 'UAZ', model: 'PROFI_2RYADNAYA_KABINA' } ],
        category: 'trucks',
        section: 'all',
        trucks_category: 'LCV',
    };
    const context = contextMock;
    const store = mockStore({
        listing: {
            data: {
                search_parameters: {
                    catalog_filter: [ { mark: 'UAZ', model: 'PROFI_2RYADNAYA_KABINA' } ],
                    category: 'trucks',
                    section: 'all',
                    trucks_category: 'LCV',
                },
            },
        },
        breadcrumbsPublicApi: {
            data: [
                {
                    entities: [ {
                        count: 1,
                        id: 'PROFI_2RYADNAYA_KABINA',
                        itemFilterParams: { model: 'PROFI_2RYADNAYA_KABINA' },
                        name: 'Профи (двухрядная кабина)',
                        nameplates: [],
                        reviews_count: 0,
                    } ],
                    level: 'MODEL_LEVEL',
                    levelFilterParams: { mark: 'UAZ' },
                    mark: {
                        id: 'UAZ',
                        logo: {
                            name: 'mark-logo',
                            sizes: {
                                'black-logo': '//avatars.mds.yandex.net/get-verba/1540742/2a0000017a13f86b3fb51f39265b8dd61242/logo',
                                logo: '//avatars.mds.yandex.net/get-verba/787013/2a0000016090425a48d4ab1a6c39dc3dab2d/logo',
                            },
                        },
                        name: 'УАЗ',
                    },
                    meta_level: 'MODEL_LEVEL',
                },
                {
                    entities: [ {
                        count: 37,
                        id: 'UAZ',
                        itemFilterParams: { mark: 'UAZ' },
                        logo: '//avatars.mds.yandex.net/get-verba/937147/2a0000017beb1fc7a6e87cd0e3eb4c2e5e43/logo',
                        name: 'УАЗ',
                        numeric_id: null,
                        popular: true,
                        reviews_count: 297,
                    } ],
                    level: 'MARK_LEVEL',
                    levelFilterParams: {},
                    meta_level: 'MARK_LEVEL',
                },
            ],
        },
    });
    const wrapper = shallow(
        <ListingHead
            searchParams={ searchParams }
        />,
        { context: { ...context, store } },
    ).dive();
    expect((wrapper.find('.ListingHead__H1breadcrumbs').dive().dive().find('Link').props() as { url: string }).url)
        .toBe('link/commercial-listing/?category=&moto_category=&trucks_category=LCV&section=all&mark=UAZ');
});

it('должен показать баннер раздела электро, если выбран двигатель электро, гибрид', () => {
    const searchParams = {
        engine_group: [ 'ELECTRO', 'HYBRID' ],
    };

    const wrapper = shallow(
        <ListingHead
            searchParams={ searchParams }
        />,
        { context: { ...contextMock, store } },
    ).dive();

    expect(wrapper.find('SmallElectroBanner')).not.toBeNull();
});

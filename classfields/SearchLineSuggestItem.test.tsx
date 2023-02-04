import React from 'react';
import type { ShallowWrapper } from 'enzyme';
import { shallow } from 'enzyme';
import { shallowToJson } from 'enzyme-to-json';

import { Category } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';
import type { Photo, RegionInfo } from '@vertis/schema-registry/ts-types-snake/auto/api/common_model';
import type { Mark, Model, SuperGeneration } from '@vertis/schema-registry/ts-types-snake/auto/api/catalog_model';
import type { SavedSearchView } from '@vertis/schema-registry/ts-types-snake/auto/api/searches_model';

import contextMock from 'autoru-frontend/mocks/contextMock';

import statApi from 'auto-core/lib/event-log/statApi';

import type { TSearchLineSuggestItem } from 'auto-core/types/TSearchLineSuggest';

import type { Props } from './SearchLineSuggestItem';
import SearchLineSuggestItem from './SearchLineSuggestItem';

jest.mock('auto-core/lib/event-log/statApi');

let item: TSearchLineSuggestItem;
let statProps: Omit<Props, 'item'>;
beforeEach(() => {
    statProps = {
        position: 2,
        query: 'ауди или бмв',
        queryId: 'abc123',
    };
});

describe('есть марка', () => {
    it('должен отрисовать логотип, марку на первой строке, категорию на второй строке', () => {
        item = {
            category: Category.CARS,
            params: {
                catalog_filter: [ { mark: 'AUDI' } ],
                section: 'all',
            },
            paramsDescription: {
                moreCount: 0,
                paramsInfo: [],
            },
            subcategoryHumanName: 'Автомобили',
            url: 'https://auto.ru/samara/cars/all/?query=%D1%81%D0%B0%D0%BC%D0%B0%D1%80%D0%B0',
            view: {
                mark_model_nameplate_gen_views: [
                    {
                        mark: {
                            code: 'AUDI',
                            name: 'Audi',
                            ru_name: 'Ауди',
                            logo: {
                                sizes: {
                                    logo: '//avatars.mds.yandex.net/get-verba/997355/2a000001651f4baeb0ee7d9d292ce0db5e9a/logo',
                                },
                            } as Partial<Photo> as Photo,
                        } as Mark,
                    },
                ],
            } as SavedSearchView,
        } as Partial<TSearchLineSuggestItem> as TSearchLineSuggestItem;

        const tree = shallow(
            <SearchLineSuggestItem item={ item } { ...statProps }/>,
        );

        expect(shallowToJson(tree)).toMatchSnapshot();
    });

    it('должен отрисовать логотип, марку на первой строке, категорию и параметры на второй строке', () => {
        item = {
            category: Category.CARS,
            params: {
                catalog_filter: [ { mark: 'AUDI' } ],
                price_to: 500000,
                section: 'all',
            },
            paramsDescription: {
                moreCount: 0,
                paramsInfo: [
                    { label: 'Цена', val: 'до 500 000 ₽' },
                ],
            },
            subcategoryHumanName: 'Автомобили',
            url: 'https://auto.ru/samara/cars/all/?query=%D1%81%D0%B0%D0%BC%D0%B0%D1%80%D0%B0',
            view: {
                mark_model_nameplate_gen_views: [
                    {
                        mark: {
                            code: 'AUDI',
                            name: 'Audi',
                            ru_name: 'Ауди',
                            logo: {
                                sizes: {
                                    logo: '//avatars.mds.yandex.net/get-verba/997355/2a000001651f4baeb0ee7d9d292ce0db5e9a/logo',
                                },
                            } as Partial<Photo> as Photo,
                        } as Mark,
                    },
                ],
            } as SavedSearchView,
        } as Partial<TSearchLineSuggestItem> as TSearchLineSuggestItem;

        const tree = shallow(
            <SearchLineSuggestItem item={ item } { ...statProps }/>,
        );

        expect(shallowToJson(tree)).toMatchSnapshot();
    });

    it('должен отрисовать логотип, марку на первой строке, категорию, параметры и город на второй строке', () => {
        item = {
            category: Category.CARS,
            params: {
                geo_id: [ 51 ],
                catalog_filter: [ { mark: 'AUDI' } ],
                price_to: 500000,
                section: 'all',
            },
            paramsDescription: {
                moreCount: 0,
                paramsInfo: [
                    { label: 'Цена', val: 'до 500 000 ₽' },
                ],
            },
            subcategoryHumanName: 'Автомобили',
            url: 'https://auto.ru/samara/cars/all/?query=%D1%81%D0%B0%D0%BC%D0%B0%D1%80%D0%B0',
            view: {
                mark_model_nameplate_gen_views: [
                    {
                        mark: {
                            code: 'AUDI',
                            name: 'Audi',
                            ru_name: 'Ауди',
                            logo: {
                                sizes: {
                                    logo: '//avatars.mds.yandex.net/get-verba/997355/2a000001651f4baeb0ee7d9d292ce0db5e9a/logo',
                                },
                            } as Partial<Photo> as Photo,
                        } as Mark,
                    },
                ],
                regions: [
                    { id: '51', name: 'Самара' } as RegionInfo,
                ],
            } as SavedSearchView,
        } as Partial<TSearchLineSuggestItem> as TSearchLineSuggestItem;

        const tree = shallow(
            <SearchLineSuggestItem item={ item } { ...statProps }/>,
        );

        expect(shallowToJson(tree)).toMatchSnapshot();
    });

    it('должен составить правильный title, если есть марка и модель', () => {
        item = {
            category: Category.CARS,
            params: {
                catalog_filter: [ { mark: 'FORD', model: 'FOCUS' } ],
                section: 'all',
            },
            paramsDescription: {
                moreCount: 0,
                paramsInfo: [],
            },
            subcategoryHumanName: 'Автомобили',
            url: 'https://auto.ru/samara/cars/all/?query=%D1%81%D0%B0%D0%BC%D0%B0%D1%80%D0%B0',
            view: {
                mark_model_nameplate_gen_views: [
                    {
                        mark: {
                            code: 'FORD',
                            name: 'Ford',
                        } as Mark,
                        model: {
                            code: 'FOCUS',
                            name: 'Focus',
                        } as Model,
                    },
                ],
            } as SavedSearchView,
        } as Partial<TSearchLineSuggestItem> as TSearchLineSuggestItem;

        const tree = shallow(
            <SearchLineSuggestItem item={ item } { ...statProps }/>,
        );

        expect(tree.find('.SearchLineSuggestItem__title')).toHaveText('Ford Focus');
    });

    it('должен составить правильный title, если есть марка, модель и поколение', () => {
        item = {
            category: Category.CARS,
            params: {
                catalog_filter: [ { mark: 'FORD', model: 'FOCUS' } ],
                section: 'all',
            },
            paramsDescription: {
                moreCount: 0,
                paramsInfo: [],
            },
            subcategoryHumanName: 'Автомобили',
            url: 'https://auto.ru/samara/cars/all/?query=%D1%81%D0%B0%D0%BC%D0%B0%D1%80%D0%B0',
            view: {
                mark_model_nameplate_gen_views: [
                    {
                        mark: {
                            code: 'FORD',
                            name: 'Ford',
                        } as Mark,
                        model: {
                            code: 'FOCUS',
                            name: 'Focus',
                        } as Model,
                        super_gen: {
                            id: '3480337',
                            name: 'II',
                        } as SuperGeneration,
                    },
                ],
            } as SavedSearchView,
        } as Partial<TSearchLineSuggestItem> as TSearchLineSuggestItem;

        const tree = shallow(
            <SearchLineSuggestItem item={ item } { ...statProps }/>,
        );

        expect(tree.find('.SearchLineSuggestItem__title')).toHaveText('Ford Focus II');
    });
});

describe('нет марки', () => {
    it('должен отрисовать логотип категории, название категории в одну строку, если нет параметров', () => {
        item = {
            category: Category.CARS,
            params: {
                geo_id: [ 51 ],
                section: 'all',
            },
            paramsDescription: {
                moreCount: 0,
                paramsInfo: [],
            },
            subcategoryHumanName: 'Автомобили',
            url: 'https://auto.ru/samara/cars/all/?query=%D1%81%D0%B0%D0%BC%D0%B0%D1%80%D0%B0',
        } as Partial<TSearchLineSuggestItem> as TSearchLineSuggestItem;

        const tree = shallow(
            <SearchLineSuggestItem item={ item } { ...statProps }/>,
        );

        expect(shallowToJson(tree)).toMatchSnapshot();
    });

    it('должен отрисовать логотип категории, название категории на первой строке, параметры на второй строке', () => {
        item = {
            category: Category.CARS,
            params: {
                price_to: 500000,
                section: 'all',
            },
            paramsDescription: {
                moreCount: 0,
                paramsInfo: [
                    { label: 'Цена', val: 'до 500 000 ₽' },
                ],
            },
            subcategoryHumanName: 'Автомобили',
            url: 'https://auto.ru/samara/cars/all/?query=%D1%81%D0%B0%D0%BC%D0%B0%D1%80%D0%B0',
        } as Partial<TSearchLineSuggestItem> as TSearchLineSuggestItem;

        const tree = shallow(
            <SearchLineSuggestItem item={ item } { ...statProps }/>,
        );

        expect(shallowToJson(tree)).toMatchSnapshot();
    });

    it('должен отрисовать логотип категории, название категории на первой строке, параметры и город на второй строке', () => {
        item = {
            category: Category.CARS,
            params: {
                geo_id: [ 51 ],
                catalog_filter: [ { mark: 'AUDI' } ],
                price_to: 500000,
                section: 'all',
            },
            paramsDescription: {
                moreCount: 0,
                paramsInfo: [
                    { label: 'Цена', val: 'до 500 000 ₽' },
                ],
            },
            subcategoryHumanName: 'Автомобили',
            url: 'https://auto.ru/samara/cars/all/?query=%D1%81%D0%B0%D0%BC%D0%B0%D1%80%D0%B0',
            view: {
                mark_model_nameplate_gen_views: [
                    {
                        mark: {
                            code: 'AUDI',
                            name: 'Audi',
                            ru_name: 'Ауди',
                            logo: {
                                sizes: {
                                    logo: '//avatars.mds.yandex.net/get-verba/997355/2a000001651f4baeb0ee7d9d292ce0db5e9a/logo',
                                },
                            } as Partial<Photo> as Photo,
                        } as Mark,
                    },
                ],
                regions: [
                    { id: '51', name: 'Самара' } as RegionInfo,
                ],
            } as SavedSearchView,
        } as Partial<TSearchLineSuggestItem> as TSearchLineSuggestItem;

        const tree = shallow(
            <SearchLineSuggestItem item={ item } { ...statProps }/>,
        );

        expect(shallowToJson(tree)).toMatchSnapshot();
    });
});

describe('метрика на клик', () => {
    let wrapper: ShallowWrapper<SearchLineSuggestItem>;
    beforeEach(() => {
        item = {
            category: Category.CARS,
            params: {
                category: 'cars',
                section: 'all',
            },
            paramsDescription: {
                moreCount: 0,
                paramsInfo: [],
            },
            subcategoryHumanName: 'Автомобили',
            url: 'https://auto.ru/samara/cars/all/?query=%D1%81%D0%B0%D0%BC%D0%B0%D1%80%D0%B0',
        } as Partial<TSearchLineSuggestItem> as TSearchLineSuggestItem;

        wrapper = shallow(
            <SearchLineSuggestItem item={ item } { ...statProps }/>,
            { context: contextMock },
        );
    });

    it('должен отправить метрику про переход', () => {
        wrapper.find('.SearchLineSuggestItem').simulate(('click'));

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'searchline', 'click_suggest' ]);
    });

    it('должен отправить метрику про поисковые параметры', () => {
        wrapper.find('.SearchLineSuggestItem').simulate(('click'));

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([
            'searchline',
            'click_search_parameters',
            [
                [ 'category', 'cars' ],
                [ 'section', 'all' ],
            ],
        ]);
    });

    it('должен отправить фронтлог про переход', () => {
        wrapper.find('.SearchLineSuggestItem').simulate(('click'));

        expect(statApi.logImmediately).toHaveBeenCalledWith({
            suggest_click_event: {
                position: statProps.position,
                query: statProps.query,
                query_id: statProps.queryId,
            },
        });
    });
});

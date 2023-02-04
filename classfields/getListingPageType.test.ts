import type { TSearchParameters } from 'auto-core/types/TSearchParameters';

import getListingPageType, { PageType } from './getListingPageType';

it('если нет экспа - отдаёт листинг', () => {
    const searchParams: TSearchParameters = {
        section: 'new',
        category: 'cars',
        catalog_filter: [ {
            mark: 'renault',
        } ],
    };

    expect(getListingPageType(searchParams, false)).toBe(PageType.LISTING);
});

it('несколько каталог фильтров - отдаёт листинг', () => {
    const searchParams: TSearchParameters = {
        section: 'new',
        category: 'cars',
        catalog_filter: [ {
            mark: 'renault',
        }, {
            mark: 'audi',
        } ],
    };

    expect(getListingPageType(searchParams, true)).toBe(PageType.LISTING);
});

it('есть доп-параметры - отдаёт листинг', () => {
    const searchParams: TSearchParameters = {
        section: 'new',
        category: 'cars',
        engine_type: [ 'ELECTRO' ],
        catalog_filter: [ {
            mark: 'renault',
        } ],
    };

    expect(getListingPageType(searchParams, true)).toBe(PageType.LISTING);
});

it('должен отдать главную маркетплейса', () => {
    const searchParams: TSearchParameters = {
        section: 'new',
        category: 'cars',
    };

    expect(getListingPageType(searchParams, true)).toBe(PageType.MARKETPLACE_INDEX);
});

it('должен отдать страницу марки маркетплейса', () => {
    const searchParams: TSearchParameters = {
        section: 'new',
        category: 'cars',
        catalog_filter: [ {
            mark: 'renault',
        } ],
    };

    expect(getListingPageType(searchParams, true)).toBe(PageType.MARKETPLACE_MARK);
});

it('должен отдать страницу модели маркетплейса', () => {
    const searchParams: TSearchParameters = {
        section: 'new',
        category: 'cars',
        catalog_filter: [ {
            mark: 'renault',
            model: 'renault',
            generation: '123',
            configuration: '345',
        } ],
    };

    expect(getListingPageType(searchParams, true)).toBe(PageType.MARKETPLACE_MODEL);
});

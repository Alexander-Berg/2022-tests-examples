import breadcrumbsPublicApiMock from 'auto-core/react/dataDomain/breadcrumbsPublicApi/mocks/breadcrumbsPublicApi.mock';
import type { StateBreadcrumbsPublicApi } from 'auto-core/react/dataDomain/breadcrumbsPublicApi/types';

import type { TRouteParams } from 'auto-core/types/TStateCatalogRoute';
import { SpecificationType } from 'auto-core/types/TStateCatalogRoute';

import getBreadcrumbs from './getBreadcrumbs';

type State = {
    breadcrumbsPublicApi: StateBreadcrumbsPublicApi;
    searchParams: TRouteParams;
}

const INITIAL_STATE: State = {
    breadcrumbsPublicApi: breadcrumbsPublicApiMock,
    searchParams: {
        category: 'cars',
        mark: 'FORD',
        model: 'ECOSPORT',
        specification: SpecificationType.RAZMER_KOLES,
    },
};

it('Должен вернуть 4 ХК для марка и модель', () => {
    expect(getBreadcrumbs(INITIAL_STATE)).toHaveLength(4);
    expect(getBreadcrumbs(INITIAL_STATE)).toEqual([
        { caption: 'Продажа автомобилей', name: 'index', route: 'index', urlOptions: { geoIds: [] },
            urlParams: { category: undefined, section: undefined },
        },
        { caption: 'Каталог', name: 'catalog', route: 'catalog', urlOptions: { geoIds: [] },
            urlParams: { category: undefined, section: undefined },
        },
        { caption: 'Ford', name: 'mark', route: 'catalog',
            urlParams: { category: 'cars', mark: 'FORD', model: 'ECOSPORT', specification: 'razmer-koles' },
        },
        { caption: 'EcoSport', name: 'model', route: 'catalog',
            urlParams: { category: 'cars', mark: 'FORD', model: 'ECOSPORT', specification: 'razmer-koles' },
        },
    ]);
});

it('Должен вернуть 3 ХК для марки', () => {
    delete INITIAL_STATE.searchParams.model;
    expect(getBreadcrumbs(INITIAL_STATE)).toHaveLength(3);
    expect(getBreadcrumbs(INITIAL_STATE)).toEqual([
        { caption: 'Продажа автомобилей', name: 'index', route: 'index', urlOptions: { geoIds: [] }, urlParams: { category: undefined, section: undefined } },
        { caption: 'Каталог', name: 'catalog', route: 'catalog', urlOptions: { geoIds: [] }, urlParams: { category: undefined, section: undefined } },
        { caption: 'Ford', name: 'mark', route: 'catalog', urlParams: { category: 'cars', mark: 'FORD', specification: 'razmer-koles' } },
    ]);
});

it('Возвращает две крошки', () => {
    INITIAL_STATE.searchParams = {};

    expect(getBreadcrumbs(INITIAL_STATE)).toHaveLength(2);
    expect(getBreadcrumbs(INITIAL_STATE)).toEqual([
        { caption: 'Продажа автомобилей', name: 'index', route: 'index', urlOptions: { geoIds: [] }, urlParams: { category: undefined, section: undefined } },
        { caption: 'Каталог', name: 'catalog', route: 'catalog', urlOptions: { geoIds: [] }, urlParams: { category: undefined, section: undefined } },
    ]);
});

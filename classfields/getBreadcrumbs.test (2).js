const getBreadcrumbs = require('auto-core/react/dataDomain/listing/selectors/getBreadcrumbs');

let INITIAL_STATE;
beforeEach(() => {
    INITIAL_STATE = {
        breadcrumbsPublicApi: {
            data: [
                {
                    meta_level: 'MARK_LEVEL',
                    entities: [
                        {
                            id: 'AUDI',
                            name: 'Audi',
                        },
                    ],
                },
                {
                    meta_level: 'MODEL_LEVEL',
                    mark: {
                        id: 'AUDI',
                    },
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
                        },
                    ],
                },
                {
                    meta_level: 'GENERATION_LEVEL',
                    mark: {
                        id: 'AUDI',
                    },
                    model: {
                        id: 'A5',
                    },
                    entities: [
                        {
                            id: '21745628',
                            name: 'II (F5) Рестайлинг',
                        },
                    ],
                },
            ],
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
                    catalog_equipment: [ 'seats-5' ],
                    displacement_from: 3000,
                    displacement_to: 3000,
                    section: 'all',
                    category: 'cars',
                },
            },
        },
    };
});

it('Должен вернуть пустой массив если не выбрана марка', () => {
    INITIAL_STATE.breadcrumbsPublicApi.data = [];
    INITIAL_STATE.listing.data.search_parameters.catalog_filter = [];

    expect(getBreadcrumbs(INITIAL_STATE)).toEqual([]);
});

it('Должен вернуть пустой массив если выбрано несколько марок', () => {
    INITIAL_STATE.listing.data.search_parameters.catalog_filter.push({
        mark: 'DODGE', model: 'CARAVAN', nameplate_name: 'grand_caravan',
    });

    expect(getBreadcrumbs(INITIAL_STATE)).toEqual([]);
});

it('Должен вернуть ХК для: марка AUDI, модель A5, поколение II (F5) Рестайлинг', () => {
    expect(getBreadcrumbs(INITIAL_STATE)).toMatchSnapshot();
});

it('Должен вернуть ХК для: марка AUDI, модель A5', () => {
    delete INITIAL_STATE.listing.data.search_parameters.catalog_filter[0].generation;
    INITIAL_STATE.breadcrumbsPublicApi.data.splice(-1, 1);

    expect(getBreadcrumbs(INITIAL_STATE)).toMatchSnapshot();
});

it('Должен вернуть ХК для: марка AUDI', () => {
    delete INITIAL_STATE.listing.data.search_parameters.catalog_filter[0].generation;
    delete INITIAL_STATE.listing.data.search_parameters.catalog_filter[0].model;
    INITIAL_STATE.breadcrumbsPublicApi.data.splice(-1, 2);

    expect(getBreadcrumbs(INITIAL_STATE)).toMatchSnapshot();
});

it('Должен вернуть ХК для: марка AUDI, модель A5, поколение II (F5) Рестайлинг c пробегом', () => {
    INITIAL_STATE.listing.data.search_parameters.section = 'used';

    expect(getBreadcrumbs(INITIAL_STATE)).toMatchSnapshot();
});

it('Должен вернуть ХК для: новая марка AUDI, модель A5, поколение II (F5) Рестайлинг', () => {
    INITIAL_STATE.listing.data.search_parameters.section = 'new';

    expect(getBreadcrumbs(INITIAL_STATE)).toMatchSnapshot();
});

it('Должен вернуть ХК для: марка AUDI и трансмиссия', () => {
    delete INITIAL_STATE.listing.data.search_parameters.catalog_filter[0].generation;
    delete INITIAL_STATE.listing.data.search_parameters.catalog_filter[0].model;
    INITIAL_STATE.breadcrumbsPublicApi.data.splice(-1, 2);
    INITIAL_STATE.listing.data.search_parameters.transmission = [ 'MECHANICAL' ];

    expect(getBreadcrumbs(INITIAL_STATE)).toMatchSnapshot();
});

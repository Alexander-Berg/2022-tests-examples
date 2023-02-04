const getSubscriptionTitleByState = require('./getSubscriptionTitleByState');

const breadcrumbsPublicApi = require('auto-core/react/dataDomain/breadcrumbsPublicApi/mocks/breadcrumbsPublicApi.mock');

it('должен вернуть тайтл, если mmm один', () => {
    const state = {
        listing: {
            data: {
                search_parameters: {
                    catalog_filter: [
                        { mark: 'FORD', model: 'ECOSPORT' },
                    ],
                },
            },
        },
        breadcrumbsPublicApi,
    };
    expect(getSubscriptionTitleByState(state)).toBe('Ford EcoSport');
});

it('должен вернуть тайтл, если mmm больше одного', () => {
    const state = {
        listing: {
            data: {
                search_parameters: {
                    catalog_filter: [
                        { mark: 'FORD', model: 'ECOSPORT' },
                        { mark: 'FORD', model: 'EDGE' },
                    ],
                },
            },
        },
        breadcrumbsPublicApi,
    };
    expect(getSubscriptionTitleByState(state)).toBe('Ford EcoSport, Ford Edge');
});

it('должен вернуть тайтл, если нет catalog_filter', () => {
    const state = {
        listing: {
            data: {
                search_parameters: {},
            },
        },
        breadcrumbsPublicApi,
    };
    expect(getSubscriptionTitleByState(state)).toBe('Все марки автомобилей');
});

it('должен вернуть тайтл, если нет catalog_filter и это коммтс', () => {
    const state = {
        listing: {
            data: {
                search_parameters: {
                    trucks_category: 'LCV',
                    section: 'all',
                    category: 'trucks',
                },
            },
        },
        breadcrumbsPublicApi,
    };
    expect(getSubscriptionTitleByState(state)).toBe('Все марки лёгкого коммерческого транспорта');
});

it('должен вернуть тайтл, если нет catalog_filter и это мото', () => {
    const state = {
        listing: {
            data: {
                search_parameters: {
                    moto_category: 'ATV',
                    section: 'all',
                    category: 'moto',
                },
            },
        },
        breadcrumbsPublicApi,
    };
    expect(getSubscriptionTitleByState(state)).toBe('Все марки мотовездеходов');
});

// хороший человек не стал бы писать тесты на костыль, а я написал
it('должен вернуть тайтл, если нет catalog_filter и это хз что', () => {
    const state = {
        listing: {
            data: {
                search_parameters: {
                    section: 'all',
                    category: 'hui',
                },
            },
        },
        breadcrumbsPublicApi,
    };
    expect(getSubscriptionTitleByState(state)).toBe('Все марки');
});

it('должен вернуть тайтл, если поиск по дилеру', () => {
    const state = {
        listing: {
            data: {
                search_parameters: {
                    catalog_filter: [
                        { mark: 'FORD', model: 'ECOSPORT' },
                    ],
                    dealer_id: 'ya dealer',
                },
            },
        },
        breadcrumbsPublicApi,
        salonInfo: {
            data: {
                name: 'Самый лучший салон',
            },
        },
    };
    expect(getSubscriptionTitleByState(state)).toBe('Самый лучший салон, Ford EcoSport');
});

const breadcrumbsPublicApiMock = require('auto-core/react/dataDomain/breadcrumbsPublicApi/mocks/breadcrumbsPublicApi.mock');
const shouldShowTagsLinks = require('./shouldShowTagsLinks');

const initialState = {
    listing: {
        data: {
            search_parameters: {
                category: 'cars',
                section: 'new',
                catalog_filter: [ { mark: 'MERCEDES' } ],
            },
        },
    },
    geo: {
        gids: [ '1', '2' ],
    },
    breadcrumbsPublicApi: { ...breadcrumbsPublicApiMock },
};

it('должен вернуть true для выдачи новых автомобилей, без указания гео, без указания марки и модели', () => {
    const currentState = {
        ...initialState,
        geo: {
            gids: [],
        },
        listing: {
            data: {
                search_parameters: {
                    category: 'cars',
                    section: 'new',
                },
            },
        },
    };

    expect(shouldShowTagsLinks(currentState)).toEqual(true);
});

it('должен вернуть true для выдачи автомобилей вторички, без указания гео, без указания марки и модели', () => {
    const currentState = {
        ...initialState,
        geo: {
            gids: [],
        },
        listing: {
            data: {
                search_parameters: {
                    category: 'cars',
                    section: 'used',
                },
            },
        },
    };

    expect(shouldShowTagsLinks(currentState)).toEqual(true);
});

it('должен вернуть true для выдачи автомобилей вторички и новых, без указания гео, без указания марки и модели', () => {
    const currentState = {
        ...initialState,
        geo: {
            gids: [],
        },
        listing: {
            data: {
                search_parameters: {
                    category: 'cars',
                    section: 'all',
                },
            },
        },
    };

    expect(shouldShowTagsLinks(currentState)).toEqual(true);
});

it('должен вернуть true для выдачи автомобилей, с указанием гео крупного города, без указания марки и модели', () => {
    const currentState = {
        ...initialState,
        geo: {
            gids: [ 213 ],
        },
        listing: {
            data: {
                search_parameters: {
                    category: 'cars',
                    section: 'all',
                },
            },
        },
    };

    expect(shouldShowTagsLinks(currentState)).toEqual(true);
});

it('должен вернуть true для выдачи автомобилей, с указанием гео, без указания марки и модели', () => {
    const currentState = {
        ...initialState,
        listing: {
            data: {
                search_parameters: {
                    category: 'cars',
                    section: 'all',
                },
            },
        },
    };

    expect(shouldShowTagsLinks(currentState)).toEqual(true);
});

it('должен вернуть false для выдачи мото, без указания гео, без указания марки и модели', () => {
    const currentState = {
        ...initialState,
        geo: {
            gids: [],
        },
        listing: {
            data: {
                search_parameters: {
                    category: 'moto',
                    section: 'all',
                },
            },
        },
    };

    expect(shouldShowTagsLinks(currentState)).toEqual(false);
});

it('должен вернуть false для выдачи автомобилей, без указания гео, с указанием марки и без модели', () => {
    const currentState = {
        ...initialState,
        geo: {
            gids: [ ],
        },
    };

    expect(shouldShowTagsLinks(currentState)).toEqual(false);
});

it('должен вернуть false для выдачи автомобилей, с указанием гео небольшого города, без указания марки и модели', () => {
    const currentState = {
        ...initialState,
        geo: {
            gids: [ 11171 ],
        },
        listing: {
            data: {
                search_parameters: {
                    category: 'cars',
                    section: 'all',
                },
            },
        },
    };

    expect(shouldShowTagsLinks(currentState)).toEqual(false);
});

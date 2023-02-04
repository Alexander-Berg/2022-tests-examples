const getModelsDetailsFromBreadcrumbs = require('./getModelsDetailsFromBreadcrumbs');
const breadcrumbsMock = require('auto-core/react/dataDomain/breadcrumbsPublicApi/mocks/breadcrumbsPublicApi.mock');

it('должен вернуть список марок с кодом, именем и списком моделей', () => {
    expect(getModelsDetailsFromBreadcrumbs(
        { FORD: [ 'FOCUS', 'FLEX' ], SKODA: [ 'KODIAQ', 'OCTAVIA' ] },
        'FORD',
        breadcrumbsMock.data,
    )).toStrictEqual([
        {
            count: 0,
            cyrillic_name: 'Флекс',
            id: 'FLEX',
            itemFilterParams: {
                model: 'FLEX',
            },
            name: 'Flex',
            nameplates: [],
            popular: false,
            year_from: 2008,
            year_to: 2019,
        },
        {
            count: 216,
            cyrillic_name: 'Фокус',
            id: 'FOCUS',
            itemFilterParams: {
                model: 'FOCUS',
            },
            name: 'Focus',
            nameplates: [
                {
                    id: '-1',
                    name: 'Focus',
                    semantic_url: 'focus',
                    no_model: true,
                },
                {
                    id: '12880780',
                    name: 'Active',
                    semantic_url: 'active',
                    no_model: false,
                },
            ],
            popular: false,
            year_from: 1998,
            year_to: 2021,
        },
    ]);
});

it('должен вернуть пустой список, если марки-модели нет в breadcrumbs', () => {
    expect(getModelsDetailsFromBreadcrumbs({ SKODA: [ 'KODIAQ', 'OCTAVIA' ] }, 'SKODA', breadcrumbsMock.data)).toStrictEqual([]);
});

it('должен вернуть пустой список, если марки-модели нет в availableMarksModels', () => {
    expect(getModelsDetailsFromBreadcrumbs({ SKODA: [ 'KODIAQ', 'OCTAVIA' ] }, 'FORD', breadcrumbsMock.data)).toStrictEqual([]);
});

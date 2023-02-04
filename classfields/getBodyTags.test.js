const getBodyTags = require('auto-core/react/dataDomain/crossLinks/helpers/getBodyTags');

it('Должен вернуть пустой массив если не выбрана марка', () => {
    const searchParameters = {
        section: 'all',
        category: 'cars',
    };
    const mmmInfo = {};
    const bodyTypes = [];

    expect(getBodyTags(searchParameters, mmmInfo, bodyTypes)).toMatchSnapshot();
});

it('Должен вернуть пустой массив если больше одного фильтра по марке', () => {
    const searchParameters = {
        catalog_filter: [
            { mark: 'VAZ' },
            { mark: 'AUDI' },
        ],
        section: 'all',
        category: 'cars',
    };

    const mmmInfo = {};
    const bodyTypes = [];

    expect(getBodyTags(searchParameters, mmmInfo, bodyTypes)).toEqual([]);
});

it('Должен вернуть все типы кузова из bodyTypes для AUDI', () => {
    const searchParameters = {
        catalog_filter: [
            { mark: 'AUDI' },
        ],
        section: 'all',
        category: 'cars',
    };

    const mmmInfo = { mark: { id: 'AUDI', name: 'Audi' } };
    const bodyTypes = [
        {
            body: 'COUPE',
            bodyRus: 'Купе',
            key: 'AUDI_COUPE',
            value: 'COUPE',
        },
        {
            body: 'SEDAN',
            bodyRus: 'Седан',
            key: 'AUDI_SEDAN',
            value: 'SEDAN',
        },
    ];

    expect(getBodyTags(searchParameters, mmmInfo, bodyTypes)).toMatchSnapshot();
});

it('Должен вернуть все типы кузова из bodyTypes для AUDI 100', () => {
    const searchParameters = {
        catalog_filter: [
            { mark: 'AUDI', model: '100' },
        ],
        section: 'all',
        category: 'cars',
    };

    const mmmInfo = { mark: { id: 'AUDI', name: 'Audi' }, model: { id: '100', name: '100' } };
    const bodyTypes = [
        {
            body: 'COUPE',
            bodyRus: 'Купе',
            key: 'AUDI_COUPE',
            value: 'COUPE',
        },
        {
            body: 'SEDAN',
            bodyRus: 'Седан',
            key: 'AUDI_SEDAN',
            value: 'SEDAN',
        },
    ];

    expect(getBodyTags(searchParameters, mmmInfo, bodyTypes)).toMatchSnapshot();
});

it('Должен вернуть все типы кузова из bodyTypes для модели с шильдом', () => {
    const searchParameters = {
        catalog_filter: [
            {
                mark: 'FORD',
                model: 'C_MAX',
                nameplate_name: 'grand',
            },
        ],
        section: 'all',
        category: 'cars',
    };

    const mmmInfo = {
        mark: {
            name: 'Audi',
            id: 'AUDI',
        },
        model: {
            name: 'A3',
            id: 'A3',
            nameplates: [
                { id: '0', name: 'Grand C-Max', semantic_url: 'grand', no_model: true },
            ],
        },
    };

    const bodyTypes = [
        {
            body: 'COUPE',
            bodyRus: 'Купе',
            key: 'AUDI_COUPE',
            value: 'COUPE',
        },
        {
            body: 'SEDAN',
            bodyRus: 'Седан',
            key: 'AUDI_SEDAN',
            value: 'SEDAN',
        },
    ];

    expect(getBodyTags(searchParameters, mmmInfo, bodyTypes)).toMatchSnapshot();
});

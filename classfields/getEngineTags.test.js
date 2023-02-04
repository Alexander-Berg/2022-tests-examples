const getEngineTags = require('auto-core/react/dataDomain/crossLinks/helpers/getEngineTags');

const engineTypes =
    [
        {
            engine: 'GASOLINE',
            engineRus: 'бензиновым',
            key: 'AUDI_GASOLINE',
            value: 'GASOLINE',
        },
        {
            engine: 'DIESEL',
            engineRus: 'дизельным',
            key: 'AUDI_DIESEL',
            value: 'DIESEL',
        },
    ];

it('Должен вернуть пустой массив, если не выбрана марка', () => {
    const searchParameters = {
        section: 'all',
        category: 'cars',
    };

    const mmmInfo = {};

    expect(getEngineTags(searchParameters, mmmInfo, engineTypes)).toEqual([]);
});

it('Должен вернуть двигатели из engineTypes для марки BMW', () => {
    const searchParameters = {
        catalog_filter: [
            {
                mark: 'BMW',
            },
        ],
        section: 'all',
        category: 'cars',
    };

    const mmmInfo = {
        mark: {
            name: 'BMW',
        },
    };

    expect(getEngineTags(searchParameters, mmmInfo, engineTypes)).toMatchSnapshot();
});

it('Должен вернуть двигатели из engineTypes для марки BMW и модели 3ER', () => {
    const searchParameters = {
        catalog_filter: [
            {
                mark: 'BMW',
                model: '3ER',
            },
        ],
        section: 'all',
        category: 'cars',
    };

    const mmmInfo = {
        mark: {
            name: 'BMW',
        },
        model: {
            name: '3 серии',
        },
    };

    expect(getEngineTags(searchParameters, mmmInfo, engineTypes)).toMatchSnapshot();
});

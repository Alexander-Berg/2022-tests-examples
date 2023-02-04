const getGenerationTags = require('auto-core/react/dataDomain/crossLinks/helpers/getGenerationTags');

const generations = [
    { generation: '2305282', key: 'BMW_3ER_2305282', value: '2305282' },
    { generation: '3473199', key: 'BMW_3ER_3473199', value: '3473199' },
];

it('Должен вернуть пустой массив, если не выбрана марка', () => {
    const searchParameters = {
        section: 'all',
        category: 'cars',
    };

    const mmmInfo = {};

    expect(getGenerationTags(searchParameters, mmmInfo, generations)).toEqual([]);
});

it('Должен вернуть пустой массив, если не выбрана модель', () => {
    const searchParameters = {
        catalog_filter: [
            {
                mark: 'BMW',
            },
        ],
        section: 'all',
        category: 'cars',
    };

    const mmmInfo = {};

    expect(getGenerationTags(searchParameters, mmmInfo, generations)).toEqual([]);
});

it('Должен вернуть поколения из generations для марки BMW и модели 3ER', () => {
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
            id: 'BMW',
        },
        model: {
            name: '3 серии',
            id: '3ER',
        },
    };

    expect(getGenerationTags(searchParameters, mmmInfo, generations)).toMatchSnapshot();
});

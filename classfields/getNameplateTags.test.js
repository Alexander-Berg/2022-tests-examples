const getNameplateTags = require('auto-core/react/dataDomain/crossLinks/helpers/getNameplateTags');

const nameplates = [
    { nameplate: 'e-tron', key: 'AUDI_A3_e_tron', value: 'e-tron' },
    { nameplate: 'g-tron', key: 'AUDI_A3_g_tron', value: 'g-tron' },
];
const allNameplates = [
    { id: 'e-tron', name: 'e-tron', key: 'AUDI_A3_e_tron', semantic_url: 'e-tron' },
    { id: 'g-tron', name: 'g-tron', key: 'AUDI_A3_g_tron', semantic_url: 'g-tron' },
];

it('Должен вернуть пустой массив, если не выбрана марка', () => {
    const searchParameters = {
        section: 'all',
        category: 'cars',
    };

    const mmmInfo = {};

    expect(getNameplateTags(searchParameters, mmmInfo, nameplates, allNameplates)).toEqual([]);
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

    expect(getNameplateTags(searchParameters, mmmInfo, nameplates, allNameplates)).toEqual([]);
});

it('Должен вернуть шильды из nameplates для марки BMW и модели 3ER', () => {
    const searchParameters = {
        catalog_filter: [
            {
                mark: 'AUDI',
                model: 'A3',
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
        },
    };

    expect(getNameplateTags(searchParameters, mmmInfo, nameplates, allNameplates)).toMatchSnapshot();
});

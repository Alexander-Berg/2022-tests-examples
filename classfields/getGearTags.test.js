const getGearTags = require('auto-core/react/dataDomain/crossLinks/helpers/getGearTags');

const gearTypes =
    [
        {
            gear: 'ALL_WHEEL_DRIVE',
            gearRus: 'полным',
            key: 'AUDI_ALL_WHEEL_DRIVE',
            value: 'ALL_WHEEL_DRIVE',
        },
        {
            gear: 'FORWARD_CONTROL',
            gearRus: 'передним',
            key: 'AUDI_FORWARD_CONTROL',
            value: 'FORWARD_CONTROL',
        },
    ];

it('Должен вернуть пустой массив, если не выбрана марка', () => {
    const searchParameters = {
        section: 'all',
        category: 'cars',
    };

    const mmmInfo = {};

    expect(getGearTags(searchParameters, mmmInfo, gearTypes)).toEqual([]);
});

it('Должен вернуть все типы привода из gearTypes для марки BMW', () => {
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

    expect(getGearTags(searchParameters, mmmInfo, gearTypes)).toMatchSnapshot();
});

it('Должен вернуть все типы привода из gearTypes для марки BMW и модели X3', () => {
    const searchParameters = {
        catalog_filter: [
            {
                mark: 'BMW',
                model: 'X3',
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
            name: 'X3',
        },
    };

    expect(getGearTags(searchParameters, mmmInfo, gearTypes)).toMatchSnapshot();
});

const getGroupedTariffs = require('./getGroupedTariffs');

it('должен группировать подкатегории', () => {
    const tariffs = {
        CARS_USED: {
            category: 'CARS',
            enabled: true,
            type: 'SINGLE',
        },
        CARS_NEW: {
            enabled: false,
            category: 'CARS',
            type: 'CALLS',
        },
        TRUCKS_COMMERCIAL: {
            enabled: true,
            category: 'TRUCKS',
            type: 'SINGLE',
            truck_class: 'COMMERCIAL',
        },
        TRUCKS_LCV_USED: {
            enabled: true,
            category: 'TRUCKS',
            type: 'SINGLE',
            truck_class: 'LCV',
        },
        MOTO: {
            enabled: false,
            category: 'MOTO',
        },
    };

    const expectedResult = {
        CARS_USED: {
            category: 'CARS',
            enabled: true,
            type: 'SINGLE',
        },
        CARS_NEW: {
            enabled: false,
            category: 'CARS',
            type: 'CALLS',
        },
        TRUCKS: {
            enabled: true,
            category: 'TRUCKS',
            type: 'SINGLE',
            subcategories: [
                {
                    enabled: true,
                    category: 'TRUCKS',
                    type: 'SINGLE',
                    truck_class: 'COMMERCIAL',
                },
                {
                    enabled: true,
                    category: 'TRUCKS',
                    type: 'SINGLE',
                    truck_class: 'LCV',
                },
            ],
        },
        MOTO: {
            enabled: false,
            category: 'MOTO',
        },
    };

    expect(getGroupedTariffs({ calculator: { tariffs } })).toEqual(expectedResult);
});

it('не должен изменять структуру, если вложенных подкатегорий не долнжо быть', () => {
    const tariffs = {
        CARS_USED: {
            category: 'CARS',
            enabled: true,
            type: 'SINGLE',
        },
        CARS_NEW: {
            enabled: false,
            category: 'CARS',
            type: 'CALLS',
        },
        TRUCKS_COMMERCIAL: {
            enabled: true,
            category: 'TRUCKS',
            type: 'QUOTA',
            truck_class: 'COMMERCIAL',
        },
        MOTO: {
            enabled: false,
            category: 'MOTO',
        },
    };

    expect(getGroupedTariffs({ calculator: { tariffs } })).toEqual(tariffs);
});

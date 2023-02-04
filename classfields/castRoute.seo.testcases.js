const MMM_CASES = [
    // EMPTY FILTERS
    {},
    // MARK
    {
        mark: 'AUDI',
    },
    {
        mark: 'BENTLEY',
    },
    {
        mark: 'GAZ',
    },

    // MARK + MODEL
    {
        mark: 'AUDI',
        model: 'A6',
    },
    {
        mark: 'AUDI',
        model: 'SQ5',
    },
    {
        mark: 'BENTLEY',
        model: 'CONTINENTAL_GT',
    },
    {
        mark: 'GAZ',
        model: '21',
    },

    // MARK + MODEL + GENERATION
    {
        mark: 'AUDI',
        model: 'A6',
        generation: '20246005',
    },
    {
        mark: 'AUDI',
        model: 'A6',
        generation: '21210593',
    },
    {
        mark: 'AUDI',
        model: 'SQ5',
        generation: '22651901',
    },
    {
        mark: 'BENTLEY',
        model: 'CONTINENTAL_GT',
        generation: '20542082',
    },
    {
        mark: 'GAZ',
        model: '21',
        generation: '7867811',
    },
];

const BODY_TYPES = [ 'LIMOUSINE', 'SEDAN' ];
const ENGINE_TYPES = [ 'GASOLINE', 'ELECTRO' ];
const GEAR_TYPES = [ 'ALL_WHEEL_DRIVE', 'FORWARD_CONTROL', 'REAR_DRIVE' ];
// черный и непопулярный
const COLOR_TYPES = [ '040001', 'FF5454' ];
const PRICE = [ '1000000' ];

const SECTIONS = [ 'all', 'new', 'used' ];

const PARES = [
    {
        body_type_group: [ 'SEDAN' ],
        engine_group: [ 'GASOLINE' ],
    },
    {
        body_type_group: [ 'SEDAN' ],
        gear_type: [ 'FORWARD_CONTROL' ],
    },
    {
        body_type_group: [ 'SEDAN' ],
        color: [ '040001' ],
    },
    {
        body_type_group: [ 'SEDAN' ],
        price_to: [ '1000000' ],
    },
    {
        engine_group: [ 'GASOLINE' ],
        gear_type: [ 'FORWARD_CONTROL' ],
    },
    {
        engine_group: [ 'GASOLINE' ],
        color: [ '040001' ],
    },
    {
        engine_group: [ 'GASOLINE' ],
        price_to: [ '1000000' ],
    },
    {
        gear_type: [ 'FORWARD_CONTROL' ],
        color: [ '040001' ],
    },
    {
        gear_type: [ 'FORWARD_CONTROL' ],
        price_to: [ '1000000' ],
    },
    {
        displacement_from: 2000,
        displacement_to: 2000,
    },
    {
        displacement_from: 2000,
        displacement_to: 2100,
    },
    {
        catalog_equipment: [ 'seats-5' ],
    },
    {
        catalog_equipment: [ 'seats-4,seats-5' ],
    },
    {
        catalog_equipment: [ 'esp', 'seats-5' ],
    },
];

// body_type_group
// engine_group
// gear_group
// color

const TEST_CASES = [];

SECTIONS.forEach(section => {
    MMM_CASES.forEach(mmmParams => {
        BODY_TYPES.forEach(bodyType => {
            const routeParams = {
                // Выбираем нужные марку/модель/поколение в фильтрах
                catalog_filter: [ mmmParams ],
                category: 'cars',
                section,
                body_type_group: [ bodyType ],
            };

            TEST_CASES.push(routeParams);
        });

        ENGINE_TYPES.forEach(engineType => {
            const routeParams = {
                catalog_filter: [ mmmParams ],
                category: 'cars',
                section,
                engine_group: [ engineType ],
            };

            TEST_CASES.push(routeParams);
        });

        GEAR_TYPES.forEach(gearType => {
            const routeParams = {
                catalog_filter: [ mmmParams ],
                category: 'cars',
                section,
                gear_type: [ gearType ],
            };

            TEST_CASES.push(routeParams);
        });

        COLOR_TYPES.forEach(colorType => {
            const routeParams = {
                catalog_filter: [ mmmParams ],
                category: 'cars',
                section,
                color: [ colorType ],
            };

            TEST_CASES.push(routeParams);
        });

        PRICE.forEach(price => {
            const routeParams = {
                catalog_filter: [ mmmParams ],
                category: 'cars',
                section,
                price_to: [ price ],
            };

            TEST_CASES.push(routeParams);
        });

        PARES.forEach(pareParams => {
            const routeParams = {
                catalog_filter: [ mmmParams ],
                category: 'cars',
                section,
                ...pareParams,
            };

            TEST_CASES.push(routeParams);
        });
    });
});

module.exports = TEST_CASES;

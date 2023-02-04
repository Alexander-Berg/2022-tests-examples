const buildCustomTagsFromParams = require('./');

const defaultParams = {
    category: 'cars',
    body_type_group: [ 'SEDAN' ],
    engine_group: [ 'GASOLINE' ],
    search_tag: [],
    transmission: [ 'VARIATOR' ],
};

it('должен вернуть пустой массив для category !== cars', () => {
    const params = {
        ...defaultParams,
        category: 'moto',
    };

    expect(buildCustomTagsFromParams(params)).toMatchObject([]);
});

it('должен вернуть тег bigclearance', () => {
    const params = {
        ...defaultParams,
        clearance: 210,
    };

    expect(buildCustomTagsFromParams(params)).toMatchObject([ 'bigclearance' ]);
});

it('должен вернуть тег bigfamily', () => {
    const params = {
        ...defaultParams,
        catalog_equipment: [ 'power-child-locks-rear-doors', 'isofix', 'third-row-seats' ],
        seats: 9,
        trunk_volume: 480,
    };

    expect(buildCustomTagsFromParams(params)).toMatchObject([ 'family', 'bigfamily' ]);
});

it('должен вернуть тег bigtrunk', () => {
    const params = {
        ...defaultParams,
        trunk_volume: 500,
    };

    expect(buildCustomTagsFromParams(params)).toMatchObject([ 'bigtrunk' ]);
});

it('должен вернуть тег business', () => {
    const params = {
        ...defaultParams,
        search_tag: [ 'wide-back-seats', 'big', 'prestige', 'options', 'fast' ],
    };

    expect(buildCustomTagsFromParams(params)).toMatchObject([ 'business' ]);
});

it('должен вернуть тег city_type', () => {
    const params = {
        ...defaultParams,
        search_tag: [ 'economical' ],
        transmission: [ 'AUTOMATIC' ],
    };

    expect(buildCustomTagsFromParams(params)).toMatchObject([ 'city_type' ]);
});

it('должен вернуть тег countryside', () => {
    const params = {
        ...defaultParams,
        catalog_equipment: [ 'roof-rails' ],
        clearance: 170,
        trunk_volume: 450,
    };

    expect(buildCustomTagsFromParams(params)).toMatchObject([ 'countryside' ]);
});

it('должен вернуть тег ecomobile', () => {
    const params = {
        ...defaultParams,
        engine_group: [ 'HYBRID' ],
    };

    expect(buildCustomTagsFromParams(params)).toMatchObject([ 'ecomobile' ]);
});

it('должен вернуть тег econom', () => {
    const params = {
        ...defaultParams,
        search_tag: [ 'liquid', 'medium', 'economical' ],
        price: 120_000,
    };

    expect(buildCustomTagsFromParams(params)).toMatchObject([ 'city_type', 'econom' ]);
});

it('должен вернуть тег family', () => {
    const params = {
        ...defaultParams,
        catalog_equipment: [ 'isofix', 'power-child-locks-rear-doors' ],
        seats: 5,
        trunk_volume: 450,
    };

    expect(buildCustomTagsFromParams(params)).toMatchObject([ 'family' ]);
});

it('должен вернуть тег hunting', () => {
    const params = {
        ...defaultParams,
        gear_type: 'ALL_WHEEL_DRIVE',
        body_type_group: [ 'ALLROAD_5_DOORS' ],
        search_tag: [ 'all-terrain', 'offroad', 'big-trunk' ],
    };

    expect(buildCustomTagsFromParams(params)).toMatchObject([ 'hunting' ]);
});

it('должен вернуть тег panorama_roof', () => {
    const params = {
        ...defaultParams,
        catalog_equipment: [ 'panorama-roof' ],
    };

    expect(buildCustomTagsFromParams(params)).toMatchObject([ 'panorama_roof' ]);
});

it('должен вернуть тег retro', () => {
    const params = {
        ...defaultParams,
        year: 1985,
    };

    expect(buildCustomTagsFromParams(params)).toMatchObject([ 'retro' ]);
});

it('должен вернуть тег safe', () => {
    const params = {
        ...defaultParams,
        catalog_equipment: [
            'abs',
            'esp',
            'airbag-driver',
            'collision-prevention-assist',
        ],
    };

    expect(buildCustomTagsFromParams(params)).toMatchObject([ 'safe' ]);
});

it('должен вернуть тег 7seatsfamily', () => {
    const params = {
        ...defaultParams,
        catalog_equipment: [ 'power-child-locks-rear-doors', 'isofix', 'third-row-seats' ],
        seats: 7,
        trunk_volume: 480,
    };

    expect(buildCustomTagsFromParams(params)).toMatchObject([ 'family', 'bigfamily', '7seatsfamily' ]);
});

it('должен вернуть тег smallengine', () => {
    const params = {
        ...defaultParams,
        displacement: 1200,
        power: 90,
    };

    expect(buildCustomTagsFromParams(params)).toMatchObject([ 'smallengine' ]);
});

it('должен вернуть тег taxi', () => {
    const params = {
        ...defaultParams,
        search_tag: [ 'liquid', 'economical' ],
        displacement: 1400,
        year: new Date().getFullYear() - 5,
        fuel_rate: 7,
    };

    expect(buildCustomTagsFromParams(params)).toMatchObject([ 'taxi', 'city_type' ]);
});

it('должен вернуть тег travel', () => {
    const params = {
        ...defaultParams,
        catalog_equipment: [ 'navigation', 'cruise-control', '220v-socket' ],
        fuel_rate: 9,
        trunk_volume: 480,
        clearance: 170,
        body_type_group: [ 'ALLROAD_5_DOORS' ],
    };

    expect(buildCustomTagsFromParams(params)).toMatchObject([ 'travel' ]);
});

it('должен вернуть тег winter', () => {
    const params = {
        ...defaultParams,
        catalog_equipment: [
            'windcleaner-heat',
            'front-seats-heat',
            'wheel-heat',
            'programmed-block-heater',
        ],
        clearance: 165,
        gear_type: 'ALL_WHEEL_DRIVE',
    };

    expect(buildCustomTagsFromParams(params)).toMatchObject([ 'winter' ]);
});

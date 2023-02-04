const getCarPropSefValues = require('./getTechParamSefValues');

it('Должен вернуть все типы кузова', () => {
    expect(getCarPropSefValues('body_type_group'))
        .toEqual([
            'ALLROAD_5_DOORS', 'COUPE', 'SEDAN', 'HATCHBACK_3_DOORS',
            'WAGON', 'LIFTBACK', 'HATCHBACK_5_DOORS', 'MINIVAN',
            'PICKUP', 'CABRIO', 'ALLROAD_3_DOORS', 'ALLROAD', 'VAN',
        ]);
});

it('Должен вернуть все типы двигателя', () => {
    expect(getCarPropSefValues('engine_type'))
        .toEqual([
            'GASOLINE', 'DIESEL', 'ELECTRO', 'HYBRID',
        ]);
});

it('Должен вернуть все типы двигателя для LADA', () => {
    expect(getCarPropSefValues('engine_type', 'VAZ'))
        .toEqual([ 'GASOLINE', 'LPG' ]);
});

it('Должен вернуть все типы кузова для FORD', () => {
    expect(getCarPropSefValues('body_type_group', 'FORD'))
        .toEqual([
            'MINIVAN', 'ALLROAD_5_DOORS', 'HATCHBACK_3_DOORS',
            'HATCHBACK_5_DOORS', 'SEDAN', 'WAGON', 'LIFTBACK',
            'CABRIO', 'COUPE', 'PICKUP',
        ]);
});

it('Должен вернуть все типы кузова для FORD, не разделяя их по количеству дверей', () => {
    expect(getCarPropSefValues('body_type_group', 'FORD', '', { useAlias: true }))
        .toEqual([
            'MINIVAN', 'ALLROAD', 'HATCHBACK', 'SEDAN', 'WAGON', 'CABRIO', 'COUPE', 'PICKUP',
        ]);
});

it('Должен вернуть все типы двигателя для LADA Granta', () => {
    expect(getCarPropSefValues('engine_type', 'VAZ', 'GRANTA'))
        .toEqual([ 'GASOLINE' ]);
});

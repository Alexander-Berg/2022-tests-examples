const engineType = require('./engineType');

it('должен вернуть Бензин, газобалонное оборудование', () => {
    const name = engineType.getOriginalWithOptions({
        category: 'cars',
        vehicle_info: {
            equipment: { gbo: true },
            tech_param: {
                engine_type: 'GASOLINE',
            },
        },
    });

    expect(name).toEqual('Бензин, газобаллонное оборудование');
});

it('должен вернуть Бензин', () => {
    const name = engineType.getOriginalWithOptions({
        category: 'cars',
        vehicle_info: {
            tech_param: {
                engine_type: 'GASOLINE',
            },
        },
    });

    expect(name).toEqual('Бензин');
});

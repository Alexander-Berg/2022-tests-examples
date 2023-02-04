const getHumanTechParamDescription = require('./getHumanTechParamDescription');

it('должен вернуть правильную строку описания для электромобиля', () => {
    const complectation = {
        tech_info: {
            tech_param: {
                id: '123',
                displacement: 0,
                engine_type: 'ELECTRO',
                power: 400,
                power_kvt: 294,
                acceleration: 7,
                transmission: 'AUTOMATIC',
                gear_type: 'ALL_WHEEL_DRIVE',
            },
        },
    };
    expect(getHumanTechParamDescription(complectation)).toStrictEqual(
        '400 л.с. (294 кВт) Электро / Автоматическая / Полный',
    );
});

it('должен вернуть правильную строку описания для автомобиля с ДВС', () => {
    const complectation = {
        tech_info: {
            tech_param: {
                id: '345',
                displacement: 1968,
                engine_type: 'DIESEL',
                power: 150,
                power_kvt: 110,
                acceleration: 11,
                fuel_rate: 5.1,
                transmission: 'MECHANICAL',
                gear_type: 'FORWARD_CONTROL',
            },
        },
    };
    expect(getHumanTechParamDescription(complectation)).toStrictEqual(
        '2.0 л (150 л.с.) Дизель / Механическая / Передний',
    );
});

const engineInfo = require('./engineInfo');

it('должен вернуть корректную информацию о двигателе для электромобиля', () => {
    const complectation = {
        tech_info: {
            tech_param: {
                id: '123',
                displacement: 0,
                engine_type: 'ELECTRO',
                power: 400,
                power_kvt: 294,
                acceleration: 7,
            },
        },
    };

    expect(engineInfo(complectation)).toStrictEqual({
        id: '400 л.с. / 294 кВт / Электро',
        engineType: 'ELECTRO',
        power: 400,
        title: 'Электро / 400 л.c. / 294 кВт',
        value: [ '123' ],
        acceleration: 7,
        displacement: 0,
        fuel_rate: undefined,
    });
});

it('должен вернуть корректную информацию о двигателе автомобиля с ДВС', () => {
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
            },
        },
    };

    expect(engineInfo(complectation)).toStrictEqual({
        id: '2.0 л / 150 л.с. / Дизель',
        engineType: 'DIESEL',
        power: 150,
        title: 'Дизель 2.0 л, 150 л.c.',
        value: [ '345' ],
        displacement: 1968,
        acceleration: 11,
        fuel_rate: 5.1,
    });
});

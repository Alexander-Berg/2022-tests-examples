const getEngineInfo = require('./getEngineInfo');

it('должен вернуть правильный результат для комплектации с ДВС', () => {
    const complectationICE = {
        tech_info: {
            tech_param: {
                displacement: 1968,
                engine_type: 'DIESEL',
                power: 150,
                power_kvt: 110,
            },
        },
    };

    expect(getEngineInfo(complectationICE)).toEqual('2.0 л / 150 л.с. / Дизель');
});

it('должен вернуть правильный результат для комплектации с электродвигателем', () => {
    const complectationElectro = {
        tech_info: {
            tech_param: {
                displacement: 0,
                engine_type: 'ELECTRO',
                power: 400,
                power_kvt: 294,
            },
        },
    };

    expect(getEngineInfo(complectationElectro)).toStrictEqual('400 л.с. / 294 кВт / Электро');
});

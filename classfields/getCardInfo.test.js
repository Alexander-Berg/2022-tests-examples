const getCardInfo = require('./getCardInfo');

it('должен вернуть правильный результат для комплектации с ДВС', () => {
    const complectationICE = {
        tech_info: {
            tech_param: {
                id: '20839377',
                displacement: 1968,
                engine_type: 'DIESEL',
                gear_type: 'ALL_WHEEL_DRIVE',
                transmission: 'ROBOT',
                power: 150,
                power_kvt: 110,
                human_name: '2.0d AMT (150 л.с.) 4WD',
                acceleration: 10,
                clearance_min: 187,
                fuel_rate: 5.6,
            },
        },
    };

    expect(getCardInfo(complectationICE)).toStrictEqual([
        {
            label: 'Двигатель',
            value: '2.0 л / 150 л.с. / Дизель',
        },
        {
            label: 'Коробка',
            value: 'Роботизированная',
        },
        {
            label: 'Привод',
            value: 'Полный',
        },
        {
            label: 'Расход топлива',
            value: '5.6 л',
        },
        {
            label: 'Разгон до 100 км/ч',
            value: '10 с',
        },
    ]);
});

it('должен вернуть правильный результат для комплектации с электродвигателем', () => {
    const complectationElectro = {
        tech_info: {
            tech_param: {
                displacement: 0,
                engine_type: 'ELECTRO',
                gear_type: 'ALL_WHEEL_DRIVE',
                transmission: 'AUTOMATIC',
                power: 400,
                power_kvt: 294,
                human_name: 'Electro AT (295 кВт) 4WD',
                acceleration: 4.8,
                gear_type_autoru: 'ALL',
                transmission_autoru: 'PP',
            },
        },
    };

    expect(getCardInfo(complectationElectro)).toStrictEqual([
        {
            label: 'Двигатель',
            value: '400 л.с. / 294 кВт / Электро',
        },
        {
            label: 'Коробка',
            value: 'Автоматическая',
        },
        {
            label: 'Привод',
            value: 'Полный',
        },
        {
            label: 'Разгон до 100 км/ч',
            value: '4.8 с',
        },
    ]);
});

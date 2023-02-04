const _ = require('lodash');

const engines = require('./engines');

const groupingInfoMock = require('./mocks/groupingInfo.mock.json');

it('возвращает список объёмов и типов топлива через запятую', () => {
    const groupingInfo = _.cloneDeep(groupingInfoMock);

    expect(engines(groupingInfo)).toEqual('2.0, 3.0 л / бензин, дизель, электро');
});

it('возвращает список объём и тип топлива для одного двигателя', () => {
    const groupingInfo = {
        tech_params: [ {
            id: 21212564,
            displacement: 1353,
            engine_type: 'GASOLINE',
            gear_type: 'FORWARD_CONTROL',
            transmission: 'MECHANICAL',
            power: 252,
            power_kvt: 185,
            human_name: '1.4 AMT (140 л.с.)',
            acceleration: 9.2,
            clearance_min: 150,
            fuel_rate: 6.1,
        } ],
    };
    expect(engines(groupingInfo)).toEqual('1.4 л / бензин');
});

it('возвращает список объёмов и типов топлива, если больше двух объёмов', () => {
    const groupingInfo = _.cloneDeep(groupingInfoMock);
    groupingInfo.tech_params.push({
        id: 21212564,
        displacement: 1353,
        engine_type: 'GASOLINE',
        gear_type: 'FORWARD_CONTROL',
        transmission: 'MECHANICAL',
        power: 252,
        power_kvt: 185,
        human_name: '1.4 AMT (140 л.с.)',
        acceleration: 9.2,
        clearance_min: 150,
        fuel_rate: 6.1,
    });

    expect(engines(groupingInfo)).toEqual('1.4-3.0 л / бензин, дизель, электро');
});

it('возвращает пустую строку, если нет техпарамов', () => {
    expect(engines({})).toEqual('');
});

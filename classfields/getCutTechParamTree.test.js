const de = require('descript');
const block = require('./getCutTechParamTree');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');
const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let context;
let req;
let res;

const coupe = {
    configurations: [
        {
            entity: {
                available_steering_wheel: [ 'LEFT' ],
                back_wheel_base: 1655,
                body_size: 4784,
                body_type: 'COUPE',
                body_type_group: 'COUPE',
                boot_volume_max: 408,
                boot_volume_min: 408,
                doors_count: 2,
                front_brake: 'Ventilated disc',
                front_suspension: 'Independent, coil suspension',
                front_wheel_base: 1582,
                height: 1381,
                human_name: 'Купе',
                seats: [ '4' ],
                steering_wheel: 'LEFT',
                tank_volume: 59,
                turning_circle: 12.2,
                wheel_base: 2720,
                width: 1916,
            },
            tech_params: [
                {
                    entity: {
                        acceleration: 5.8,
                        back_brake: 'Ventilated disc',
                        back_suspension: 'Independent, coil suspension',
                        clearance: [ 130 ],
                        compression: 9.5,
                        displacement: 2300,
                        engine_type: 'GASOLINE',
                        fuel_tank_volume: 59,
                        gear_type: 'REAR_DRIVE',
                        gear_value: 6,
                        human_name: '2.3 AT (317 л.с.)',
                        max_speed: 250,
                        moment: 432,
                        moment_rpm: [ 3000 ],
                        petrol_type: '95 RON',
                        power: 317,
                        power_rpm: [ 5500 ],
                        transmission: 'AUTOMATIC',
                        valves: 4,
                        valvetrain: 'DOHC',
                        weight: 1653,
                        wheel_size: '255/40/R19',
                        year_start: 2017,
                        year_stop: 0,
                    },
                },
            ],
        },
    ],
    entity: {
        cyrillic_name: '6 Рестайлинг',
        year_from: 2017,
        year_to: 2022,
    },
};

const hatchback = {
    configurations: [
        {
            entity: {
                auto_class: 'S',
                available_steering_wheel: [ 'LEFT' ],
                back_wheel_base: 1417,
                body_size: 4445,
                body_type: 'HATCHBACK_3_DOORS',
                body_type_group: 'HATCHBACK_3_DOORS',
                doors_count: 3,
                front_brake: 'Ventilated disc',
                front_suspension: 'Independent, coil suspension',
                front_wheel_base: 1412,
                height: 1278,
                human_name: 'Хэтчбек 3 дв.',
                seats: [ '4' ],
                steering_wheel: 'LEFT',
                tank_volume: 49,
                wheel_base: 2443,
                width: 1783,
            },
            tech_params: [
                {
                    entity: {
                        acceleration: 14.3,
                        back_brake: 'Drum',
                        back_suspension: 'Conventional, coil suspension',
                        clearance: [ 130 ],
                        compression: 9,
                        consumption_city: 10.2,
                        consumption_hiway: 7.1,
                        consumption_mixed: 9,
                        cylinders_order: 'IN-LINE',
                        cylinders_value: 4,
                        diameter: '96.0x79.4',
                        displacement: 2301,
                        engine_feeding: 'Carburetor',
                        engine_model: 'Lima LL23',
                        engine_order: 'Front Longitudinal engine',
                        engine_type: 'GASOLINE',
                        feeding: 'None',
                        fuel_tank_volume: 49,
                        gear_type: 'REAR_DRIVE',
                        gear_value: 4,
                        human_name: '2.3 MT (89 л.с.)',
                        max_speed: 160,
                        moment: 160,
                        moment_rpm: [ 2800 ],
                        petrol_type: '80 RON',
                        power: 89,
                        power_kvt: '66.0',
                        power_rpm: [ 4800 ],
                        transmission: 'MECHANICAL',
                        valves: 2,
                        valvetrain: 'SOHC',
                        weight: 1250,
                        year_start: 1974,
                        year_stop: 1978,
                    },
                },
            ],
        },
    ],
    entity: {
        cyrillic_name: '6 Рестайлинг',
        year_from: 2017,
        year_to: 2022,
    },
};

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });

    publicApi.get('/1.0/reference/catalog/cars/tech-param-tree?mark=FORD&model=MUSTANG')
        .reply(200, { status: 'SUCCESS', super_generations: [ coupe, hatchback ] });
});

it('должен вернуть хетчбек и купэ', () => {
    return de.run(block, {
        context,
        params: {
            mark: 'FORD',
            model: 'MUSTANG',
            specification: 'razmer-koles',
        } })
        .then((result) => {
            expect(result).toEqual({ super_generations: [ coupe, hatchback ], status: 'SUCCESS' });
        });
});

it('должен вернуть хетчбек', () => {
    return de.run(block, {
        context,
        params: {
            mark: 'FORD',
            model: 'MUSTANG',
            specification: 'razmer-koles',
            body_type_group: [ 'HATCHBACK', 'HATCHBACK_3_DOORS', 'HATCHBACK_5_DOORS', 'LIFTBACK' ],
        } })
        .then((result) => {
            expect(result).toEqual({ super_generations: [ hatchback ], status: 'SUCCESS' });
        });
});

it('должен вернуть пустой super_generations', () => {
    return de.run(block, {
        context,
        params: {
            mark: 'FORD',
            model: 'MUSTANG',
            specification: 'razmer-koles',
            body_type_group: [ 'SEDAN' ],
        } })
        .then((result) => {
            expect(result).toEqual({ super_generations: [ ], status: 'SUCCESS' });
        });
});

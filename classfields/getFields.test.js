const getFields = require('./getFields');

describe('engine', () => {
    it('должен вернуть литры для CARS', () => {
        expect(getFields('engine', {
            category: 'cars',
            vehicle_info: {
                tech_param: {
                    displacement: 2987,
                    engine_type: 'DIESEL',
                    power: 224,
                },
                mark_info: {
                    code: 'KIA',
                },
                model_info: {
                    code: 'RIO',
                },
            },
        })).toMatchSnapshot();
    });

    it('должен вернуть см3 для MOTO', () => {
        expect(getFields('engine', {
            category: 'moto',
            vehicle_info: {
                displacement: 1687,
                engine: 'INJECTOR',
                mark_info: {
                    code: 'DUCATI',
                },
                model_info: {
                    code: 'ST',
                },
            },
        })).toMatchSnapshot();
    });

    it('должен вернуть литры для TRUCKS', () => {
        expect(getFields('engine', {
            category: 'trucks',
            vehicle_info: {
                displacement: 6000,
                horse_power: 220,
                mark_info: {
                    code: 'IVECO',
                },
                model_info: {
                    code: 'DAILY',
                },
            },
        })).toMatchSnapshot();
    });

    it('должен вернуть литры для TRUCKS, если очень маленькое значение', () => {
        expect(getFields('engine', {
            category: 'trucks',
            sub_category: 'dredge',
            vehicle_info: {
                displacement: 44,
                horse_power: 101,
                mark_info: {
                    code: 'JCB',
                },
                model_info: {
                    code: '4CX',
                },
            },
        })).toMatchSnapshot();
    });
});

describe('complectationOrEquipmentCount', () => {
    it('показать комплектацию, если указана комплектация', () => {
        expect(getFields('complectationOrEquipmentCount', {
            section: 'new',
            vehicle_info: {
                complectation: {
                    name: 'C 43 4MATIC Особая серия',
                },
            },
        })).toMatchSnapshot();
    });

    it('показать количество опций, если не указана комплектация', () => {
        expect(getFields('complectationOrEquipmentCount', {
            section: 'new',
            vehicle_info: {
                equipment: {
                    test1: {},
                    test2: {},
                    test3: {},
                    test4: {},
                    test5: {},
                },
            },
        })).toMatchSnapshot();
    });
});

describe('для нового авто', () => {
    it('должен спрятать параметр "Таможня" при значении "Растаможен"', () => {
        expect(getFields('customs', {
            section: 'new',
            documents: {
                custom_cleared: true,
            },
        })).toMatchSnapshot();
    });
    it('должен выделить параметр "Таможня" при значении "Не растаможен"', () => {
        expect(getFields('customs', {
            section: 'new',
            documents: {
                custom_cleared: false,
            },
        })).toMatchSnapshot();
    });
    it('должен спрятать параметр "Состояние"', () => {
        expect(getFields('state', {
            section: 'new',
            state: {
                state_not_beaten: true,
            },
        })).toMatchSnapshot();
    });
    it('должен спрятать параметр "Пробег"', () => {
        expect(getFields('kmAge', {
            section: 'new',
        })).toMatchSnapshot();
    });
    it('должен спрятать параметр "Руль" для леворульных авто', () => {
        expect(getFields('wheel', {
            section: 'new',
            vehicle_info: {
                steering_wheel: 'LEFT',
            },
        })).toMatchSnapshot();
    });
    it('должен выделить параметр "Руль" для праворульных авто', () => {
        expect(getFields('wheel', {
            section: 'new',
            vehicle_info: {
                steering_wheel: 'RIGHT',
            },
        })).toMatchSnapshot();
    });
    it('должен спрятать параметр "Обмен"', () => {
        expect(getFields('exchange', {
            section: 'new',
            additional_info: {
                exchange: true,
            },
        })).toMatchSnapshot();
    });
    it('должен спрятать параметр "ПТС" для оригинала', () => {
        expect(getFields('pts', {
            section: 'new',
            documents: {
                pts: 'ORIGINAL',
            },
        })).toMatchSnapshot();
    });
    it('должен выделить параметр "ПТС" для дубликата', () => {
        expect(getFields('pts', {
            section: 'new',
            documents: {
                pts: 'DUPLICATE',
            },
        })).toMatchSnapshot();
    });
});

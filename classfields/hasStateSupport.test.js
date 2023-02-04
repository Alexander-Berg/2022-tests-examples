const hasStateSupport = require('./hasStateSupport');
const stateSupportDataMock = require('autoru-frontend/mockData/bunker/desktop/state_support.json');

it('должен вернуть true, если объявление удовлетворяет требованиям программ господдержки', () => {
    const offerMock = {
        category: 'cars',
        section: 'new',
        price_info: {
            RUR: 777000,
        },
        vehicle_info: {
            mark_info: {
                code: 'KIA',
            },
            model_info: {
                code: 'RIO',
            },
        },
    };

    expect(hasStateSupport(offerMock, null, stateSupportDataMock)).toBe(true);
});

it('должен вернуть true, если объявление удовлетворяет требованиям программ господдержки с учётом скидки', () => {
    const offerMock = {
        category: 'cars',
        section: 'new',
        seller_type: 'COMMERCIAL',
        price_info: {
            RUR: 1007000,
        },
        vehicle_info: {
            mark_info: {
                code: 'KIA',
            },
            model_info: {
                code: 'RIO',
            },
        },
        discount_options: {
            max_discount: 10000,
        },
    };

    expect(hasStateSupport(offerMock, null, stateSupportDataMock)).toBe(true);
});

it('должен вернуть true, если объявление не подходит по цене, но передан диапазон цен, нижняя граница которого подходит', () => {
    const offerMock = {
        category: 'cars',
        section: 'new',
        price_info: {
            RUR: 1007000,
        },
        vehicle_info: {
            mark_info: {
                code: 'KIA',
            },
            model_info: {
                code: 'RIO',
            },
        },
        discount_options: {
            max_discount: 10000,
        },
    };
    const priceRangeMock = {
        min: {
            price: 777000,
        },
    };

    expect(hasStateSupport(offerMock, priceRangeMock, stateSupportDataMock)).toBe(true);
});

it('должен вернуть false, если гос поддержка отключена в бункере', () => {
    const offerMock = {
        category: 'cars',
        section: 'new',
        price_info: {
            RUR: 777000,
        },
        vehicle_info: {
            mark_info: {
                code: 'KIA',
            },
            model_info: {
                code: 'RIO',
            },
        },
    };

    expect(hasStateSupport(offerMock, null, { ...stateSupportDataMock, isFeatureEnabled: false })).toBe(false);
});

it('должен вернуть false, если объявление не передано', () => {
    expect(hasStateSupport()).toBe(false);
});

it('должен вернуть false, если марка объявления не удовлетворяет требованиям программ господдержки', () => {
    const offerMock = {
        category: 'cars',
        section: 'new',
        price_info: {
            RUR: 777000,
        },
        vehicle_info: {
            mark_info: {
                code: 'BMW',
            },
        },
    };

    expect(hasStateSupport(offerMock, null, stateSupportDataMock)).toBe(false);
});

it('должен вернуть false, если модель объявления не удовлетворяет требованиям программ господдержки', () => {
    const offerMock = {
        category: 'cars',
        section: 'new',
        price_info: {
            RUR: 777000,
        },
        vehicle_info: {
            mark_info: {
                code: 'KIA',
            },
            model_info: {
                code: 'OPTIMA',
            },
        },
    };

    expect(hasStateSupport(offerMock, null, stateSupportDataMock)).toBe(false);
});

it('должен вернуть false, если цена объявления не удовлетворяет требованиям программ господдержки', () => {
    const offerMock = {
        category: 'cars',
        section: 'new',
        price_info: {
            RUR: 1777000,
        },
        vehicle_info: {
            mark_info: {
                code: 'KIA',
            },
            model_info: {
                code: 'RIO',
            },
        },
    };

    expect(hasStateSupport(offerMock, null, stateSupportDataMock)).toBe(false);
});

it('должен вернуть false, если это б/у объявление (не удовлетворяет требованиям программ господдержки)', () => {
    const offerMock = {
        category: 'cars',
        section: 'used',
        price_info: {
            RUR: 1777000,
        },
        vehicle_info: {
            mark_info: {
                code: 'KIA',
            },
            model_info: {
                code: 'RIO',
            },
        },
    };

    expect(hasStateSupport(offerMock, null, stateSupportDataMock)).toBe(false);
});

it('должен вернуть false, если цена объявления, даже с учетом скидки, не удовлетворяет требованиям программ господдержки', () => {
    const offerMock = {
        category: 'cars',
        section: 'new',
        price_info: {
            RUR: 1777000,
        },
        vehicle_info: {
            mark_info: {
                code: 'KIA',
            },
            model_info: {
                code: 'RIO',
            },
        },
        discount_options: {
            max_discount: 10000,
        },
    };

    expect(hasStateSupport(offerMock, null, stateSupportDataMock)).toBe(false);
});

it('должен вернуть false, если это не легковой транспорт (не удовлетворяет требованиям программ господдержки)', () => {
    const offerMock = {
        category: 'trucks',
        section: 'new',
        price_info: {
            RUR: 1777000,
        },
        vehicle_info: {
            mark_info: {
                code: 'KIA',
            },
            model_info: {
                code: 'RIO',
            },
        },
    };

    expect(hasStateSupport(offerMock, null, stateSupportDataMock)).toBe(false);
});

it('должен вернуть true, если шильд удовлетворяет требованиям программ господдержки', () => {
    const offerMock = {
        category: 'cars',
        section: 'new',
        price_info: {
            RUR: 777000,
        },
        vehicle_info: {
            mark_info: {
                code: 'VAZ',
            },
            model_info: {
                code: 'VESTA',
            },
            tech_param: {
                nameplate: 'SPORT',
            },
        },
    };

    expect(hasStateSupport(offerMock, null, stateSupportDataMock)).toBe(true);
});

it('должен вернуть false, если шильд не удовлетворяет требованиям программ господдержки', () => {
    const offerMock = {
        category: 'cars',
        section: 'new',
        price_info: {
            RUR: 777000,
        },
        vehicle_info: {
            mark_info: {
                code: 'VAZ',
            },
            model_info: {
                code: 'VESTA',
            },
            tech_param: {
                nameplate: 'SW',
            },
        },
    };

    expect(hasStateSupport(offerMock, null, stateSupportDataMock)).toBe(false);
});

it('должен вернуть true, если год удовлетворяет требованиям программ господдержки', () => {
    const offerMock = {
        category: 'cars',
        section: 'new',
        price_info: {
            RUR: 777000,
        },
        vehicle_info: {
            mark_info: {
                code: 'SKODA',
            },
            model_info: {
                code: 'RAPID',
            },
        },
        documents: {
            year: 2020,
        },
    };

    expect(hasStateSupport(offerMock, null, stateSupportDataMock)).toBe(true);
});

it('должен вернуть false, если год не удовлетворяет требованиям программ господдержки', () => {
    const offerMock = {
        category: 'cars',
        section: 'new',
        price_info: {
            RUR: 777000,
        },
        vehicle_info: {
            mark_info: {
                code: 'SKODA',
            },
            model_info: {
                code: 'RAPID',
            },
        },
        documents: {
            year: 2017,
        },
    };

    expect(hasStateSupport(offerMock, null, stateSupportDataMock)).toBe(false);
});

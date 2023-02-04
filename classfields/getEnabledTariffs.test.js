const getEnabledTariffs = require('./getEnabledTariffs');

it('должен возращать активные тарифы', () => {
    const state = {
        tariffs: {
            CARS_USED: {
                enabled: true,
            },
            MOTO: {
                enabled: false,
            },
        },
    };

    const expectedResult = {
        CARS_USED: true,
        MOTO: false,
    };

    expect(getEnabledTariffs({ calculator: state })).toEqual(expectedResult);
});

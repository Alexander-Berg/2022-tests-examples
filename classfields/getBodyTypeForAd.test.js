const getBodyTypeForAd = require('./getBodyTypeForAd');

it('должен вернуть suv для ALLROAD_3_DOORS', () => {
    expect(getBodyTypeForAd({
        vehicle_info: {
            configuration: {
                body_type: 'ALLROAD_3_DOORS',
            },
        },
    })).toEqual('suv');
});

it('должен вернуть suv для ALLROAD_5_DOORS', () => {
    expect(getBodyTypeForAd({
        vehicle_info: {
            configuration: {
                body_type: 'ALLROAD_5_DOORS',
            },
        },
    })).toEqual('suv');
});

it('должен вернуть suv для CROSSOVER', () => {
    expect(getBodyTypeForAd({
        vehicle_info: {
            configuration: {
                body_type: 'CROSSOVER',
            },
        },
    })).toEqual('suv');
});

it('должен вернуть "" для SEDAN', () => {
    expect(getBodyTypeForAd({
        vehicle_info: {
            configuration: {
                body_type: 'SEDAN',
            },
        },
    })).toEqual('');
});

it('должен вернуть "", если нет configuration', () => {
    expect(getBodyTypeForAd({
        vehicle_info: {},
    })).toEqual('');
});

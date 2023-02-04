const getSalonOffersCount = require('./getSalonOffersCount');

it('должен вернуть 0, если не салон', () => {
    expect(getSalonOffersCount({ category: 'cars' })).toEqual(0);
});

it('должен вернуть количество легковых по умолчанию', () => {
    expect(getSalonOffersCount({
        category: 'cars',
        salon: {
            offer_counters: {
                cars_all: 5,
                moto_all: 10,
            },
        },
    })).toEqual(5);
});

it('должен вернуть количество авто в переданной категории', () => {
    expect(getSalonOffersCount({
        salon: {
            offer_counters: {
                cars_all: 5,
                moto_all: 10,
            },
        },
        category: 'moto',
    })).toEqual(10);
});

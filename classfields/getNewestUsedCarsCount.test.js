const getNewestUsedCarsCount = require('./getNewestUsedCarsCount');

const state = {
    listingNewestUsedCars: {
        data: {
            pagination: {
                total_offers_count: 42,
            },
        },
    },
};

it('должен вернуть правильное количество самых новых офферов в листинге б/у', () => {
    expect(getNewestUsedCarsCount(state)).toEqual(42);
});

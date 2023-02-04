const getGroupTypeLabel = require('./getGroupTypeLabel');

it('должен вернуть текст для группы новых', () => {
    const offer = {
        groupping_info: {
            offer_counter: {
                new_cars: 10,
            },
        },
        vehicle_info: {
            super_gen: {
                id: 123,
                year_from: 2015,
                year_to: 2019,
            },
        },
    };
    expect(getGroupTypeLabel(offer)).toBe('Новые');
});

it('должен вернуть текст для группы б/у', () => {
    const offer = {
        groupping_info: {
            offer_counter: {
                used_cars: 10,
            },
            production_years: {
                min_year: 2015,
                max_year: 2019,
            },
        },
    };
    expect(getGroupTypeLabel(offer)).toBe('C пробегом (2015-2019)');
});

it('должен вернуть текст для смешанной группы', () => {
    const offer1 = {
        groupping_info: {
            offer_counter: {
                used_cars: 10,
                new_cars: 10,
            },
            production_years: {
                min_year: 2015,
                max_year: 2019,
            },
        },
    };
    const offer2 = {
        groupping_info: {
            offer_counter: {
                cars_all: 10,
            },
            production_years: {
                min_year: 2015,
                max_year: 2019,
            },
        },
    };
    expect(getGroupTypeLabel(offer1)).toBe('Новые и с пробегом (2015-2019)');
    expect(getGroupTypeLabel(offer2)).toBe('Новые и с пробегом (2015-2019)');
});

it('должен вернуть один год, если минимальный и максимальный годы выпуска равны', () => {
    const offer = {
        groupping_info: {
            offer_counter: {
                used_cars: 10,
            },
            production_years: {
                min_year: 2015,
                max_year: 2015,
            },
        },
    };
    expect(getGroupTypeLabel(offer)).toBe('C пробегом (2015)');
});

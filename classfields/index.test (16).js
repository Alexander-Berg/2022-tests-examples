const prepareYears = require('./index');

const GROUPED_OFFERS = [
    { vehicle_info: { super_gen: { year_from: 2017, year_to: 2019 } } },
    { vehicle_info: { super_gen: { year_from: 2018, year_to: 2020 } } },
    { vehicle_info: { super_gen: { year_from: 2019, year_to: 2021 } } },
];

it('должен вернуть массив уникальных тегов из списка офферов', () => {
    expect(
        prepareYears({
            groupedOffers: GROUPED_OFFERS,
        }),
    ).toEqual({ min_year: 2017, max_year: 2021 });
});

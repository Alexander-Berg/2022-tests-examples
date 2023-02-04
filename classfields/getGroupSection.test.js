const getGroupSection = require('./getGroupSection');

it('должен вернуть "new" для группы новых', () => {
    const offer = {
        groupping_info: {
            offer_counter: {
                new_cars: 10,
            },
        },
    };
    expect(getGroupSection(offer)).toBe('new');
});

it('должен вернуть "used" для группы б/у', () => {
    const offer = {
        groupping_info: {
            offer_counter: {
                used_cars: 10,
            },
        },
    };
    expect(getGroupSection(offer)).toBe('used');
});

it('должен вернуть "all" для смешанной группы', () => {
    const offer1 = {
        groupping_info: {
            offer_counter: {
                used_cars: 10,
                new_cars: 10,
            },
        },
    };
    const offer2 = {
        groupping_info: {
            offer_counter: {
                cars_all: 10,
            },
        },
    };
    expect(getGroupSection(offer1)).toBe('all');
    expect(getGroupSection(offer2)).toBe('all');
});

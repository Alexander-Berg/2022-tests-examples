const getSingleMMM = require('./getSingleMMM');

it('должен вернуть mmm, если он один', () => {
    const state = {
        listing: {
            data: {
                search_parameters: {
                    catalog_filter: [
                        { mark: 'AUDI', model: 'A3' },
                    ],
                },
            },
        },
    };
    expect(getSingleMMM(state)).toEqual({ mark: 'AUDI', model: 'A3' });
});

it('должен вернуть null, если mmm больше одного', () => {
    const state = {
        listing: {
            data: {
                search_parameters: {
                    catalog_filter: [
                        { mark: 'AUDI', model: 'A3' },
                        { mark: 'BMW', model: 'X3' },
                    ],
                },
            },
        },
    };
    expect(getSingleMMM(state)).toBeNull();
});

it('должен вернуть null, если нет catalog_filter', () => {
    const state = {
        listing: {
            data: {
                search_parameters: {},
            },
        },
    };
    expect(getSingleMMM(state)).toBeNull();
});

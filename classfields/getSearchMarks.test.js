const getSearchMarks = require('./getSearchMarks');

it('должен выбрать марки из параметров листинга', () => {
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
    expect(getSearchMarks(state)).toEqual([ 'AUDI', 'BMW' ]);
});

it('должен вернуть пустой массив, если нет catalog_filter', () => {
    const state = {
        listing: {
            data: {
                search_parameters: {},
            },
        },
    };
    expect(getSearchMarks(state)).toEqual([]);
});

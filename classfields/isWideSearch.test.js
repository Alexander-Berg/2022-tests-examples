const isWideSearch = require('./isWideSearch');

it('должен вернуть true, если не выбран регион и не выбрана марка и не секция новых авто', () => {
    const state = {
        geo: {
            gids: [],
        },
        listing: {
            data: {
                search_parameters: {
                    section: 'all',
                },
            },
        },
    };
    expect(isWideSearch(state)).toBe(true);
});

it('должен вернуть true, если не выбран регион и в каталог фильтре нет марок и не секция новых авто', () => {
    const state = {
        geo: {
            gids: [],
        },
        listing: {
            data: {
                search_parameters: {
                    section: 'all',
                    catalog_filter: [ { vendoor: 'VENDOR1' } ],
                },
            },
        },
    };
    expect(isWideSearch(state)).toBe(true);
});

it('должен вернуть false, если выбрана секция новых авто', () => {
    const state = {
        geo: {
            gids: [],
        },
        listing: {
            data: {
                search_parameters: {
                    section: 'new',
                },
            },
        },
    };
    expect(isWideSearch(state)).toBe(false);
});

it('должен вернуть false, если выбран регион', () => {
    const state = {
        geo: {
            gids: [ 213 ],
        },
        listing: {
            data: {
                search_parameters: {
                    section: 'all',
                },
            },
        },
    };
    expect(isWideSearch(state)).toBe(false);
});

it('должен вернуть false, если выбрана марка', () => {
    const state = {
        geo: {
            gids: [],
        },
        listing: {
            data: {
                search_parameters: {
                    catalog_filter: [ { mark: 'AUDI' } ],
                    section: 'all',
                },
            },
        },
    };
    expect(isWideSearch(state)).toBe(false);
});

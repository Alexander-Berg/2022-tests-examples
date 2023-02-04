const getFiltersFromMmms = require('./getFiltersFromMmms');

it('Должен отделить catalof-filter от exclude-catalog-filter', () => {
    const mmmItems = [
        { mark: 'AUDI', model: 'A3' },
        { mark: 'BMW', model: 'X3', exclude: true },
    ];
    const filters = {
        catalogFilter: [
            { mark: 'AUDI', model: 'A3' },
        ],
        excludeCatalogFilter: [
            { mark: 'BMW', model: 'X3' },
        ],
    };
    expect(getFiltersFromMmms(mmmItems)).toEqual(filters);
});

it('Должен отделить catalof-filter от exclude-catalog-filter (исключение пустое)', () => {
    const mmmItems = [
        { mark: 'AUDI', model: 'A3' },
        { mark: 'BMW', model: 'X3' },
    ];
    const filters = {
        catalogFilter: [
            { mark: 'AUDI', model: 'A3' },
            { mark: 'BMW', model: 'X3' },
        ],
        excludeCatalogFilter: [],
    };
    expect(getFiltersFromMmms(mmmItems)).toEqual(filters);
});

it('Должен отделить catalof-filter от exclude-catalog-filter (только исключение)', () => {
    const mmmItems = [
        { mark: 'AUDI', model: 'A3', exclude: true },
        { mark: 'BMW', model: 'X3', exclude: true },
    ];
    const filters = {
        excludeCatalogFilter: [
            { mark: 'AUDI', model: 'A3' },
            { mark: 'BMW', model: 'X3' },
        ],
        catalogFilter: [],
    };
    expect(getFiltersFromMmms(mmmItems)).toEqual(filters);
});

it('Должен убрать условие с повторяющейся маркой, если не указана модель', () => {
    const mmmItems = [
        { mark: 'AUDI', model: 'A3' },
        { mark: 'AUDI' },
    ];
    const filters = {
        catalogFilter: [
            { mark: 'AUDI', model: 'A3' },
        ],
        excludeCatalogFilter: [],
    };
    expect(getFiltersFromMmms(mmmItems)).toEqual(filters);
});

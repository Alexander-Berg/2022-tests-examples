const getAllMmmItems = require('./getAllMmmItems');

it('Должен преобразовать catalog_filter и exclude_catalog_filter в массив mmmItems', () => {
    const catalogFilter = [
        { mark: 'AUDI', model: 'A3' },
    ];
    const excludeCatalogFilter = [
        { mark: 'BMW', model: 'X3' },
    ];
    const mmmItems = [
        { mark: 'AUDI', model: 'A3' },
        { mark: 'BMW', model: 'X3', exclude: true },
    ];
    expect(getAllMmmItems(catalogFilter, excludeCatalogFilter)).toEqual(mmmItems);
});

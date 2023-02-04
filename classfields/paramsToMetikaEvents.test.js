const paramsToMetrikaEvents = require('./paramsToMetrikaEvents');

it('должен правильно преобразовать параметры', () => {
    const params = {
        price_from: 1000,
        from_to: 100000,
    };

    expect(paramsToMetrikaEvents(params)).toEqual([
        [ 'price_from', 1000 ],
        [ 'from_to', 100000 ],
    ]);
});

it('должен правильно преобразовать параметры с action', () => {
    const params = {
        price_from: 1000,
        from_to: 100000,
    };

    expect(paramsToMetrikaEvents(params, 'send')).toEqual([
        [ 'price_from', 'send', 1000 ],
        [ 'from_to', 'send', 100000 ],
    ]);
});

it('должен правильно преобразовать catalog_filter', () => {
    const params = {
        catalog_filter: [ { mark: 'BMW' }, { mark: 'AUDI', model: 'A4' } ],
        category: 'cars',
        section: 'all',
        from: 'searchline',
    };

    expect(paramsToMetrikaEvents(params)).toEqual([
        [ 'catalog_filter', 'mark=AUDI,model=A4,mark=BMW' ],
        [ 'category', 'cars' ],
        [ 'section', 'all' ],
        [ 'from', 'searchline' ],
    ]);
});

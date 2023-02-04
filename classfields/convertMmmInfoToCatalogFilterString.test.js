const convertMmmInfoToCatalogFilterString = require('./convertMmmInfoToCatalogFilterString');

it('должен преобразовать объект в значение', () => {
    expect(convertMmmInfoToCatalogFilterString([
        { mark: 'AUDI', model: 'A3' },
        { mark: 'AUDI', model: 'A4' } ]),
    ).toEqual([
        'mark=AUDI,model=A3',
        'mark=AUDI,model=A4',
    ]);
});

it('должен вернуть пустую строку', () => {
    expect(convertMmmInfoToCatalogFilterString([ {} ])).toEqual([]);
});

it('должен преобразовать объект в значение с соблюдением порядка', () => {
    expect(convertMmmInfoToCatalogFilterString([
        { tech_param: '123', model: 'A3', generation: '000', mark: 'AUDI' },
    ]),
    ).toEqual([
        'mark=AUDI,model=A3,generation=000,tech_param=123',
    ]);
});

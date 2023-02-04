const convertCatalogFilterStringToMmmItem = require('./convertCatalogFilterStringToMmmItem');

it('должен преобразовать значение в объект', () => {
    expect(convertCatalogFilterStringToMmmItem('mark=AUDI,model=A3')).toEqual({ mark: 'AUDI', model: 'A3' });
});

it('должен вернуть пустой объект для пустого значения', () => {
    expect(convertCatalogFilterStringToMmmItem()).toEqual({});
});

it('должен правильно распарсить дважды заэнкоженный параметр', () => {
    expect(convertCatalogFilterStringToMmmItem('mark%3DGAZ%2Cmodel%3DGAZEL_3302')).toEqual({ mark: 'GAZ', model: 'GAZEL_3302' });
});

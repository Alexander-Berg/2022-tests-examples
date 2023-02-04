const adfoxAdTargetPuids = require('./adfoxAdTargetPuids');

it('должен вернуть {}, если нет объявления', () => {
    expect(adfoxAdTargetPuids()).toEqual({});
});

it('должен вернуть {}, если объявление без фотки', () => {
    expect(adfoxAdTargetPuids({ model: 'A4' })).toEqual({});
});

it('должен вернуть puidы для adTarget', () => {
    expect(adfoxAdTargetPuids({
        brand: 'brand_value',
        image: 'https://image',
        model: 'model_value',
        price: 500000,
        year: 'year_value',
    })).toEqual({
        puid2: 'brand_value',
        puid3: 'model_value',
        puid4: 'year_value',
        puid5: '500000-599999',
        puid63: 'https://image',
    });
});

const pricesFromTo = require('./pricesFromTo');

it('должен вернуть одну цену для обычного объявления без группы и скидки', () => {
    const offer = pricesFromTo({
        price_info: { EUR: 1, RUR: 2, USD: 3 },
    }, { currency: 'RUR' });

    expect(offer).toEqual([
        { prefix: '', price: { EUR: 1, RUR: 2, USD: 3 } },
    ]);
});

it('должен вернуть цены от и до для группы', () => {
    const offer = pricesFromTo({
        groupping_info: {
            price_from: { EUR: 10, RUR: 11, USD: 12 },
            price_to: { EUR: 20, RUR: 21, USD: 22 },
            size: 10,
        },
        price_info: { EUR: 1, RUR: 2, USD: 3 },
    }, { currency: 'RUR' });

    expect(offer).toEqual([
        { prefix: 'от ', price: { EUR: 10, RUR: 11, USD: 12 } },
        { prefix: 'до ', price: { EUR: 20, RUR: 21, USD: 22 } },
    ]);
});

it('должен вернуть цены до и от для группы, если сортировка от дорогих к дешевым', () => {
    const offer = pricesFromTo({
        groupping_info: {
            price_from: { EUR: 10, RUR: 11, USD: 12 },
            price_to: { EUR: 20, RUR: 21, USD: 22 },
        },
        price_info: { EUR: 1, RUR: 2, USD: 3 },
    }, { currency: 'RUR', sort: 'price-desc' });

    expect(offer).toEqual([
        { prefix: 'до ', price: { EUR: 20, RUR: 21, USD: 22 } },
        { prefix: 'от ', price: { EUR: 10, RUR: 11, USD: 12 } },
    ]);
});

it('для нового со скидкой должен вернуть две цены', () => {
    const offer = pricesFromTo({
        price_info: { RUR: 300000 },
        discount_options: { max_discount: 10000 },
        section: 'new',
    }, { currency: 'RUR' });

    expect(offer).toEqual([
        { prefix: 'от ', price: { RUR: 290000 } },
        { discount: true, prefix: 'до ', price: { RUR: 300000 } },
    ]);
});

it('не должен вернуть цену для проданного объявления', () => {
    const offer = pricesFromTo({
        price_info: {},
    }, { currency: 'RUR' });

    expect(offer).toEqual([
        { prefix: '', price: { } },
    ]);
});

it('должен вернуть цены от и до со скидками для обычного объявления', () => {
    const offer = pricesFromTo({
        section: 'new',
        discount_options: { max_discount: 42 },
        price_info: { EUR: 101, RUR: 102, USD: 103 },
        seller_type: 'COMMERCIAL',
    }, { currency: 'RUR' });

    expect(offer).toEqual([
        { prefix: 'от ', price: { EUR: 101, RUR: 60, USD: 103 } },
        { discount: true, prefix: 'до ', price: { EUR: 101, RUR: 102, USD: 103 } },
    ]);
});

it('не должен вернуть цены от и до со скидками для бу объявления', () => {
    const offer = pricesFromTo({
        section: 'used',
        discount_options: { max_discount: 42 },
        price_info: { EUR: 101, RUR: 102, USD: 103 },
        seller_type: 'COMMERCIAL',
    }, { currency: 'RUR' });

    expect(offer).toEqual([
        { prefix: '', price: { EUR: 101, RUR: 102, USD: 103 } },
    ]);
});

it('не должен вернуть цены со скидками для обычного объявления, если валюта не RUR', () => {
    const offer = pricesFromTo({
        section: 'new',
        discount_options: { max_discount: 42 },
        price_info: { EUR: 101, RUR: 102, USD: 103 },
        seller_type: 'COMMERCIAL',
    }, { currency: 'USD' });

    expect(offer).toEqual([
        { prefix: '', price: { EUR: 101, RUR: 102, USD: 103 } },
    ]);
});

it('не должен вернуть цены со скидками для группы', () => {
    const offer = pricesFromTo({
        groupping_info: {
            price_from: { EUR: 10, RUR: 11, USD: 12 },
            price_to: { EUR: 20, RUR: 21, USD: 22 },
            size: 10,
        },
        discount_options: { max_discount: 42 },
        price_info: { EUR: 101, RUR: 102, USD: 103 },
        seller_type: 'COMMERCIAL',
    }, { currency: 'RUR' });

    expect(offer).toEqual([
        { prefix: 'от ', price: { EUR: 10, RUR: 11, USD: 12 } },
        { prefix: 'до ', price: { EUR: 20, RUR: 21, USD: 22 } },
    ]);
});

it('должен вернуть цены со скидками для группы c одним объявлением', () => {
    const offer = pricesFromTo({
        section: 'new',
        groupping_info: {
            price_from: { EUR: 101, RUR: 102, USD: 103 },
            price_to: { EUR: 101, RUR: 102, USD: 103 },
            size: 1,
        },
        discount_options: { max_discount: 42 },
        price_info: { EUR: 101, RUR: 102, USD: 103 },
        seller_type: 'COMMERCIAL',
    }, { currency: 'RUR' });

    expect(offer).toEqual([
        { prefix: 'от ', price: { EUR: 101, RUR: 60, USD: 103 } },
        { discount: true, prefix: 'до ', price: { EUR: 101, RUR: 102, USD: 103 } },
    ]);
});

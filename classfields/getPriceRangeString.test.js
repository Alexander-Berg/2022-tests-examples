const getPriceRangeString = require('./getPriceRangeString');
const { nbsp } = require('auto-core/react/lib/html-entities');

const PRICE_FROM_OBJECT = {
    RUR: 1000500,
};

const PRICE_TO_OBJECT = {
    RUR: 12000000,
};

it('должен вернуть пустую строку при отсутствии первого параметра', () => {
    expect(getPriceRangeString(undefined, PRICE_TO_OBJECT)).toEqual('');
    expect(getPriceRangeString()).toEqual('');
});

it('должен вернуть строку с диапазоном цен, если в переданных объектах цены отличаются', () => {
    expect(
        getPriceRangeString(PRICE_FROM_OBJECT, PRICE_TO_OBJECT).replace(new RegExp(nbsp, 'g'), ' '),
    ).toEqual('1 000 500 – 12 000 000 ₽');
});

it('должен вернуть строку с одной ценой, если в переданных объектах цены совпадают', () => {
    expect(
        getPriceRangeString(PRICE_FROM_OBJECT, PRICE_FROM_OBJECT).replace(new RegExp(nbsp, 'g'), ' '),
    ).toEqual('1 000 500 ₽');
});

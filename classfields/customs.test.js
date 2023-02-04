const customsPreparer = require('./customs');

describe('карточка', () => {
    const isCardPage = true;

    it('неоплаченный отчет', () => {
        const data = { item: { status: 'UNKNOWN' }, isCardPage };
        const expected = { key: 'Таможня' };
        expect(customsPreparer(data)).toEqual(expected);
    });

    it('оплаченный, есть записи', () => {
        const data = { item: { status: 'OK', record_count: 2 }, isCardPage };
        const expected = { key: '2 записи о прохождении таможни' };
        expect(customsPreparer(data)).toEqual(expected);
    });

    it('оплаченный, нет записей', () => {
        const data = { item: { status: 'OK' }, isCardPage };
        const expected = { key: 'Автомобиль не ввозили в РФ' };
        expect(customsPreparer(data)).toEqual(expected);
    });
});

describe('стендалоун', () => {
    it('неоплаченный отчет', () => {
        const data = { item: { status: 'UNKNOWN' } };
        const expected = { key: 'Таможня' };
        expect(customsPreparer(data)).toEqual(expected);
    });

    it('оплаченный, есть записи', () => {
        const data = { item: { status: 'OK', record_count: 2 } };
        const expected = { key: 'Таможня', value: '2 записи' };
        expect(customsPreparer(data)).toEqual(expected);
    });

    it('оплаченный, нет записей', () => {
        const data = { item: { status: 'OK' } };
        const expected = { key: 'Таможня', value: 'Нет записей' };
        expect(customsPreparer(data)).toEqual(expected);
    });
});

const preparer = require('./estimates');

describe('на карточке', () => {
    const isCardPage = true;

    it('оценка от партнера - status UNKNOWN', () => {
        const data = { item: { status: 'UNKNOWN' }, isCardPage };
        const expected = { key: 'Оценка от партнёров появится чуть позже' };
        expect(preparer(data)).toEqual(expected);
    });

    it('оценка от партнера - во всех остальных случаях', () => {
        const data = { item: { status: 'OK' }, isCardPage };
        const expected = { key: 'Оценка от партнёров' };
        expect(preparer(data)).toEqual(expected);
    });
});

describe('стендалоун', () => {
    const isCardPage = false;

    it('оценка от партнера - status UNKNOWN', () => {
        const data = { item: { status: 'UNKNOWN' }, isCardPage };
        const expected = { key: 'Оценка от партнёров', value: 'Информация появится чуть позже' };
        expect(preparer(data)).toEqual(expected);
    });

    it('оценка от партнера - есть записи', () => {
        const data = { item: { status: 'OK', record_count: 2 }, isCardPage };
        const expected = { key: 'Оценка от партнёров', value: '2 записи' };
        expect(preparer(data)).toEqual(expected);
    });

    it('оценка от партнера - нет записей', () => {
        const data = { item: { status: 'OK' }, isCardPage };
        const expected = { key: 'Оценка от партнёров', value: 'Нет записей' };
        expect(preparer(data)).toEqual(expected);
    });
});

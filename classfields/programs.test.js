const preparer = require('./programs');

let isCardPage;

const BLOCK_KEY = 'Программы производителей';

describe('карточка:', () => {
    isCardPage = true;

    it('вернет правильный результат', () => {
        const item = { status: 'OK' };
        const data = preparer({ item, isCardPage });

        expect(data).toEqual({
            key: BLOCK_KEY,
        });
    });

    it('ничего не вернет', () => {
        const item = { status: 'ERROR' };
        const data = preparer({ item, isCardPage });

        expect(data).toBeUndefined();
    });
});

describe('отчет:', () => {
    beforeEach(() => {
        isCardPage = false;
    });

    it('status === OK: вернет правильный результат', () => {
        const item = { status: 'OK', record_count: 2 };
        const data = preparer({ item, isCardPage });

        expect(data).toEqual({
            key: 'Программы производителей',
            value: '2 записи',
        });
    });

    it('status === UNKNOWN: вернет правильный результат', () => {
        const item = { status: 'UNKNOWN' };
        const data = preparer({ item, isCardPage });

        expect(data).toEqual({
            key: 'Программы производителей',
            value: 'Нет записей',
        });
    });

    it('ничего не вернет', () => {
        const item = {};
        const data = preparer({ item, isCardPage });

        expect(data).toBeUndefined();
    });
});

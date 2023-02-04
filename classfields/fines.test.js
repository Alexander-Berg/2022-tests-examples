const preparer = require('./fines');

let isCardPage;

describe('карточка:', () => {
    beforeEach(() => {
        isCardPage = true;
    });

    describe('status OK', () => {
        it('если нет штрафов', () => {
            const item = { status: 'OK', type: 'fines' };
            const data = preparer({ item, isCardPage });

            expect(data).toEqual({ key: 'Все штрафы оплачены' });
        });
    });

    describe('status ERROR', () => {
        it('если есть штрафы', () => {
            const item = { status: 'ERROR', type: 'fines', record_count: 10 };
            const data = preparer({ item, isCardPage });

            expect(data).toEqual({ key: '10 неоплаченных штрафов' });
        });
    });

    describe('status undefined', () => {
        it('если нет штрафов', () => {
            const item = { type: 'fines' };
            const data = preparer({ item, isCardPage });

            expect(data).toEqual({ key: 'Поиск данных о штрафах' });
        });
    });

    it('пока непонятно, что с штрафами', () => {
        const item = { status: 'UNKNOWN', type: 'fines' };
        const data = preparer({ item, isCardPage });

        expect(data).toEqual({ key: 'Информация о штрафах появится позже' });
    });
});

describe('отчет:', () => {
    beforeEach(() => {
        isCardPage = false;
    });

    describe('status OK', () => {
        it('если нет штрафов', () => {
            const item = { status: 'OK', type: 'fines' };
            const data = preparer({ item, isCardPage });

            expect(data).toEqual({ key: 'Штрафы', value: 'Нет неоплаченных' });
        });
    });

    describe('status ERROR', () => {
        it('если есть штрафы', () => {
            const item = { status: 'ERROR', type: 'fines', record_count: 10 };
            const data = preparer({ item, isCardPage });

            expect(data).toEqual({ key: 'Штрафы', value: '10 неоплаченных' });
        });
    });

    describe('status undefined', () => {
        it('если нет штрафов', () => {
            const item = { type: 'fines' };
            const data = preparer({ item, isCardPage });

            expect(data).toEqual({ key: 'Штрафы' });
        });
    });

    it('пока непонятно, что с штрафами', () => {
        const item = { status: 'UNKNOWN', type: 'fines' };
        const data = preparer({ item, isCardPage });

        expect(data).toEqual({ key: 'Штрафы', value: 'Информация появится позже' });
    });
});

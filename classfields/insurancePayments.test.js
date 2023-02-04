const { nbsp } = require('auto-core/react/lib/html-entities');

const preparer = require('./insurancePayments');

let isCardPage;

describe('карточка:', () => {
    beforeEach(() => {
        isCardPage = true;
    });

    it('если есть страховые выплаты', () => {
        const item = { status: 'OK', record_count: 10 };
        const data = preparer({ item, isCardPage });

        expect(data).toEqual({ key: `10${ nbsp }страховых выплат` });
    });

    it('если нет страховых выплат', () => {
        const item = { status: 'OK' };
        const data = preparer({ item, isCardPage });

        expect(data).toEqual({ key: `Страховые выплаты не${ nbsp }найдены` });
    });

    it('пока непонятно, что со страховыми выплатами', () => {
        const item = { status: 'UNKNOWN' };
        const data = preparer({ item, isCardPage });

        expect(data).toEqual({ key: 'Поиск страховых выплат' });
    });
});

describe('отчет:', () => {
    beforeEach(() => {
        isCardPage = false;
    });

    it('если есть страховые выплаты', () => {
        const item = { status: 'OK', record_count: 10 };
        const data = preparer({ item, isCardPage });

        expect(data).toEqual({ key: 'Страховые выплаты', value: `10${ nbsp }записей` });
    });

    it('если нет страховых выплат', () => {
        const item = { status: 'OK' };
        const data = preparer({ item, isCardPage });

        expect(data).toEqual({ key: 'Страховые выплаты', value: 'Нет записей' });
    });

    it('пока непонятно, что со страховыми выплатами', () => {
        const item = { status: 'UNKNOWN' };
        const data = preparer({ item, isCardPage });

        expect(data).toEqual({ key: 'Страховые выплаты', value: 'Информация появится чуть позже' });
    });
});

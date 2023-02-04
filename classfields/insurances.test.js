const preparer = require('./insurances');

let isCardPage;

describe('карточка:', () => {
    beforeEach(() => {
        isCardPage = true;
    });

    it('если есть полисы', () => {
        const item = { status: 'OK', record_count: 10 };
        const data = preparer({ item, isCardPage });

        expect(data).toEqual({ key: '10 страховых полисов' });
    });

    it('если нет полисов', () => {
        const item = { status: 'OK' };
        const data = preparer({ item, isCardPage });

        expect(data).toEqual({ key: 'Не обнаружили страховые полисы' });
    });

    it('пока непонятно, что с полисами', () => {
        const item = { status: 'UNKNOWN' };
        const data = preparer({ item, isCardPage });

        expect(data).toEqual({ key: 'Информация о страховых полисах появится позже' });
    });
});

describe('отчет:', () => {
    beforeEach(() => {
        isCardPage = false;
    });

    it('если есть полисы', () => {
        const item = { status: 'OK', record_count: 10 };
        const data = preparer({ item, isCardPage });

        expect(data).toEqual({ key: 'Страховые полисы', value: '10 записей' });
    });

    it('если нет полисов', () => {
        const item = { status: 'OK' };
        const data = preparer({ item, isCardPage });

        expect(data).toEqual({ key: 'Страховые полисы', value: 'Нет записей' });
    });

    it('пока непонятно, что с полисами', () => {
        const item = { status: 'UNKNOWN' };
        const data = preparer({ item, isCardPage });

        expect(data).toEqual({ key: 'Страховые полисы', value: 'Информация появится позже' });
    });
});

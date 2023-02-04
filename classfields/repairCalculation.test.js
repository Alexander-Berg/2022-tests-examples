const preparer = require('./repairCalculation');

let isCardPage;

describe('карточка:', () => {
    beforeEach(() => {
        isCardPage = true;
    });

    it('status === undefined: вернет правильный результат', () => {
        const item = {};
        const data = preparer({ item, isCardPage });

        expect(data).toEqual({
            key: 'Поиск оценок стоимости ремонта',
        });
    });

    it('status === OK: если нет значения, ничего не вернет', () => {
        const item = { status: 'OK' };
        const data = preparer({ item, isCardPage });

        expect(data).toBeUndefined();
    });

    it('status === OK: если есть кол-во, вернет правильный результат', () => {
        const item = { status: 'OK', record_count: 11 };
        const data = preparer({ item, isCardPage });

        expect(data).toEqual({
            key: '11 расчётов стоимости ремонта',
        });
    });

    it('status === LOCKED', () => {
        const item = { status: 'LOCKED' };
        const data = preparer({ item, isCardPage });

        expect(data).toEqual({
            key: 'Рассчёт оценки стоимости ремонта недоступен',
        });
    });
});

describe('отчет:', () => {
    beforeEach(() => {
        isCardPage = false;
    });

    it('если есть кол-во, вернет правильный результат', () => {
        const item = { status: 'OK', record_count: 11 };
        const data = preparer({ item, isCardPage });

        expect(data).toEqual({
            key: 'Расчёт стоимости ремонта',
            value: '11 записей',
        });
    });

    it('если ничего нет, вернет правильный результат', () => {
        const item = {};
        const data = preparer({ item, isCardPage });

        expect(data).toEqual({
            key: 'Расчёт стоимости ремонта',
            value: 'Нет записей',
        });
    });

    it('если status === LOCKED, сообщит о необходимости регистрации', () => {
        const item = { status: 'LOCKED' };
        const data = preparer({ item, isCardPage });

        expect(data).toEqual({
            key: 'Расчёт стоимости ремонта',
            value: 'Рассчёт недоступен',
        });
    });
});

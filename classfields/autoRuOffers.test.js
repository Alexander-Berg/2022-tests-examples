const preparer = require('./autoRuOffers');

let isCardPage;

describe('карточка:', () => {
    beforeEach(() => {
        isCardPage = true;
    });

    it('status === ERROR: ничего не вернет', () => {
        const item = { status: 'ERROR' };
        const data = preparer({ item, isCardPage });

        expect(data).toEqual({});
    });

    it('status === OK: если есть оффер и кол-во не 1, вернет правильный результат', () => {
        const item = { status: 'OK', record_count: 3 };
        const result = { report: { offer_id: '123' } };
        const data = preparer({ item, isCardPage, result });

        expect(data).toMatchSnapshot();
    });

    it('status === OK: если есть оффер и кол-во 1, вернет правильный результат', () => {
        const item = { status: 'OK', record_count: 1 };
        const result = { report: { offer_id: '123' } };
        const data = preparer({ item, isCardPage, result });

        expect(data).toMatchSnapshot();
    });

    it('status === OK: если нет оффера и есть кол-во, вернет правильный результат', () => {
        const item = { status: 'OK', record_count: 2 };
        const data = preparer({ item, isCardPage });

        expect(data).toMatchSnapshot();
    });

    it('status === OK: если нет оффера и нет кол-ва, ничего не вернет', () => {
        const item = { status: 'OK' };
        const data = preparer({ item, isCardPage });

        expect(data).toBeUndefined();
    });
});

describe('отчет:', () => {
    beforeEach(() => {
        isCardPage = false;
    });

    it('status === ERROR: вернет флаг для удаления данных', () => {
        const item = { status: 'ERROR' };
        const data = preparer({ item, isCardPage });

        expect(data).toMatchSnapshot();
    });

    it('status === OK: если есть оффер и кол-во не 1, вернет правильный результат', () => {
        const item = { status: 'OK', record_count: 3 };
        const result = { report: { offer_id: '123' } };
        const data = preparer({ item, isCardPage, result });

        expect(data).toMatchSnapshot();
    });

    it('status === OK: если есть оффер и кол-во 1, вернет правильный результат', () => {
        const item = { status: 'OK', record_count: 1 };
        const result = { report: { offer_id: '123' } };
        const data = preparer({ item, isCardPage, result });

        expect(data).toMatchSnapshot();
    });

    it('status === OK: если нет оффера и есть кол-во, вернет правильный результат', () => {
        const item = { status: 'OK', record_count: 2 };
        const data = preparer({ item, isCardPage });

        expect(data).toMatchSnapshot();
    });

    it('status === OK: если нет оффера и нет кол-ва, вернет флаг для удаления данных', () => {
        const item = { status: 'OK' };
        const data = preparer({ item, isCardPage });

        expect(data).toMatchSnapshot();
    });
});

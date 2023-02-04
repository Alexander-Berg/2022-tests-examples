const preparer = require('./ptsOwners');

const result = {
    report: {
        offer_id: '123',
    },
};

let isCardPage;

describe('карточка:', () => {
    beforeEach(() => {
        isCardPage = true;
    });

    it('если есть кол-во, вернет правильный результат', () => {
        const item = { record_count: 10 };
        const data = preparer({ item, isCardPage, result });

        expect(data).toMatchSnapshot();
    });

    it('если нет кол-ва, ничего не вернет', () => {
        const item = { };
        const data = preparer({ item, isCardPage, result });

        expect(data).toBeUndefined();
    });
});

describe('отчет:', () => {
    beforeEach(() => {
        isCardPage = false;
    });

    it('status === OK: если есть кол-во, вернет правильный результат', () => {
        const item = { status: 'OK', record_count: 10 };
        const data = preparer({ item, isCardPage, result });

        expect(data).toMatchSnapshot();
    });

    it('status === OK: если нет кол-ва, вернет флаг для удаления данных', () => {
        const item = { status: 'OK' };
        const data = preparer({ item, isCardPage, result });

        expect(data).toMatchSnapshot();
    });

    it('status === UNKNOWN: если есть владельцы в объявлении и их кол-во не совпадает с записями, вернет правильный результат', () => {
        const resultWithOwners = { report: {
            offer_id: '123',
            pts_owners: { owners_count_offer: 3 },
        } };
        const item = { status: 'UNKNOWN', record_count: 10 };
        const data = preparer({ item, isCardPage, result: resultWithOwners });

        expect(data).toMatchSnapshot();
    });

    it('status === UNKNOWN: если владельцев в объявлении нет, вернет правильный результат', () => {
        const item = { status: 'UNKNOWN', record_count: 10 };
        const data = preparer({ item, isCardPage, result });

        expect(data).toMatchSnapshot();
    });

    it('status === UNKNOWN: нет объявления, вернет флаг для удаления данных', () => {
        const item = { status: 'UNKNOWN', record_count: 10 };
        const data = preparer({ item, isCardPage, result: {} });

        expect(data).toMatchSnapshot();
    });
});

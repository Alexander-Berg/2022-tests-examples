const preparer = require('./dtp');

let isCardPage;

describe('карточка:', () => {
    beforeEach(() => {
        isCardPage = true;
    });

    it('status === ERROR: вернет правильный результат, если есть ДТП', () => {
        const item = { status: 'ERROR', record_count: 2 };
        const data = preparer({ item, isCardPage });

        expect(data).toMatchSnapshot();
    });

    it('status === ERROR: вернет правильный результат, если нет ДТП', () => {
        const item = { status: 'ERROR' };
        const data = preparer({ item, isCardPage });

        expect(data).toMatchSnapshot();
    });

    it('status === UNKNOWN: вернет правильный результат', () => {
        const item = { status: 'UNKNOWN' };
        const data = preparer({ item, isCardPage });

        expect(data).toMatchSnapshot();
    });

    it('status === NOT_VISIBLE, record_count === -1: вернет правильный результат', () => {
        const item = { record_count: -1 };
        const data = preparer({ item, isCardPage });

        expect(data).toMatchSnapshot();
    });

    it('status === OK: вернет правильный результат', () => {
        const item = { status: 'OK' };
        const data = preparer({ item, isCardPage });

        expect(data).toMatchSnapshot();
    });

    it('если record_count -1 вернет правильный результат (без количества)', () => {
        const experimentsData = { has: (s) => s === 'AUTORUFRONT-16286_no_number' };
        const item = { status: 'FOUND', record_count: -1 };
        const data = preparer({ item, isCardPage, experimentsData });

        expect(data).toMatchSnapshot();
    });
});

describe('отчет:', () => {
    beforeEach(() => {
        isCardPage = false;
    });

    it('status === ERROR: вернет правильный результат, если есть ДТП', () => {
        const item = { status: 'ERROR', record_count: 2 };
        const data = preparer({ item, isCardPage });

        expect(data).toMatchSnapshot();
    });

    it('status === ERROR: вернет правильный результат, если нет ДТП', () => {
        const item = { status: 'ERROR' };
        const data = preparer({ item, isCardPage });

        expect(data).toMatchSnapshot();
    });

    it('status === UNKNOWN: вернет правильный результат', () => {
        const item = { status: 'UNKNOWN' };
        const data = preparer({ item, isCardPage });

        expect(data).toMatchSnapshot();
    });

    it('status === OK: вернет правильный результат', () => {
        const item = { status: 'OK' };
        const data = preparer({ item, isCardPage });

        expect(data).toMatchSnapshot();
    });
});

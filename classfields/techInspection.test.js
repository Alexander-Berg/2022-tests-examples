const preparer = require('./techInspection');

let isCardPage;

describe('карточка', () => {
    beforeEach(() => {
        isCardPage = true;
    });

    it('не найдены', () => {
        const item = { status: 'OK' };
        const data = preparer({ item, isCardPage });

        expect(data).toMatchSnapshot();
    });

    it('найдены 2 записи', () => {
        const item = { status: 'OK', record_count: 2 };
        const data = preparer({ item, isCardPage });

        expect(data).toMatchSnapshot();
    });

    it('поиск', () => {
        const item = { status: 'UNKNOWN' };
        const data = preparer({ item, isCardPage });

        expect(data).toMatchSnapshot();
    });
});

describe('стендалоун', () => {
    beforeEach(() => {
        isCardPage = false;
    });

    it('не найдены', () => {
        const item = { status: 'OK' };
        const data = preparer({ item, isCardPage });

        expect(data).toMatchSnapshot();
    });

    it('найдены 2 записи', () => {
        const item = { status: 'OK', record_count: 2 };
        const data = preparer({ item, isCardPage });

        expect(data).toMatchSnapshot();
    });

    it('поиск', () => {
        const item = { status: 'UNKNOWN' };
        const data = preparer({ item, isCardPage });

        expect(data).toMatchSnapshot();
    });
});

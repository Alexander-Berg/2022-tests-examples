const preparer = require('./pts');

let isCardPage;

const result = {
    report: {
        offer_id: '123',
        pts_info: {
            header: {},
        },
    },
};

describe('карточка:', () => {
    beforeEach(() => {
        isCardPage = true;
    });

    it('status === ERROR: вернет правильный результат, если есть оффер', () => {
        const item = { status: 'ERROR' };
        const data = preparer({ item, isCardPage, result });

        expect(data).toMatchSnapshot();
    });

    it('status === ERROR: ничего не вернет, если нет оффера', () => {
        const item = { status: 'ERROR' };
        const data = preparer({ item, isCardPage });

        expect(data).toBeUndefined();
    });

    it('status === UNKNOWN: вернет правильный результат', () => {
        const item = { status: 'UNKNOWN' };
        result.report.pts_info.header = {
            is_updating: false,
        };

        const data = preparer({ item, isCardPage, result });

        expect(data).toMatchSnapshot();
    });

    it('status === UNKNOWN и is_updating = true: вернет правильный результат', () => {
        const item = { status: 'UNKNOWN' };
        result.report.pts_info.header = {
            is_updating: true,
        };
        const data = preparer({ item, isCardPage, result });

        expect(data).toMatchSnapshot();
    });

    it('status === OK: вернет правильный результат', () => {
        const item = { status: 'OK' };
        const data = preparer({ item, isCardPage });

        expect(data).toMatchSnapshot();
    });

    it('status === undefined: ничего не вернет', () => {
        const item = { };
        const data = preparer({ item, isCardPage });

        expect(data).toBeUndefined();
    });
});

describe('отчет:', () => {
    beforeEach(() => {
        isCardPage = false;
    });

    it('status === ERROR: вернет правильный результат, если есть оффер', () => {
        const item = { status: 'ERROR' };
        const data = preparer({ item, isCardPage, result });

        expect(data).toMatchSnapshot();
    });

    it('status === ERROR: ничего не вернет, если нет оффера', () => {
        const item = { status: 'ERROR' };
        const data = preparer({ item, isCardPage });

        expect(data).toBeUndefined();
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

    it('status === undefined: ничего не вернет', () => {
        const item = {};
        const data = preparer({ item, isCardPage });

        expect(data).toBeUndefined();
    });
});

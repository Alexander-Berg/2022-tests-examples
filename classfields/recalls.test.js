const preparer = require('./recalls');

let isCardPage;

describe('recalls preparer карточка:', () => {
    beforeEach(() => {
        isCardPage = true;
    });

    it('status === "ERROR": вернет пустой объект', () => {
        const item = { status: 'ERROR' };
        const data = preparer({ item, isCardPage });

        expect(data).toEqual({});
    });

    it('status === OK: вернет правильный объект "нет данных"', () => {
        const item = { status: 'OK', record_count: 0 };
        const data = preparer({ item, isCardPage });

        expect(data).toMatchSnapshot();
    });

    it('status === OK: вернет правильный объект "2 записи"', () => {
        const item = { status: 'OK', record_count: 2 };
        const data = preparer({ item, isCardPage });

        expect(data).toMatchSnapshot();
    });
});

describe('recalls preparer отчет:', () => {
    beforeEach(() => {
        isCardPage = false;
    });

    it('status === "ERROR": вернет пустой объект', () => {
        const item = { status: 'ERROR' };
        const data = preparer({ item, isCardPage });

        expect(data).toEqual({});
    });

    it('status === OK: вернет правильный объект "нет данных"', () => {
        const item = { status: 'OK', record_count: 0 };
        const data = preparer({ item, isCardPage });

        expect(data).toMatchSnapshot();
    });

    it('status === OK: вернет правильный объект "2 записи"', () => {
        const item = { status: 'OK', record_count: 2 };
        const data = preparer({ item, isCardPage });

        expect(data).toMatchSnapshot();
    });
});

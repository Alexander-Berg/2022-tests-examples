const preparer = require('./cheapeningGraph');

let isCardPage;

describe('карточка:', () => {
    beforeEach(() => {
        isCardPage = true;
    });

    it('status === ERROR: ничего не вернет', () => {
        const item = { status: 'ERROR' };
        const data = preparer({ item, isCardPage });

        expect(data).toBeUndefined();
    });

    it('status === undefined: вернет правильный результат', () => {
        const item = { };
        const data = preparer({ item, isCardPage });

        expect(data).toMatchSnapshot();
    });

    it('status === OK: если есть значение, вернет правильный результат', () => {
        const item = { status: 'OK' };
        const result = { report: { cheapening_graph: { cheapening_graph_data: { avg_annual_discount_percent: 20 } } } };
        const data = preparer({ item, isCardPage, result });

        expect(data).toMatchSnapshot();
    });

    it('status === OK: если нет значения, ничего не вернет', () => {
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

    it('status === undefined: вернет правильный результат', () => {
        const item = {};
        const data = preparer({ item, isCardPage });

        expect(data).toMatchSnapshot();
    });

    it('status === OK: если есть значение, вернет правильный результат', () => {
        const item = { status: 'OK' };
        const result = { report: { cheapening_graph: { cheapening_graph_data: { avg_annual_discount_percent: 20 } } } };
        const data = preparer({ item, isCardPage, result });

        expect(data).toMatchSnapshot();
    });

    it('status === OK: если нет значения, вернет флаг для удаления данных', () => {
        const item = { status: 'OK' };
        const data = preparer({ item, isCardPage });

        expect(data).toMatchSnapshot();
    });
});

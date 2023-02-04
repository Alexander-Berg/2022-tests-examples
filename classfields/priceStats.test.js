const preparer = require('./priceStats');

let isCardPage;

describe('карточка:', () => {
    isCardPage = true;

    it('ничего не вернет', () => {
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
        const result = { report: { price_stats_graph: { price_stats_data: { predicted_price: 20000 } } } };
        const data = preparer({ item, isCardPage, result });

        expect(data).toMatchSnapshot();
    });

    it('status === OK: если нет значения, вернет флаг для удаления данных', () => {
        const item = { status: 'OK' };
        const data = preparer({ item, isCardPage });

        expect(data).toMatchSnapshot();
    });
});

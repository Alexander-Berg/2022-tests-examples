import getCostValue from './getCostValue';

const TEST_CASES = [
    {
        cost: undefined,
        result: null,
        title: 'undefined',
    },

    {
        cost: 10500,
        result: '10 500 ₽',
        title: 'одно число',
    },

    {
        cost: { from: 0, to: 35000 },
        result: '0 — 35 000 ₽',
        title: 'диапазон',
    },

    {
        cost: { from: 40000 },
        result: 'от 40 000 ₽',
        title: 'диапазон без верхней границы',
    },

    {
        cost: { from: 8000000 },
        result: 'от 8 млн. ₽',
        title: 'диапазон с одной нижней границей больше миллиона',
    },
];

TEST_CASES.forEach(({ cost, result, title }) => {
    it(`getCostValue: если передать ${ title }`, () => {
        expect(getCostValue(cost)).toBe(result);
    });
});

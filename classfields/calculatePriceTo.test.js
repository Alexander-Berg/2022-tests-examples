const calculatePriceTo = require('./calculatePriceTo');

const testCases = [
    {
        price_from: 825_000,
        price_to: 1_072_500,
    },
    {
        price_from: 900_000,
        price_to: 1_170_000,
    },
    {
        price_from: 2_123_111,
        price_to: 2_760_000,
    },
    {
        price_from: 3_125_000,
        price_to: 4_062_000,
    },
    {
        price_from: 10_500_000,
        price_to: 13_650_000,
    },
];

testCases.forEach(testCase => {
    it(`Рассчитал правильный диапазон цен для чпу ${ testCase.price_from } -> ${ testCase.price_to }`, () => {
        expect(calculatePriceTo(testCase.price_from, testCase.price_to)).toMatchSnapshot();
    });
});

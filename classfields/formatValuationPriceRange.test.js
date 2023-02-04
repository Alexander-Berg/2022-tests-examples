const formatValuationPriceRange = require('./formatValuationPriceRange');

const TESTS = [
    { args: [ 330123, 420456 ], result: 'от 330 до 420 тыс ₽' },
    { args: [ 330123, 1420456 ], result: 'от 330 тыс до 1.4 млн ₽' },
    { args: [ 1330123, 1420456 ], result: 'от 1.3 до 1.4 млн ₽' },
];

TESTS.forEach((testCase) => {
    it(`should format ${ JSON.stringify(testCase.args) } as "${ testCase.result }"`, () => {
        expect(formatValuationPriceRange.apply(null, testCase.args)).toEqual(testCase.result);
    });
});

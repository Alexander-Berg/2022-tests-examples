const formatSegment = require('./formatSegment');

const TEST_CASES = [
    { input: '0-300000', output: 'До 300 тыс.' },
    { input: '300000-500000', output: '300 — 500 тыс.' },
    { input: '800000-1500000', output: '0,8 — 1,5 млн' },
    { input: '2000000-4000000', output: '2 — 4 млн' },
    { input: '1500000', output: 'Свыше 1,5 млн' },
];

TEST_CASES.forEach((testCase) => {
    it(`должен правильно форматировать сегмент "${ testCase.input }" как "${ testCase.output }"`, () => {
        expect(formatSegment(testCase.input)).toEqual(testCase.output);
    });
});

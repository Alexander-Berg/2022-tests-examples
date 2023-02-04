const MockDate = require('mockdate');

const getRelativeDateText = require('./getRelativeDateText');

beforeEach(() => {
    MockDate.set('2018-01-05');
});

afterEach(() => {
    MockDate.reset();
});

const TEST_CASES = [
    { input: '2018-01-04', output: 'вчера', description: 'должен возвращать "вчера", если переданная дата была вчера' },
    { input: '2018-01-05', output: 'сегодня', description: 'должен возвращать "сегодня", если переданная дата - сегодня' },
    { input: '2018-01-07', output: '', description: 'должен возвращать пустую строку, если переданная дата не подходит под условия' },
];

TEST_CASES.forEach((testCase) => {
    it(testCase.description, () => {
        const result = getRelativeDateText(testCase.input);

        expect(result).toBe(testCase.output);
    });
});

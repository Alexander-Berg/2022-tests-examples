const MockDate = require('mockdate');

const getDateText = require('./getDateText');
const tzOffset = `+0${ -Math.floor(new Date().getTimezoneOffset() / 60) }:00`;

beforeEach(() => {
    MockDate.set('2018-01-05');
});

afterEach(() => {
    MockDate.reset();
});

const TEST_CASES = [
    { input: `2018-01-04T09:00:00${ tzOffset }`, output: 'вчера', description: 'должен возвращать "вчера", если переданная дата была вчера' },
    { input: `2018-01-05T09:00:00${ tzOffset }`, output: 'сегодня', description: 'должен возвращать "сегодня", если переданная дата - сегодня' },
    // eslint-disable-next-line max-len
    { input: `2018-01-03T09:00:00${ tzOffset }`, output: '3 января', description: 'должен возвращать дату без года, если дата не вчера и не сегодня, но в этом году' },
    { input: `2017-01-03T09:00:00${ tzOffset }`, output: '3 января 2017', description: 'должен возвращать дату с годом, если дата не в этом году' },
];

describe('без времени', () => {
    TEST_CASES.forEach((testCase) => {
        it(testCase.description, () => {
            const result = getDateText(testCase.input);

            expect(result).toBe(testCase.output);
        });
    });
});

describe('с временем', () => {
    TEST_CASES.forEach((testCase) => {
        it(testCase.description, () => {
            const result = getDateText(testCase.input, { withTime: true });

            expect(result).toBe(testCase.output + ', 09:00');
        });
    });

    it('должен вставить правильный разделитель', () => {
        const result = getDateText(
            `2018-01-04T09:00:00${ tzOffset }`,
            { withTime: true, separator: ' в ' },
        );

        expect(result).toBe('вчера в 09:00');
    });
});

it('работает c noRelativeDates', () => {
    const result = getDateText(
        `2018-01-05T09:00:00${ tzOffset }`,
        { noRelativeDates: true },
    );

    expect(result).toBe('5 января 2018');
});

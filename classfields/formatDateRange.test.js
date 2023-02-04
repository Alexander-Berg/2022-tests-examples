const formatDateRange = require('./formatDateRange');

const TEST_CASES = [
    {
        description: 'должен правильно форматировать дни одного месяца',
        input: { from: '2019-01-21', to: '2019-01-22' },
        output: '21 — 22 января',
    },
    {
        description: 'должен правильно форматировать одиночную дату От',
        input: { from: '2019-01-21' },
        output: '21 января',
    },
    {
        description: 'должен правильно форматировать одиночную дату До',
        input: { to: '2019-01-22' },
        output: '22 января',
    },
    {
        description: 'должен правильно форматировать одинаковые даты',
        input: { from: '2019-01-22', to: '2019-01-22' },
        output: '22 января',
    },
    {
        description: 'должен правильно форматировать даты в разных месяцах',
        input: { from: '2019-01-22', to: '2019-02-22' },
        output: '22 января — 22 февраля',
    },
    {
        description: 'должен правильно форматировать даты в разных годах',
        input: { from: '2019-01-22', to: '2020-02-22' },
        output: '22 января 2019 — 22 февраля 2020',
    },
    {
        description: 'должен правильно форматировать даты в типе Date',
        input: { from: new Date('2019-01-22T10:46:36.141Z'), to: new Date('2019-02-22T15:46:36.141Z') },
        output: '22 января — 22 февраля',
    },
    {
        description: 'должен вернуть пустую строку, если ничего не передано',
        input: {},
        output: '',
    },
];

TEST_CASES.forEach((testCase) => {
    const { description, input, output } = testCase;

    it(description, () => {
        expect(formatDateRange(input)).toEqual(output);
    });
});

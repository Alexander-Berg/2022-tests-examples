const MockDate = require('mockdate');

const getFormattedDate = require('./getFormattedDate');

const TEST_CASES = [
    {
        description: 'Должен отдать дату, если на входе время в виде строки',
        date: '1448497000000',
        result: '26 ноября 2015',
    },
    {
        description: 'Должен отдать дату, если на входе время в виде числа',
        date: 1448497000000,
        result: '26 ноября 2015',
    },
    {
        description: 'Должен отдать нормальное сообщение, если передана дата',
        date: '2015-11-26',
        result: '26 ноября 2015',
    },
    {
        description: 'Должен отдать нормальное сообщение, если передана дата в формате ISO',
        date: '2015-11-26T13:13:13.000+0300',
        result: '26 ноября 2015',
    },
    {
        description: 'Должен отдать нормальное сообщение, если на входе некоректное время',
        date: 'ABC1448485200000',
        result: 'Неизвестно',
    },
    {
        description: 'Должен отдать нормальное сообщение, если время не передано',
        date: undefined,
        result: 'Неизвестно',
    },
    {
        description: 'Корректно для часового пояса Лондона',
        date: 1587243600000,
        timeZone: 'Europe/London',
        result: '18 апреля 2020',
    },
];

beforeEach(() => {
    MockDate.set('2019-02-26T13:13:13.000+0300');
});

afterEach(() => {
    MockDate.reset();
});

TEST_CASES.forEach(testCase => {
    it(testCase.description, () => {
        expect(getFormattedDate(testCase.date, testCase.format, testCase.timeZone)).toEqual(testCase.result);
    });
});

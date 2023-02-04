const statsCallsDailyListMock = require('../mocks/withStatsCallsDailyList.mock');

const calculateCallsChartData = require('./calculateCallsChartData');

const expectedResult = {
    total: [
        { t: '2019-10-11', y: 1 },
        { t: '2019-10-12', y: 2 },
        { t: '2019-10-13', y: 0 },
        { t: '2019-10-14', y: 13 },
    ],
    missing: [
        { t: '2019-10-11', y: 0 },
        { t: '2019-10-12', y: 2 },
        { t: '2019-10-13', y: 0 },
        { t: '2019-10-14', y: 5 },
    ],
    received: [
        { t: '2019-10-11', y: 1 },
        { t: '2019-10-12', y: 0 },
        { t: '2019-10-13', y: 0 },
        { t: '2019-10-14', y: 8 },
    ],
};

it('должен правильно замапить статистику звонков', () => {
    const result = calculateCallsChartData(
        statsCallsDailyListMock,
        { from: '2019-10-11', to: '2019-10-14' },
    );

    expect(result).toEqual(expectedResult);
});

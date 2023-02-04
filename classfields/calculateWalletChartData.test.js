const activationsDailyStatsMock = require('../mocks/withActivationsDailyStats.mock');

const calculateWalletChartData = require('./calculateWalletChartData');

it(`должен отдать количество элементов, равное разнице между переданными датами`, () => {
    const result = calculateWalletChartData(
        activationsDailyStatsMock.daily_stats,
        {
            from: '2020-01-26',
            to: '2020-01-28',
        },
    );

    expect(result).toHaveLength(3);
});

it(`должен достать элементы с переданными датами, если они есть, или проставлять ноль, если элемента с нужной датой нет в списке`, () => {
    const result = calculateWalletChartData(
        activationsDailyStatsMock.daily_stats,
        {
            from: '2020-01-26',
            to: '2020-01-28',
        },
    );

    expect(result[0].y).toBe(1765);
    expect(result[1].y).toBe(27820);
    expect(result[2].y).toBe(0);

});

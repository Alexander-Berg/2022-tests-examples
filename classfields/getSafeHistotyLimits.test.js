const getSafeHistoryLimits = require('./getSafeHistoryLimits');

it('должен возвращать измененные даты, если период превышен', () => {
    const dates = {
        // 93 дня
        from: '2018-02-20',
        to: '2018-05-24',
    };

    const expected = {
        // 92 дня
        from: '2018-02-21',
        to: '2018-05-24',
    };

    expect(getSafeHistoryLimits(dates)).toEqual(expected);
});

it('должен возвращать те же самые даты, если период не первышен', () => {
    const dates = {
        from: '2018-02-20',
        to: '2018-03-23',
    };

    expect(getSafeHistoryLimits(dates)).toEqual(dates);
});

const isExceededHistoryLimit = require('./isExceededHistoryLimit');

it('должен возвращать true, если период превышен', () => {
    const dates = {
        from: '2018-02-20',
        to: '2018-05-24', // 93 дня
    };

    expect(isExceededHistoryLimit(dates)).toBe(true);
});

it('должен возвращать false, если период не превышен', () => {
    const dates = {
        from: '2018-02-20',
        to: '2018-03-23',
    };

    expect(isExceededHistoryLimit(dates)).toBe(false);
});

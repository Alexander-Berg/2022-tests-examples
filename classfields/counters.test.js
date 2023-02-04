const counters = require('./counters');

it('должен вернуть данные за все время и сегодня, если есть данные', () => {
    expect(counters({
        counters: { all: 20, daily: 10 },
    })).toEqual('20 (10 сегодня)');
});

it('должен вернуть данные за все время, если нет данных за сегодня', () => {
    expect(counters({
        counters: { all: 20, daily: 0 },
    })).toEqual('20');
});

it('должен вернуть пустую строку, если нет данных', () => {
    expect(counters({
        counters: { all: 0 },
    })).toEqual('');
});

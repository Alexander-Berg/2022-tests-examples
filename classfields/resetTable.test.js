const resetTable = require('./resetTable');

it('должен вернуть корректный объект', () => {
    const data = {
        items: 'items',
        paging: 'paging',
        dateRange: 'dateRange',
    };

    expect(resetTable(data)).toEqual({
        type: 'TRADE_IN_RESET_TABLE',
        payload: data,
    });
});

const addItems = require('./addItems');

it('должен вернуть корретные данные', () => {
    expect(addItems([ 1, 2, 3 ])).toEqual({
        type: 'TRADE_IN_ADD_ITEMS',
        payload: [ 1, 2, 3 ],
    });
});

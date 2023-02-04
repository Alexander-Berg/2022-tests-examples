const updateItemAutopay = require('./updateItemAutopay');
it('должен вернуть корректный объект', () => {
    expect(updateItemAutopay({ id: 'clientId', autopay: { minValue: 20 } })).toEqual({
        type: 'UPDATE_CLIENTS_ITEM_AUTOPAY',
        payload: { id: 'clientId', autopay: { minValue: 20 } },
    });
});

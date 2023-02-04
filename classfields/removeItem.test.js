const removeItem = require('./removeItem');
it('должен вернуть корректный объект', () => {
    expect(removeItem('clientId')).toEqual({
        type: 'REMOVE_CLIENTS_ITEM',
        payload: 'clientId',
    });
});

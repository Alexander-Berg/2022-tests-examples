jest.mock('auto-core/react/lib/gateApi');
const resetDelivery = require('./resetDelivery');

it('должен вызвать корректный набор actions', () => {
    const dispatch = jest.fn();

    resetDelivery('1091189848')(dispatch);
    expect(dispatch.mock.calls).toEqual([
        [ {
            type: 'UPDATE_DELIVERY',
            payload: {
                saleId: '1091189848',
                values: [],
            },
        } ],
    ]);
});

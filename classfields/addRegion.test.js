const addRegion = require('./addRegion');

it('должен вызвать dispatch с корректными параметрами', () => {
    const dispatch = jest.fn();
    addRegion('region')(dispatch);

    expect(dispatch.mock.calls).toEqual([
        [ { type: 'ADD_DELIVERY_SETTINGS_REGION', payload: 'region' } ],
    ]);
});

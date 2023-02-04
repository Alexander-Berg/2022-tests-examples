const resetRegions = require('./resetRegions');

it('должен вызвать dispatch с корректными параметрами', () => {
    const dispatch = jest.fn();
    resetRegions('regions')(dispatch);

    expect(dispatch.mock.calls).toEqual([
        [ { type: 'RESET_DELIVERY_SETTINGS_REGIONS', payload: 'regions' } ],
    ]);
});

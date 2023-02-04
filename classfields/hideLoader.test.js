jest.useFakeTimers();
const hideLoader = require('./hideLoader');

it('hideLoader: должен вызвать dispatch с корректными параметрами', () => {
    const dispatch = jest.fn();
    hideLoader()(dispatch);

    expect(dispatch.mock.calls).toEqual([
        [ { type: 'HIDE_DELIVERY_SETTINGS_LOADER' } ],
    ]);
});

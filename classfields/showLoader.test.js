jest.useFakeTimers();
const showLoader = require('./showLoader');

it('showLoader: должен вызвать dispatch с корректными параметрами', () => {
    const dispatch = jest.fn();
    showLoader()(dispatch);

    expect(dispatch.mock.calls).toEqual([
        [ { type: 'SHOW_DELIVERY_SETTINGS_LOADER' } ],
    ]);
});

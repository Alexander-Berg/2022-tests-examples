const hideSuggestError = require('./hideSuggestError');

it('должен вызвать dispatch с корректными параметрами', () => {
    const dispatch = jest.fn();
    hideSuggestError()(dispatch);

    expect(dispatch.mock.calls).toEqual([
        [ { type: 'HIDE_DELIVERY_SETTINGS_SUGGEST_ERROR' } ],
    ]);
});

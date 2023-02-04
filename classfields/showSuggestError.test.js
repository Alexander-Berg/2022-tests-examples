const showSuggestError = require('./showSuggestError');

it('должен вызвать dispatch с корректными параметрами', () => {
    const dispatch = jest.fn();
    showSuggestError('текст ошибки')(dispatch);

    expect(dispatch.mock.calls).toEqual([
        [ { type: 'SHOW_DELIVERY_SETTINGS_SUGGEST_ERROR', payload: 'текст ошибки' } ],
    ]);
});

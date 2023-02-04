jest.useFakeTimers();
jest.mock('./resetRegions', () => () => ({ type: 'RESET_DELIVERY_SETTINGS_REGIONS' }));
jest.mock('./hideSuggestError', () => () => ({ type: 'HIDE_DELIVERY_SETTINGS_SUGGEST_ERROR' }));

const hide = require('./hide');

it('должен вызвать dispatch с корректными параметрами', () => {
    const dispatch = jest.fn();
    hide()(dispatch);
    jest.runAllTimers();

    expect(dispatch.mock.calls).toEqual([
        [ { type: 'HIDE_DELIVERY_SETTINGS' } ],
        [ { type: 'HIDE_DELIVERY_SETTINGS_SUGGEST_ERROR' } ],
        [ { type: 'RESET_DELIVERY_SETTINGS_REGIONS' } ],
    ]);
});

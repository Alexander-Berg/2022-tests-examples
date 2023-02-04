const reducer = require('./reducer');
const { PAGE_LOADING_SUCCESS } = require('auto-core/react/actionTypes');

it('on PAGE_LOADING_SUCCESS: должен добавить данные в стор', () => {
    const payloadMock = { foo: 'bar' };
    const state = {};

    const action = {
        type: PAGE_LOADING_SUCCESS,
        payload: { vasMotivator: payloadMock },
    };

    expect(reducer(state, action)).toEqual(payloadMock);
});

const reducer = require('./reducer');

const { PAGE_LOADING_SUCCESS } = require('auto-core/react/actionTypes');

const promoFeatures = [ { count: 1 } ];

const action = {
    type: PAGE_LOADING_SUCCESS,
    payload: {
        promoFeatures: promoFeatures,
    },
};

it(`должен установить корректный стейт, если action.PAGE_LOADING_SUCCESS`, () => {
    const state = {};

    const newState = reducer(state, action);
    expect(newState).toEqual({ features: promoFeatures });
});

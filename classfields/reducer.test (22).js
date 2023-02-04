const reducer = require('./reducer');

const ROUTES = require('auto-core/router/cabinet.auto.ru/route-names');
const { PAGE_LOADING_SUCCESS } = require('auto-core/react/actionTypes');
const actionPayload = {
    offerDailyStats: [ 1, 2, 3 ],
    userOffersCounters: {
        status: {
            inactive: { count: 20 },
        },
    },
};

const action = {
    type: PAGE_LOADING_SUCCESS,
    payload: {
        [ROUTES.index]: actionPayload,
    },
};

it(`должен установить корректный стейт, если action.PAGE_LOADING_SUCCESS`, () => {
    const state = {};

    const newState = reducer(state, action);
    expect(newState).toEqual(actionPayload);
});

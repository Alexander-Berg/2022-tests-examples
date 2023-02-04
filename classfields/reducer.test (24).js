const reducer = require('./reducer');

const ROUTES = require('auto-core/router/cabinet.auto.ru/route-names');
const { PAGE_LOADING_SUCCESS } = require('auto-core/react/actionTypes');

const FEEDS_ROUTES = [ ROUTES.feeds, ROUTES.feedsHistory, ROUTES.feedsHistoryItem ];

FEEDS_ROUTES.forEach((route) => {
    it(`должен обновлять параметры страницы при диспатче экшена PAGE_LOADING_SUCCESS с роутом "${ route }"`, () => {
        const state = {
            settings: {},
        };

        const expected = {
            settings: { foo: 123 },
            details: { bar: 123 },
        };

        const action = {
            type: PAGE_LOADING_SUCCESS,
            payload: { [route]: expected },
        };

        const newState = reducer(state, action);
        expect(newState).toEqual(expected);
    });
});

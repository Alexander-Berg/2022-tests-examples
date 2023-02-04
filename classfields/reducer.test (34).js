const ROUTES = require('auto-core/router/cabinet.auto.ru/route-names');

const reducer = require('./reducer');

const { PAGE_LOADING_SUCCESS } = require('auto-core/react/actionTypes');
const actionTypes = require('./actionTypes');

it('должен мерджить правильный payload в стейт при диспатче экшена PAGE_LOADING_SUCCESS', () => {
    const state = {
        foo: 'foo',
    };

    const action = {
        type: PAGE_LOADING_SUCCESS,
        payload: {
            [ROUTES.walkIn]: {
                bar: 'bar',
            },
        },
    };

    const expected = {
        foo: 'foo',
        bar: 'bar',
    };

    const newState = reducer(state, action);
    expect(newState).toEqual(expected);
});

it('должен обновлять эвенты', () => {
    const state = {
        aggregation: {},
        eventsList: {
            events: [ 1, 2, 3 ],
            paging: { foo: 123 },
        },
    };

    const payload = {
        events: [ 2, 3, 4, 5 ],
        paging: { bar: 123 },
    };

    const action = {
        type: actionTypes.UPDATE_EVENTS,
        payload: payload,
    };

    const newState = reducer(state, action);
    expect(newState).toEqual({
        eventsList: payload,
        aggregation: {},
    });
});

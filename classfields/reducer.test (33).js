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
            [ROUTES.users]: {
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

it('должен обновлять юзеров при диспатче экшена UPDATE_USERS', () => {
    const state = {
        users: [ 1, 2, 3 ],
    };

    const action = {
        type: actionTypes.UPDATE_USERS,
        payload: [ 4, 5, 6 ],
    };

    const expectedState = {
        users: [ 4, 5, 6 ],
    };
    const newState = reducer(state, action);
    expect(newState).toEqual(expectedState);
});

it('должен обновлять роли при диспатче экшена UPDATE_ROLES', () => {
    const state = {
        roles: [ 1, 2, 3 ],
    };

    const action = {
        type: actionTypes.UPDATE_ROLES,
        payload: [ 4, 5, 6 ],
    };

    const expectedState = {
        roles: [ 4, 5, 6 ],
    };

    const newState = reducer(state, action);
    expect(newState).toEqual(expectedState);
});

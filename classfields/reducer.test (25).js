const reducer = require('./reducer');

const { PAGE_LOADING_SUCCESS } = require('auto-core/react/actionTypes');

it('должен мерджить правильный payload в стейт при диспатче экшена PAGE_LOADING_SUCCESS', () => {
    const state = {
        foo: 'foo',
    };

    const action = {
        type: PAGE_LOADING_SUCCESS,
        payload: {
            loyalty: {
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

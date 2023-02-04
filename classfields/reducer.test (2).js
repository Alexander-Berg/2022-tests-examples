const reducer = require('./reducer');

const { PAGE_LOADING_SUCCESS } = require('auto-core/react/actionTypes');

let payload;
let bunkerState;
let expected;

beforeEach(() => {
    bunkerState = {
        foo: 'foo',
    };

    payload = {
        bunker: {
            bar: 'bar',
        },
    };

    expected = {
        foo: 'foo',
        bar: 'bar',
    };
});

it('должен мерджить ноды бункера в стейт при диспатче экшена PAGE_LOADING_SUCCESS', () => {
    const action = {
        type: PAGE_LOADING_SUCCESS,
        payload: payload,
    };

    const newState = reducer(bunkerState, action);
    expect(newState).toEqual(expected);
});

it('должен мерджить ноды бункера в стейт при диспатче deprecated экшена PAGE_LOADING_SUCCESS с data вместо payload', () => {
    const action = {
        type: PAGE_LOADING_SUCCESS,
        data: payload,
    };

    const newState = reducer(bunkerState, action);
    expect(newState).toEqual(expected);
});

it('не должен обновлять стейт, если нет данных для бункера', () => {
    const action = {
        type: PAGE_LOADING_SUCCESS,
        data: { baz: { wrongKey: 'wrongKey' } },
    };

    const newState = reducer(bunkerState, action);
    expect(newState).toEqual(bunkerState);
});

const { PAGE_LOADING_SUCCESS } = require('../../actionTypes');

const reducer = require('./reducer');

let state;
beforeEach(() => {
    state = reducer(undefined, {});
});

describe('PAGE_LOADING_SUCCESS', () => {
    it('не должен обработать, если нет payload.config', () => {
        const nextState = reducer(state, {
            type: PAGE_LOADING_SUCCESS,
            payload: {
                cookies: {},
            },
        });

        expect(nextState === state).toEqual(true);
    });

    it('не должен обработать, если нет payload.cookies', () => {
        const nextState = reducer(state, {
            type: PAGE_LOADING_SUCCESS,
            payload: {
                config: {},
            },
        });

        expect(nextState === state).toEqual(true);
    });
});

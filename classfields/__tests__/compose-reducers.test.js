import { composeReducers } from '../compose-reducers';

const reducerInc = (state, action) => {
    if (action.type === 'inc') {
        return state + 1;
    }
    return state;
};

const reducerDec = (state, action) => {
    if (action.type === 'dec') {
        return state - 1;
    }
    return state;
};

const reducerDouble = (state, action) => {
    if (action.type === 'double') {
        return state * 2;
    }
    return state;
};

describe('composeReducers', () => {
    it('should compose two reducers', () => {
        const reducer = composeReducers([ reducerInc, reducerDec ], 0);
        const state = reducer(0, { type: 'inc' });

        expect(state).toBe(1);
        const nextState = reducer(state, { type: 'dec' });

        expect(nextState).toBe(0);
    });

    it('should compose three reducers', () => {
        const reducer = composeReducers([ reducerInc, reducerDec, reducerDouble ], 0);
        const state = reducer(0, { type: 'inc' });

        expect(state).toBe(1);
        const doubledState = reducer(state, { type: 'double' });

        expect(doubledState).toBe(2);

        const decedState = reducer(doubledState, { type: 'dec' });

        expect(decedState).toBe(1);
    });
});

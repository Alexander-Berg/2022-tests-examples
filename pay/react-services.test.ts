import { createService } from './createService';
import { createStore } from './createStore';

interface State {
    counter: number;
}

describe('react-services', () => {
    it('should return new state', () => {
        const increaseCounter = createService<State, [number], State>(function increaseCounter(
            { getState, setState },
            data,
        ) {
            setState((draft) => {
                draft.counter = (draft.counter || 0) + data;
            });

            return getState();
        });

        const { dispatch } = createStore<State>();

        expect(dispatch(increaseCounter(1))).toStrictEqual({ counter: 1 });
    });

    it('should update state', () => {
        const increaseCounter = createService<State, [number]>(function increaseCounter({ setState }, data) {
            setState((draft) => {
                draft.counter = (draft.counter || 0) + data;
            });
        });

        const { dispatch, getState } = createStore<State>();
        dispatch(increaseCounter(1));

        expect(getState()).toStrictEqual({ counter: 1 });
    });
});

import type { RouteInfo, StateRouter } from './StateRouter';

import { cloneDeep } from 'lodash';

import reducer from './reducer';
import { PAGE_LOADING_SUCCESS } from './actionTypes';

import pageLoadingAction from './actions/pageLoading';
import pageLoadingFailedAction from './actions/pageLoadingFailed';
import pageLoadingSuccessAction from './actions/pageLoadingSuccess';
import pageReplaceState from './actions/pageReplaceState';

let indexRoute: RouteInfo;
let cardRoute: RouteInfo;
let state: StateRouter;
beforeEach(() => {
    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    state = reducer(undefined, { type: '@@INIT_EVENT' });
    indexRoute = {
        data: { components: [ 'A', 'B', 'C' ] },
        name: 'index',
        params: { foo: 'bar' },
        url: '/index',
    };
    cardRoute = {
        data: { components: [ 'A', 'B', 'C' ] },
        name: 'index',
        params: { foo: 'bar' },
        url: '/index',
    };
});

describe('PAGE_LOADING_SUCCESS', () => {
    it('не должен менять стейт, если в экшене нет router', () => {
        const expected = cloneDeep(state);
        const nextState = reducer(state, {
            type: PAGE_LOADING_SUCCESS,
            payload: {},
        });
        expect(nextState).toEqual(expected);
    });

    it('должен заменить стейт на новый router', () => {
        const nextState = reducer(state, pageLoadingSuccessAction(indexRoute));
        expect(nextState).toEqual({
            current: indexRoute,
            state: 'LOADED',
        });
    });

    it('должен убрать next, если пришел новый router', () => {
        state = {
            ...state,
            next: indexRoute,
        };
        const nextState = reducer(state, pageLoadingSuccessAction(indexRoute));
        expect(nextState).toEqual({
            current: indexRoute,
            state: 'LOADED',
        });
    });

    it('должен добавить prev, если current уже был', () => {
        state = {
            ...state,
            current: indexRoute,
        };
        const nextState = reducer(state, pageLoadingSuccessAction(cardRoute));
        expect(nextState).toEqual({
            current: cardRoute,
            previous: indexRoute,
            state: 'LOADED',
        });
    });
});

describe('PAGE_LOADING', () => {
    beforeEach(() => {
        state = reducer(state, pageLoadingSuccessAction(indexRoute));
    });

    it('должен добавить next и поменять state на LOADING', () => {
        const nextState = reducer(state, pageLoadingAction(cardRoute));

        expect(nextState).toEqual({
            current: indexRoute,
            next: cardRoute,
            state: 'LOADING',
        });
    });
});

describe('PAGE_LOADING_FAILED', () => {
    beforeEach(() => {
        state = reducer(state, pageLoadingSuccessAction(indexRoute));
        state = reducer(state, pageLoadingAction(cardRoute));
    });

    it('должен убрать next и поменять state на LOADED', () => {
        const nextState = reducer(state, pageLoadingFailedAction());

        expect(nextState).toEqual({
            current: indexRoute,
            state: 'LOADED',
        });
    });
});

describe('PAGE_REPLACE_STATE', () => {
    it('должен поменять current b не должен поменять previous', () => {
        state = {
            ...state,
            current: indexRoute,
            previous: cardRoute,
        };
        const nextState = reducer(state, pageReplaceState({
            ...indexRoute,
            params: {},
        }));
        expect(nextState).toEqual({
            current: {
                data: { components: [ 'A', 'B', 'C' ] },
                name: 'index',
                params: {},
                url: '/index',
            },
            previous: cardRoute,
            state: 'LOADED',
        });
    });
});

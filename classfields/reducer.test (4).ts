import type { EvaluationClearAction } from 'auto-core/react/dataDomain/evaluation/types';
import { EVALUATION_CLEAR } from 'auto-core/react/dataDomain/evaluation/types';
import { PAGE_LOADING_SUCCESS } from 'auto-core/react/actionTypes';

import {
    CLEAR_TECH_OPTIONS,
    LOAD_TECH_OPTIONS_PENDING,
    LOAD_TECH_OPTIONS_REJECTED,
    LOAD_TECH_OPTIONS_RESOLVED,
} from './types';
import type {
    ClearTechOptionsAction,
    LoadTechOptionsPendingAction,
    LoadTechOptionsRejectedAction,
    LoadTechOptionsResolvedAction,
    PageLoadingAction,
    StateCarsTechOptions,
} from './types';
import reducer from './reducer';

describe('PAGE_LOADING_SUCCESS', () => {
    it('должен взять данные из payload, если они там есть', () => {
        const action: PageLoadingAction = {
            type: PAGE_LOADING_SUCCESS,
            payload: { carsTechOptions: { gear_type: [] } },
        };
        const state = reducer(undefined, action);

        expect(state).toEqual({
            data: { gear_type: [] },
            isFetching: false,
        });
    });

    it('не должен взять данные из payload, если их там нет', () => {
        const action: PageLoadingAction = {
            type: PAGE_LOADING_SUCCESS,
            payload: {},
        };
        const state = reducer(undefined, action);

        expect(state).toEqual({
            data: {},
            isFetching: false,
        });
    });
});

describe('actions with state', () => {
    let state: StateCarsTechOptions;
    let loadTechOptionsPendingAction: LoadTechOptionsPendingAction;
    beforeEach(() => {
        const action: PageLoadingAction = {
            type: PAGE_LOADING_SUCCESS,
            payload: { carsTechOptions: { gear_type: [], year: [ 2021, 2020 ] } },
        };
        state = reducer(undefined, action);

        loadTechOptionsPendingAction = {
            type: LOAD_TECH_OPTIONS_PENDING,
            invalidateLevels: [ 'year' ],
        };
    });

    it('должен очистить state на EVALUATION_CLEAR', () => {
        const action: EvaluationClearAction = { type: EVALUATION_CLEAR };

        expect(reducer(state, action)).toEqual({
            data: {},
            isFetching: false,
        });
    });

    it('должен очистить state на CLEAR_TECH_OPTIONS', () => {
        const action: ClearTechOptionsAction = { type: CLEAR_TECH_OPTIONS };

        expect(reducer(state, action)).toEqual({
            data: {},
            isFetching: false,
        });
    });

    it('должен поменять флаг isFetching и очистить данные на LOAD_TECH_OPTIONS_PENDING', () => {
        expect(reducer(state, loadTechOptionsPendingAction)).toEqual({
            data: { gear_type: [] },
            isFetching: true,
        });
    });

    it('должен поменять флаг isFetching и запомнить данные на LOAD_TECH_OPTIONS_RESOLVED', () => {
        const action: LoadTechOptionsResolvedAction = {
            type: LOAD_TECH_OPTIONS_RESOLVED,
            payload: { body_type: [ 'SEDAN', 'WAGON_5_DOORS' ] },
        };
        state = reducer(state, loadTechOptionsPendingAction);

        expect(reducer(state, action)).toEqual({
            data: { body_type: [ 'SEDAN', 'WAGON_5_DOORS' ] },
            isFetching: false,
        });
    });

    it('должен поменять флаг isFetching и оставить данные на LOAD_TECH_OPTIONS_REJECTED', () => {
        const action: LoadTechOptionsRejectedAction = {
            type: LOAD_TECH_OPTIONS_REJECTED,
        };
        state = reducer(state, loadTechOptionsPendingAction);

        expect(reducer(state, action)).toEqual({
            data: { gear_type: [] },
            isFetching: false,
        });
    });
});

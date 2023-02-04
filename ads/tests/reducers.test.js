import update from 'immutability-helper';

import MainReducer, { AllUpdatesReducer, LastUpdatesReducer, ModelReducer } from '../src/reducers';

import { WindowsEnum } from '../src/actions/utils';

import * as actions from '../src/actions';
import * as fixtures from './fixtures';


describe('reducers default initialization', () => {

    test('MainReducer initialization', () => {
        expect(MainReducer(undefined, {})).toEqual(fixtures.INITIAL_STATE());
    });

    test('AllUpdatesReducer initialization', () => {
        expect(AllUpdatesReducer(undefined, {})).toEqual(fixtures.ALL_UPDATES_INITIAL_STATE());
    });

    test('LastUpdatesReducer initialization', () => {
        expect(LastUpdatesReducer(undefined, {})).toEqual(fixtures.LAST_UPDATES_INITIAL_STATE());
    });

    test('ModelReducer initialization', () => {
        expect(ModelReducer(undefined, {})).toEqual(fixtures.MODEL_INITIAL_STATE());
    });
});


describe('api getAllUpdates() reducers', () => {
    let state;

    beforeEach(() => {
        state = fixtures.INITIAL_STATE();
    });

    test('REQUEST_ALL_UPDATES should save status', () => {

        state.allUpdates.status = fixtures.STATUS_LOADING_STATE();

        const action = {
            type: actions.REQUEST_ALL_UPDATES,
            status: fixtures.STATUS_LOADING_STATE()
        };

        expect(MainReducer(undefined, action)).toEqual(state);
    });

    test('RECEIVE_ALL_UPDATES_SUCCESS should save status and models', () => {

        state.allUpdates.status = fixtures.STATUS_SUCCESS_STATE();
        state.allUpdates.models = fixtures.STATE_ALL_UPDATES_MODELS();

        const action = {
            type: actions.RECEIVE_ALL_UPDATES_SUCCESS,
            models: fixtures.API_ALL_UPDATES(),
            status: fixtures.STATUS_SUCCESS_STATE()
        };

        expect(MainReducer(undefined, action)).toEqual(state);
    });

    test('RECEIVE_ALL_UPDATES_FAILED should save status', () => {

        state.allUpdates.status = fixtures.STATUS_FAILED_4xx_STATE();

        const action = {
            type: actions.RECEIVE_ALL_UPDATES_FAILED,
            status: fixtures.STATUS_FAILED_4xx_STATE()
        };

        expect(MainReducer(undefined, action)).toEqual(state);
    });
});


describe('non-api reducers', () => {
    let state;

    beforeEach(() => {
        state = fixtures.INITIAL_STATE();
    });

    test('VIEW_ERROR_PANEL should set window', () => {

        state.window = WindowsEnum.ERROR;

        const action = {
            type: actions.VIEW_ERROR_PANEL
        };

        expect(MainReducer(undefined, action)).toEqual(state);
    });

    test('VIEW_LOADING_PANEL should set window', () => {

        state.window = WindowsEnum.LOADING;

        const action = {
            type: actions.VIEW_LOADING_PANEL
        };

        expect(MainReducer(undefined, action)).toEqual(state);
    });

    test('VIEW_UPDATES_PANEL should set window', () => {

        state.window = WindowsEnum.UPDATES_PANEL;

        const action = {
            type: actions.VIEW_UPDATES_PANEL
        };

        expect(MainReducer(undefined, action)).toEqual(state);
    });

    test('START_INITIALIZATION set initialization status', () => {
        state.initializationStatus = fixtures.STATUS_LOADING_STATE();

        const action = {
            type: actions.START_INITIALIZATION,
            initializationStatus: fixtures.STATUS_LOADING_STATE()
        };

        expect(MainReducer(undefined, action)).toEqual(state);
    });

    test('END_INITIALIZATION_SUCCESS set initialization status', () => {
        state.initializationStatus = fixtures.STATUS_SUCCESS_STATE();

        const action = {
            type: actions.END_INITIALIZATION_SUCCESS,
            initializationStatus: fixtures.STATUS_SUCCESS_STATE()
        };

        expect(MainReducer(undefined, action)).toEqual(state);
    });

    test('END_INITIALIZATION_FAILED set initialization status', () => {
        state.initializationStatus = fixtures.STATUS_FAILED_4xx_STATE();

        const action = {
            type: actions.END_INITIALIZATION_FAILED,
            initializationStatus: fixtures.STATUS_FAILED_4xx_STATE()
        };

        expect(MainReducer(undefined, action)).toEqual(state);
    });

    test('SELECT_ALL_UPDATES_DATE set date', () => {
        state.allUpdates.date ='2017-11-12';

        const action = {
            type: actions.SELECT_ALL_UPDATES_DATE,
            date: '2017-11-12'
        };

        expect(MainReducer(undefined, action)).toEqual(state);
    });

});

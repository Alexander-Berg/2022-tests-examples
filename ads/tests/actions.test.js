import configureMockStore from 'redux-mock-store'
import thunk from 'redux-thunk'

const middlewares = [ thunk ];
const mockStore = configureMockStore(middlewares);

import * as actions from '../src/actions';
import * as fixtures from './fixtures';
import ApiMock from './fixtures';


describe('non-api actions', () => {

    test('selectAllUpdatesDate() should dispatch set date', () => {
        const date = '2017-11-12';
        const expectedAction = {
            type: actions.SELECT_ALL_UPDATES_DATE,
            date: date
        };

        expect(actions.selectAllUpdatesDate(date)).toEqual(expectedAction)
    });

    test('startInitialization() should dispatch set status', () => {

        const expectedAction = {
            type: actions.START_INITIALIZATION,
            initializationStatus: fixtures.STATUS_LOADING_STATE()
        };

        expect(actions.startInitialization()).toEqual(expectedAction)
    });

    test('endInitializationSuccess() should dispatch set success status', () => {

        const expectedAction = {
            type: actions.END_INITIALIZATION_SUCCESS,
            initializationStatus: fixtures.STATUS_SUCCESS_STATE()
        };

        expect(actions.endInitializationSuccess()).toEqual(expectedAction)
    });

    test('endInitializationFailed() should dispatch set failed status', () => {

        const expectedAction = {
            type: actions.END_INITIALIZATION_FAILED,
            initializationStatus: fixtures.STATUS_FAILED_4xx_STATE()
        };

        expect(actions.endInitializationFailed(fixtures.API_4xx_ERROR())).toEqual(expectedAction)
    });
});


// TODO: Find appropriate method to aggregate repeated suite methods.
// Like checks about 200, 400, 500 which repeated from suite to suite with the same interfaсe


describe('api getAllUpdates() actions', () => {
    let api;
    let store;

    beforeEach(() => {
        api = new ApiMock();
        store = mockStore(fixtures.INITIAL_STATE());
    });

    test('GET/200 allUpdates should dispatch initialization', () => {

        api.setResolveGetAllUpdates(fixtures.API_ALL_UPDATES());

        const expectedActions = [
            { type: actions.REQUEST_ALL_UPDATES, status: fixtures.STATUS_LOADING_STATE() },
            { type: actions.RECEIVE_ALL_UPDATES_SUCCESS, models: fixtures.API_ALL_UPDATES(), status: fixtures.STATUS_SUCCESS_STATE() }
        ];

        return expect(store.dispatch(actions.fetchAllUpdates(api, "2017-10-10"))).resolves.toBeUndefined()
            .then(() => expect(store.getActions()).toEqual(expectedActions));
    });

    test('GET/400 allUpdates should dispatch status with concrete message and errors', () => {

        api.setRejectGetAllUpdates(fixtures.API_4xx_ERROR());

        const expectedActions = [
            { type: actions.REQUEST_ALL_UPDATES, status: fixtures.STATUS_LOADING_STATE() },
            { type: actions.RECEIVE_ALL_UPDATES_FAILED, status: fixtures.STATUS_FAILED_4xx_STATE() }
        ];

        return expect(store.dispatch(actions.fetchAllUpdates(api, "2017-10-10"))).rejects.toBeUndefined()
            .catch(() => expect(store.getActions()).toEqual(expectedActions));
    });

    test('GET/500 allUpdates should dispatch status with server fail message', () => {

        api.setRejectGetAllUpdates(fixtures.API_5xx_ERROR());

        const expectedActions = [
            { type: actions.REQUEST_ALL_UPDATES, status: fixtures.STATUS_LOADING_STATE() },
            { type: actions.RECEIVE_ALL_UPDATES_FAILED, status: fixtures.STATUS_FAILED_5xx_STATE() }
        ];

        return expect(store.dispatch(actions.fetchAllUpdates(api, "2017-10-10"))).rejects.toBeUndefined()
            .catch(() => expect(store.getActions()).toEqual(expectedActions));
    });
});


describe('initializeUpdatesPanel actions', () => {
    let api;
    let store;
    const date="2017-10-10";

    beforeEach(() => {
        api = new ApiMock();
        store = mockStore(fixtures.INITIAL_STATE());
    });


    test('GET/200 in all fetchers should dispatch view updates panel', () => {
        api.setResolveGetAllUpdates(fixtures.API_ALL_UPDATES());

        const importantActionTypes = [
            actions.REQUEST_ALL_UPDATES,
            actions.RECEIVE_ALL_UPDATES_SUCCESS
        ];

        return expect(store.dispatch(actions.initializeUpdatesPanel(api, date))).resolves.toBeUndefined()
            .then(() => expect(store.getActions().map(action => action.type))
                .toEqual(expect.arrayContaining(importantActionTypes)));
    });

    test('GET/4**/5** in getAllUpdates() should dispatch view error panel', () => {
        api.setRejectGetAllUpdates(fixtures.API_4xx_ERROR());

        const expectedActionTypes = [
            actions.REQUEST_ALL_UPDATES,
            actions.RECEIVE_ALL_UPDATES_FAILED
        ];

        const preventedActionTypes = [
            actions.RECEIVE_ALL_UPDATES_SUCCESS
        ];

        return expect(store.dispatch(actions.initializeUpdatesPanel(api, date))).resolves.toBeUndefined()
            .then(() => expect(store.getActions().map(action => action.type))
                .toEqual(expect.arrayContaining(expectedActionTypes)))
            .then(() => expect(store.getActions().map(action => action.type))
                .not.toEqual(expect.arrayContaining(preventedActionTypes)));
    });
});

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

const onShowMore = require('./onShowMore');
const gateApi = require('auto-core/react/lib/gateApi');

const actionTypes = require('../actionTypes');
const configActionTypes = require('www-cabinet/react/dataDomain/config/actionTypes');

const mockStore = require('autoru-frontend/mocks/mockStore').default;

const withCalls = require('www-cabinet/react/dataDomain/calls/mocks/withCalls.mock');

const CALLS_SORTING_FIELDS = require('www-cabinet/data/calls/call-sorting-fields.json');
const CALLS_SORTING_TYPES = require('www-cabinet/data/calls/call-sorting-types.json');

beforeEach(() => {
    gateApi.getResource.mockImplementation(() => Promise.resolve({
        calls: withCalls.callsList.calls,
        pagination: { page_num: 3 },
    }));
});

it('должен вызвать getResource с параметрами страницы', () => {
    const store = mockStore({
        calls: {
            callsList: {
                calls: withCalls.callsList.calls,
                pagination: {
                    page_num: 2,
                },
            },
        },
    });

    store.dispatch(
        onShowMore(),
    );

    expect(gateApi.getResource).toHaveBeenCalledWith('getCallsList', {
        dealer_id: undefined,
        filter: {},
        sorting: {
            sorting_field: CALLS_SORTING_FIELDS.CALL_TIME,
            sorting_type: CALLS_SORTING_TYPES.DESCENDING,
        },
        page: 3,
        page_size: 20,
    });
});

it('должен обновить список звонков', () => {
    expect.assertions(1);
    const store = mockStore({
        calls: {
            callsList: {
                calls: withCalls.callsList.calls,
                pagination: {
                    page_num: 2,
                },
            },
        },
    });

    return store.dispatch(
        onShowMore(),
    )
        .then(() => {
            const action = store.getActions().find(action => action.type === actionTypes.UPDATE_CALLS);

            const calls = action.payload.calls;

            expect(calls).toEqual([ ...withCalls.callsList.calls, ...withCalls.callsList.calls ]);
        });
});

it('должен переключать loading state', () => {
    expect.assertions(1);

    const store = mockStore({});

    return store.dispatch(
        onShowMore(),
    )
        .then(() => {
            const actions = store.getActions().filter(action => action.type === configActionTypes.TOGGLE_LOADING_STATE);

            expect(actions).toHaveLength(2);
        });
});

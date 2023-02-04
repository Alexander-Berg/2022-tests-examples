const reducer = require('./reducer');

const actionTypes = require('./actionTypes');

const { PAGE_LOADING_SUCCESS } = require('auto-core/react/actionTypes');

const TEST_CASES = [
    { action: actionTypes.PAGE_LOADING_START, expected: { isLoading: true } },
    { action: PAGE_LOADING_SUCCESS, expected: { isLoading: false } },
    { action: actionTypes.PAGE_LOADING_FAIL, expected: { isLoading: false } },
];

TEST_CASES.forEach((testCase) => {
    it(`должен обновлять состояние загрузки при экшене ${ testCase.action } `, () => {
        const state = { isLoading: false };
        const expectedPayload = testCase.expected;

        const action = {
            type: testCase.action,
            payload: expectedPayload,
        };

        const newState = reducer(state, action);
        expect(newState).toEqual(expectedPayload);
    });
});

it('должен обновлять информацию о роуте', () => {
    const state = {
        routeName: 'foo',
        routeParams: {},
    };

    const expectedPayload = {
        routeName: 'bar',
        routeParams: {
            params: 1,
        },
    };

    const action = {
        type: actionTypes.UPDATE_ROUTE_INFO,
        payload: expectedPayload,
    };

    const newState = reducer(state, action);
    expect(newState).toEqual(expectedPayload);
});

it('должен обновлять состояние загрузки', () => {
    const state = {
        routeName: 'foo',
        isLoading: true,
    };

    const action = {
        type: actionTypes.TOGGLE_LOADING_STATE,
    };

    const newState = reducer(state, action);

    expect(newState).toEqual({
        routeName: 'foo',
        isLoading: false,
    });
});

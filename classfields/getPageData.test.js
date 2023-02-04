/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getPage: jest.fn(),
    };
});

const mockStore = require('autoru-frontend/mocks/mockStore').default;
const getPageData = require('./getPageData');
const actionTypes = require('../actionTypes');

const { PAGE_LOADING_SUCCESS } = require('auto-core/react/actionTypes');

const getPage = require('auto-core/react/lib/gateApi').getPage;

let store;
let gateApiResponse;

beforeEach(() => {
    store = mockStore({
        config: {
            routeName: 'foo',
            routeParams: { params: 123 },
            isLoading: false,
        },
    });

    gateApiResponse = { test: 123 };
});

it('должен вызывать экшены успешной загрузки c обновленными параметрами роута', async() => {
    const expectedActions = [
        { type: actionTypes.PAGE_LOADING_START },
        { type: actionTypes.UPDATE_ROUTE_INFO, payload: { routeName: 'bar', routeParams: {} } },
        { type: PAGE_LOADING_SUCCESS, payload: gateApiResponse },
    ];

    getPage.mockImplementation(() => Promise.resolve(gateApiResponse));

    await store.dispatch(
        getPageData({ routeName: 'bar', params: {} }),
    );

    expect(store.getActions()).toEqual(expectedActions);
});

it('должен вызывать экшены неуспешной загрузки и делать роллбэк параметров роута', async() => {
    const expectedActions = [
        { type: actionTypes.PAGE_LOADING_START },
        { type: actionTypes.UPDATE_ROUTE_INFO, payload: { routeName: 'bar', routeParams: {} } },
        { type: actionTypes.PAGE_LOADING_FAIL },
        { type: actionTypes.UPDATE_ROUTE_INFO, payload: { routeName: 'foo', routeParams: { params: 123 } } },
        { type: 'NOTIFIER_SHOW_MESSAGE', payload: { message: 'Произошла ошибка, попробуйте ещё раз', view: 'error' } },
    ];

    getPage.mockImplementation(() => Promise.reject());

    try {
        await store.dispatch(
            getPageData({ routeName: 'bar', params: {} }),
        );
    } catch {}

    expect(store.getActions()).toEqual(expectedActions);
});

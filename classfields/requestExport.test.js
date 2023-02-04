/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

const requestExport = require('./requestExport');
const gateApi = require('auto-core/react/lib/gateApi');

const mockStore = require('autoru-frontend/mocks/mockStore').default;

const CALLS_SORTING_FIELDS = require('www-cabinet/data/calls/call-sorting-fields.json');
const CALLS_SORTING_TYPES = require('www-cabinet/data/calls/call-sorting-types.json');

let originalWindowLocation;

beforeEach(() => {
    gateApi.getResource.mockImplementation(() => Promise.resolve());

    originalWindowLocation = global.window.location;
    delete global.window.location;

    global.window.location = {};
});

afterEach(() => {
    global.window.location = originalWindowLocation;
});

it('должен вызвать ресурс экспорта', () => {
    const store = mockStore({});

    store.dispatch(
        requestExport(),
    );

    expect(gateApi.getResource).toHaveBeenCalledWith('requestExport', {
        dealer_id: undefined,
        filter: {},
        sorting: {
            sorting_field: CALLS_SORTING_FIELDS.CALL_TIME,
            sorting_type: CALLS_SORTING_TYPES.DESCENDING,
        },
    });
});

it('должен сеттить полученный url в window.location', () => {
    expect.assertions(1);

    const url = 'http://auto.ru';
    const promise = Promise.resolve({ download_url: url });

    const store = mockStore({});

    gateApi.getResource.mockImplementation(() => promise);

    store.dispatch(
        requestExport(),
    );

    return promise
        .then(() => {
            expect(global.window.location.href).toBe(url);
        });
});

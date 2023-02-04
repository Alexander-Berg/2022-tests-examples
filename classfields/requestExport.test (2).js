/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(() => Promise.resolve({})),
    };
});

const mockStore = require('autoru-frontend/mocks/mockStore').default;
const requestExport = require('./requestExport');

let store;

beforeEach(() => {
    store = mockStore({
        wallet: {
            dateLimits: { from: '2018-02-03', to: '2018-03-05' },
        },
    });
});

const getResource = require('auto-core/react/lib/gateApi').getResource;

it('должен вызвать ресурс "getDealerExpensesReport" с данными из стейта', () => {
    store.dispatch(
        requestExport({}),
    );

    expect(getResource).toHaveBeenCalledWith('getDealerExpensesReport', {
        from_date: '2018-02-03',
        to_date: '2018-03-05',
    });
});

it('должен вызвать ресурс "getDealerExpensesReport" с датами из аргументов, если переданы', () => {
    store.dispatch(
        requestExport({
            dateLimits: { from: '2017-02-03', to: '2017-03-05' },
        }),
    );

    expect(getResource).toHaveBeenCalledWith('getDealerExpensesReport', {
        from_date: '2017-02-03',
        to_date: '2017-03-05',
    });
});

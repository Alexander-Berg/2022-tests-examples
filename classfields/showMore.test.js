/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/gateApi', () => {
    return { getResource: jest.fn() };
});

const gateApi = require('auto-core/react/lib/gateApi');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const showMore = require('./showMore');

it('вызвать getCallsNumbers с параметрами и вернуть корректный набор экшнов', () => {
    const store = mockStore({
        config: {
            customerRole: 'client',
            routeParams: {
                redirect: '+71111111111',
            },
        },

    });
    gateApi.getResource.mockImplementation(() => Promise.resolve({ listing: [ 1, 2, 3 ] }));

    return store.dispatch(showMore(2))
        .then(() => {
            expect(gateApi.getResource).toHaveBeenCalledWith('getCallsNumbers', { redirect: '+71111111111', page: 2 });
            expect(store.getActions()).toEqual([
                { type: 'TOGGLE_LOADING_STATE' },
                { type: 'CALLS_NUMBERS_ADD_CALLS', payload: { listing: [ 1, 2, 3 ] } },
                { type: 'TOGGLE_LOADING_STATE' },
            ]);
        });
});

it('вернуть корректный набор экшнов в случае ошибки', () => {
    const store = mockStore({
        config: {
            customerRole: 'client',
        },
    });
    gateApi.getResource.mockImplementation(() => Promise.reject());

    return store.dispatch(showMore(2))
        .then(() => {
            expect(store.getActions()).toEqual([
                { type: 'TOGGLE_LOADING_STATE' },
                { type: 'TOGGLE_LOADING_STATE' },
                {
                    type: 'NOTIFIER_SHOW_MESSAGE',
                    payload: {
                        message: 'Произошла ошибка, попробуйте ещё раз',
                        view: 'error',
                    },
                },
            ]);
        });
});

/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/gateApi');
jest.mock('www-cabinet/react/lib/getMetrika');
jest.mock('www-cabinet/react/dataDomain/sales/actions/sendMetrikaWithCategoryAndSection');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const removeParamsMock = require('../mocks/removeParams');
const initialState = require('../mocks/initialState');
const gateApi = require('auto-core/react/lib/gateApi');
const sendMetrikaWithCategoryAndSection = require('www-cabinet/react/dataDomain/sales/actions/sendMetrikaWithCategoryAndSection');

const remove = require('./remove');

it('должен вызвать gateApi с removeAutostrategy, а потом вызвать набор экшнов', () => {
    initialState.autostrategy.isGroupOperation = false;
    const store = mockStore(initialState);
    gateApi.getResource = jest.fn(() => Promise.resolve({
        response: {
            status: 'SUCCESS',
        },
    }));

    return store.dispatch(remove(removeParamsMock)).then(() => {
        expect(gateApi.getResource).toHaveBeenCalledWith('removeAutostrategy', {
            ids: [ { offer_id: '1102149895-c214fd8e', autostrategy_type: 'ALWAYS_AT_FIRST_PAGE' } ],
            dealer_id: 16453,
        });

        expect(store.getActions()).toMatchSnapshot();
    });
});

it('должен вызывать sendMetrikaWithCategoryAndSection для групповых операций', () => {
    initialState.autostrategy.isGroupOperation = true;
    const store = mockStore(initialState);
    gateApi.getResource = jest.fn(() => Promise.resolve({
        response: {
            status: 'SUCCESS',
        },
    }));

    return store.dispatch(remove(removeParamsMock)).then(() => {
        expect(sendMetrikaWithCategoryAndSection).toHaveBeenCalledWith(
            {
                hash: 'c214fd8e',
                id: '1102149895',
                service_fresh: {
                    price: 25,
                },
            },
            [ 'autostrategy', 'off' ],
        );
    });
});

it('должен вызвать gateApi с removeAutostrategy, а потом вызвать набор экшнов, ' +
    'если что-то пошло не так', () => {
    initialState.autostrategy.isGroupOperation = false;
    const store = mockStore(initialState);
    gateApi.getResource = jest.fn(() => Promise.reject());

    return store.dispatch(remove(removeParamsMock)).then(() => {
        expect(gateApi.getResource).toHaveBeenCalledWith('removeAutostrategy', {
            ids: [ { offer_id: '1102149895-c214fd8e', autostrategy_type: 'ALWAYS_AT_FIRST_PAGE' } ],
            dealer_id: 16453,
        });

        expect(store.getActions()).toMatchSnapshot();
    });
});

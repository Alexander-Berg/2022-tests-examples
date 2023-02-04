/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/gateApi');
jest.mock('www-cabinet/react/lib/getMetrika');
jest.mock('www-cabinet/react/dataDomain/sales/actions/sendMetrikaWithCategoryAndSection');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const applyParamsMock = require('../mocks/applyParams');
const initialState = require('../mocks/initialState');
const gateApi = require('auto-core/react/lib/gateApi');
const getMetrika = require('www-cabinet/react/lib/getMetrika');
const sendMetrikaWithCategoryAndSection = require('www-cabinet/react/dataDomain/sales/actions/sendMetrikaWithCategoryAndSection');

const apply = require('./apply');

it('должен вызвать gateApi с applyAutostrategy, а потом вызвать набор экшнов', () => {
    const reachGoal = jest.fn();
    getMetrika.mockImplementation(() => ({ reachGoal }));
    initialState.autostrategy.isGroupOperation = false;
    const store = mockStore(initialState);
    gateApi.getResource = jest.fn(() => Promise.resolve({
        response: {
            status: 'SUCCESS',
        },
    }));

    return store.dispatch(apply(applyParamsMock)).then(() => {
        expect(gateApi.getResource).toHaveBeenCalledWith('applyAutostrategy', {
            autostrategies: [
                {
                    offer_id: '1102149895-c214fd8e',
                    from_date: '2021-01-14',
                    to_date: '2021-01-20',
                    always_at_first_page: {
                        for_mark_model_listing: true,
                        for_mark_model_generation_listing: true,
                    },
                    max_applications_per_day: 5,
                },
            ],
            dealer_id: 16453,
        });

        expect(sendMetrikaWithCategoryAndSection).toHaveBeenCalledWith({
            hash: 'c214fd8e',
            id: '1102149895',
            service_fresh: {
                price: 25,
            },
        }, [ 'autostrategy', 'change' ]);
        expect(reachGoal).toHaveBeenCalledWith('AUTOSTRATEGY_APPLIED');
        expect(store.getActions()).toMatchSnapshot();
    });
});

it('не должен вызывать sendMetrikaWithCategoryAndSection и metrika.reachGoal для групповых операций', () => {
    const reachGoal = jest.fn();
    getMetrika.mockImplementation(() => ({ reachGoal }));
    initialState.autostrategy.isGroupOperation = true;
    const store = mockStore(initialState);
    gateApi.getResource = jest.fn(() => Promise.resolve({
        response: {
            status: 'SUCCESS',
        },
    }));

    return store.dispatch(apply(applyParamsMock)).then(() => {
        expect(sendMetrikaWithCategoryAndSection).not.toHaveBeenCalled();
        expect(reachGoal).not.toHaveBeenCalled();
    });
});

it('должен вызвать gateApi с applyAutostrategy, а потом вызвать набор экшнов, если что-то пошло не так', () => {
    const reachGoal = jest.fn();
    getMetrika.mockImplementation(() => ({ reachGoal }));
    initialState.autostrategy.isGroupOperation = false;
    const store = mockStore(initialState);
    gateApi.getResource = jest.fn(() => Promise.reject());

    return store.dispatch(apply(applyParamsMock)).then(() => {
        expect(gateApi.getResource).toHaveBeenCalledWith('applyAutostrategy', {
            autostrategies: [
                {
                    offer_id: '1102149895-c214fd8e',
                    from_date: '2021-01-14',
                    to_date: '2021-01-20',
                    always_at_first_page: {
                        for_mark_model_listing: true,
                        for_mark_model_generation_listing: true,
                    },
                    max_applications_per_day: 5,
                },
            ],
            dealer_id: 16453,
        });

        expect(sendMetrikaWithCategoryAndSection).toHaveBeenCalledWith(
            {
                hash: 'c214fd8e',
                id: '1102149895',
                service_fresh: {
                    price: 25,
                },
            },
            [ 'autostrategy', 'error' ],
        );
        expect(reachGoal).toHaveBeenCalledWith('AUTOSTRATEGY_APPLY_ERROR');
        expect(store.getActions()).toMatchSnapshot();
    });
});

it('не должен вызывать sendMetrikaWithCategoryAndSection и metrika.reachGoal для групповых операций, ' +
    'если что-то пошло не так', () => {
    const reachGoal = jest.fn();
    getMetrika.mockImplementation(() => ({ reachGoal }));
    initialState.autostrategy.isGroupOperation = true;
    const store = mockStore(initialState);
    gateApi.getResource = jest.fn(() => Promise.reject());

    return store.dispatch(apply(applyParamsMock)).then(() => {
        expect(sendMetrikaWithCategoryAndSection).not.toHaveBeenCalled();
        expect(reachGoal).not.toHaveBeenCalled();
    });
});

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

jest.mock('www-cabinet/react/lib/getMetrika');

const gateApi = require('auto-core/react/lib/gateApi');
const getMetrika = require('www-cabinet/react/lib/getMetrika');

const changeSwitcherIsActive = require('./changeSwitcherIsActive');

const mockStore = require('autoru-frontend/mocks/mockStore').default;

gateApi.getResource.mockImplementation(() => Promise.resolve(42));

it('должен вызвать deleteDealerCampaignProduct, metrica.sendPageEvent и ' +
    'dispatch updateSwitcherById с корректными параметрами', () => {
    const sendParams = jest.fn();

    getMetrika.mockImplementation(() => ({
        params: sendParams,
    }));

    const store = mockStore({
        tradeIn: {
            usedCarsSwitcher: {
                id: 'trade-in-request:cars:used',
                isActive: true,
            },
        },
    });

    return store.dispatch(
        changeSwitcherIsActive('trade-in-request:cars:used'),
    )
        .then(() => {
            expect(gateApi.getResource)
                .toHaveBeenCalledWith('deleteDealerCampaignProduct', { product: 'trade-in-request:cars:used' });
            expect(sendParams)
                .toHaveBeenCalledWith('trade-in_cars_used_deactivate');
            expect(store.getActions()[0]).toEqual({
                type: 'TRADE_IN_UPDATE_SWITCHER_BY_ID',
                payload: {
                    id: 'usedCarsSwitcher',
                    params: { isActive: false },
                },
            });
        });
});

it('должен вызвать putDealerCampaignProduct, metrika.sendPageEvent и' +
    'dispatch updateSwitcherById с корректными параметрами', () => {
    const store = mockStore({
        tradeIn: {
            newCarsSwitcher: {
                id: 'trade-in-request:cars:new',
                isActive: false,
            },
        },
    });

    const sendParams = jest.fn();

    getMetrika.mockImplementation(() => ({
        params: sendParams,
    }));

    return store.dispatch(
        changeSwitcherIsActive('trade-in-request:cars:new'),
    )
        .then(() => {
            expect(gateApi.getResource)
                .toHaveBeenCalledWith('putDealerCampaignProduct', { product: 'trade-in-request:cars:new' });
            expect(sendParams)
                .toHaveBeenCalledWith('trade-in_cars_new_activate');
            expect(store.getActions()[0]).toEqual({
                type: 'TRADE_IN_UPDATE_SWITCHER_BY_ID',
                payload: {
                    id: 'newCarsSwitcher',
                    params: { isActive: true },
                },
            });
        });
});

jest.mock('www-cabinet/react/dataDomain/tradeIn/actions/updateUrl', () => jest.fn());
jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

jest.mock('www-cabinet/react/lib/getMetrika');

const gateApi = require('auto-core/react/lib/gateApi');
const getMetrika = require('www-cabinet/react/lib/getMetrika');

gateApi.getResource.mockImplementation(() => Promise.resolve(42));

const changeSection = require('./changeSection');

it('должен вызвать getDealerTradeIn, metrika.sendPageEvent, updateUrl ' +
    'и dispatch пачку actions с корректными параметрами', () => {
    const dispatch = jest.fn();
    const updateUrl = require('www-cabinet/react/dataDomain/tradeIn/actions/updateUrl');
    updateUrl.mockClear();

    const params = jest.fn();
    getMetrika.mockImplementation(() => ({
        params,
    }));

    const getState = jest.fn(() => ({
        config: {
            client: {
                id: 'dealer_id',
            },
        },
        tradeIn: {
            filters: {
                section: 'USED',
                dateRange: {
                    fromDate: 'fromDate',
                },
            },
        },
    }));

    const updatedParams = {
        from_date: 'fromDate',
        section: 'NEW',
        page: 1,
    };

    return changeSection('NEW')(dispatch, getState)
        .then(() => {
            expect(gateApi.getResource)
                .toHaveBeenCalledWith('getDealerTradeIn', { ...updatedParams, dealer_id: 'dealer_id' });
            expect(updateUrl).toHaveBeenCalledWith(updatedParams);
            expect(params)
                .toHaveBeenCalledWith('trade-in_select_new');
            expect(dispatch.mock.calls)
                .toEqual([
                    [ { type: 'TRADE_IN_UPDATE_IS_LOADING', payload: true } ],
                    [ { type: 'TRADE_IN_RESET_TABLE', payload: 42 } ],
                    [ { type: 'TRADE_IN_UPDATE_IS_LOADING', payload: false } ],
                ]);
        });
});

it('должен вызвать gateApi.getResource, metrika.sendPageEvent, updateUrl ' +
    'и dispatch пачку экшнов с корректными параметрами ' +
    'если dateRange.toDate', () => {
    const dispatch = jest.fn();
    const updateUrl = require('www-cabinet/react/dataDomain/tradeIn/actions/updateUrl');
    updateUrl.mockClear();

    const params = jest.fn();
    getMetrika.mockImplementation(() => ({
        params,
    }));

    const getState = jest.fn(() => ({
        config: {
            client: {
                id: 'dealer_id',
            },
        },
        tradeIn: {
            filters: {
                section: 'NEW',
                dateRange: {
                    fromDate: 'fromDate',
                },
            },
        },
    }));
    const updatedParams = {
        from_date: 'fromDate',
        section: 'USED',
        page: 1,
    };

    return changeSection('USED')(dispatch, getState)
        .then(() => {
            expect(gateApi.getResource)
                .toHaveBeenCalledWith('getDealerTradeIn', { ...updatedParams, dealer_id: 'dealer_id' });
            expect(params)
                .toHaveBeenCalledWith('trade-in_select_used');
            expect(updateUrl)
                .toHaveBeenCalledWith(updatedParams);
            expect(dispatch.mock.calls)
                .toEqual([
                    [ { type: 'TRADE_IN_UPDATE_IS_LOADING', payload: true } ],
                    [ { type: 'TRADE_IN_RESET_TABLE', payload: 42 } ],
                    [ { type: 'TRADE_IN_UPDATE_IS_LOADING', payload: false } ],
                ]);
        });
});

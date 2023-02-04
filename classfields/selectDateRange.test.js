jest.mock('www-cabinet/react/dataDomain/tradeIn/actions/updateUrl', () => jest.fn());
jest.mock('www-cabinet/react/lib/getMetrika', () => {
    return () => ({
        params: jest.fn(),
    });
});

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

const gateApi = require('auto-core/react/lib/gateApi');
const selectDateRange = require('./selectDateRange');

it('должен вызвать getDealerTradeIn, metrika.sendPageEvent, updateUrl ' +
    'и dispatch пачку actions с корректными параметрами', () => {
    const updateUrl = require('www-cabinet/react/dataDomain/tradeIn/actions/updateUrl');
    const dispatch = jest.fn();

    gateApi.getResource.mockImplementation(() => Promise.resolve(42));

    const getState = jest.fn(() => ({
        tradeIn: {
            filters: {
                section: 'ALL',
                dateRange: {
                    fromDate: 'fromDate',
                    toDate: 'toDate',
                },
            },
        },
    }));

    const updatedParams = {
        from_date: 'newFromDate',
        to_date: 'newToDate',
        section: 'ALL',
        page: 1,
    };

    return selectDateRange({ from: 'newFromDate', to: 'newToDate' })(dispatch, getState)
        .then(() => {
            expect(gateApi.getResource).toHaveBeenCalledWith('getDealerTradeIn', updatedParams);
            expect(updateUrl).toHaveBeenCalledWith(updatedParams);
            expect(dispatch.mock.calls)
                .toEqual([
                    [ { type: 'TRADE_IN_UPDATE_IS_LOADING', payload: true } ],
                    [ { type: 'TRADE_IN_RESET_TABLE', payload: 42 } ],
                    [ { type: 'TRADE_IN_UPDATE_IS_LOADING', payload: false } ],
                ]);
        });
});

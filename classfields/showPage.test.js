jest.mock('auto-core/react/actions/scroll', () => jest.fn());
jest.mock('www-cabinet/react/dataDomain/tradeIn/actions/updateUrl', () => jest.fn());

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

const gateApi = require('auto-core/react/lib/gateApi');

gateApi.getResource.mockImplementation(() => Promise.resolve(42));

const showPage = require('./showPage');

it('должен вызвать getDealerTradeIn, updateUrl' +
    'и dispatch массив actions с корретными параметрами', () => {
    const updateUrl = require('www-cabinet/react/dataDomain/tradeIn/actions/updateUrl');
    const scrollTo = require('auto-core/react/actions/scroll');
    scrollTo.mockClear();
    const dispatch = jest.fn();
    const getState = jest.fn(() => ({
        tradeIn: {
            filters: {
                section: 'USED',
                dateRange: {
                    fromDate: 'fromDate',
                    toDate: 'toDate',
                },
            },
        },
    }));
    const updatedParams = {
        from_date: 'fromDate',
        to_date: 'toDate',
        section: 'USED',
        page: 2,
    };

    return showPage(2)(dispatch, getState)
        .then(() => {
            expect(scrollTo).toHaveBeenCalledWith('main');
            expect(gateApi.getResource).toHaveBeenCalledWith('getDealerTradeIn', updatedParams);
            expect(updateUrl).toHaveBeenCalledWith(updatedParams);
            expect(dispatch.mock.calls).toEqual([
                [ { type: 'TRADE_IN_UPDATE_IS_LOADING', payload: true } ],
                [ { type: 'TRADE_IN_RESET_TABLE', payload: 42 } ],
                [ { type: 'TRADE_IN_UPDATE_IS_LOADING', payload: false } ],
            ]);
        });
});

it('должен вызвать getDealerTradeIn, updateUrl, scroll ' +
    'и dispatch массив actions с корретными параметрами ' +
    'if there is no dateRange.toDate', () => {
    const updateUrl = require('www-cabinet/react/dataDomain/tradeIn/actions/updateUrl');
    const scrollTo = require('auto-core/react/actions/scroll');
    scrollTo.mockClear();
    const dispatch = jest.fn();
    const getState = jest.fn(() => ({
        tradeIn: {
            filters: {
                section: 'USED',
                dateRange: {
                    fromDate: 'fromDate',
                },
            },
            paging: {
                min: 0,
                max: 20,
                current: 1,
            },

        },
    }));
    const updatedParams = {
        from_date: 'fromDate',
        section: 'USED',
        page: 10,
    };

    return showPage(10)(dispatch, getState)
        .then(() => {
            expect(scrollTo).toHaveBeenCalledWith('main');
            expect(gateApi.getResource).toHaveBeenCalledWith('getDealerTradeIn', updatedParams);
            expect(updateUrl).toHaveBeenCalledWith(updatedParams);
            expect(dispatch.mock.calls).toEqual([
                [ { type: 'TRADE_IN_UPDATE_IS_LOADING', payload: true } ],
                [ { type: 'TRADE_IN_RESET_TABLE', payload: 42 } ],
                [ { type: 'TRADE_IN_UPDATE_IS_LOADING', payload: false } ],
            ]);
        });
});

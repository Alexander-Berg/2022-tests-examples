jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

const gateApi = require('auto-core/react/lib/gateApi');
const showMore = require('./showMore');

gateApi.getResource.mockImplementation(() => Promise.resolve(42));

it('должен вызвать getDealerTradeIn и dispatch пачку actions с корректными параметрами', () => {
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
            paging: {
                min: 0,
                max: 20,
                current: 1,
            },
        },
    }));

    return showMore(10)(dispatch, getState)
        .then(() => {
            expect(gateApi.getResource)
                .toHaveBeenCalledWith('getDealerTradeIn',
                    {
                        from_date: 'fromDate',
                        to_date: 'toDate',
                        section: 'USED',
                        page: 10,
                    });
            expect(dispatch.mock.calls)
                .toEqual([
                    [ { type: 'TRADE_IN_UPDATE_IS_LOADING', payload: true } ],
                    [ { type: 'TRADE_IN_ADD_ITEMS', payload: 42 } ],
                    [ { type: 'TRADE_IN_UPDATE_IS_LOADING', payload: false } ],
                ]);
        });
});

it('должен вызвать getDealerTradeIn и dispatch пачку actions с корректными параметрами ' +
    'если нет dateRange.toDate', () => {
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

    return showMore(10)(dispatch, getState)
        .then(() => {
            expect(gateApi.getResource)
                .toHaveBeenCalledWith('getDealerTradeIn',
                    {
                        from_date: 'fromDate',
                        section: 'USED',
                        page: 10,
                    });
            expect(dispatch.mock.calls)
                .toEqual([
                    [ { type: 'TRADE_IN_UPDATE_IS_LOADING', payload: true } ],
                    [ { type: 'TRADE_IN_ADD_ITEMS', payload: 42 } ],
                    [ { type: 'TRADE_IN_UPDATE_IS_LOADING', payload: false } ],
                ]);
        });
});

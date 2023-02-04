jest.mock('auto-core/react/lib/gateApi');
const load = require('./load');

it('не должен dispatch actions если state.sales.isFetching (данные загружаеются)', () => {
    const dispatch = jest.fn();
    const getState = jest.fn(() => ({ sales: { isFetching: true } }));

    load({})(dispatch, getState);
    expect(dispatch.mock.calls).toEqual([]);
});

it('должен вызвать gateApi.getPage с корретными параметрами и вернуть корректный набор actions', () => {
    const dispatch = jest.fn();
    const getState = jest.fn(() => ({
        sales: {
            isFetching: false,
        },
    }));
    const gateApi = require('auto-core/react/lib/gateApi');

    gateApi.getPage = jest.fn(() => Promise.resolve({
        sales: {
            userOffers: {
                offers: [ 'item1', 'item2', 'item3' ],
                pagination: 'pagination',
            },
        },
    }));

    return load({
        status: 'inactive',
    })(dispatch, getState)
        .then(() => {
            expect(gateApi.getPage).toHaveBeenCalledWith('sales', {
                status: [ 'inactive', 'expired' ],
                with_daily_counters: true,
                counters: true,
            });

            expect(dispatch.mock.calls).toEqual([
                [ { type: 'LOAD_SALES_PENDING' } ],
                [ {
                    type: 'LOAD_SALES_RESOLVED',
                    payload: {
                        sales: {
                            items: [ 'item1', 'item2', 'item3' ],
                            pager: 'pagination',
                        },
                    },
                } ],
            ]);
        });
});

it(
    'должен вызвать gateApi.getPage с корретными параметрами и вернуть корректный набор actions, ' +
    'если в ответе не нашлось .sales.userOffers', () => {
        const dispatch = jest.fn();
        const getState = jest.fn(() => ({
            sales: {
                isFetching: false,
            },
        }));
        const gateApi = require('auto-core/react/lib/gateApi');

        gateApi.getPage = jest.fn(() => Promise.resolve({
            sales: {},
        }));

        return load({
            status: 'active',
            sort: 'sort',
            sort_dir: 'sort_dir',
        })(dispatch, getState)
            .then(() => {
                expect(gateApi.getPage).toHaveBeenCalledWith('sales', {
                    sort: 'sort',
                    sort_dir: 'sort_dir',
                    status: 'active',
                    with_daily_counters: true,
                    counters: true,
                });

                expect(dispatch.mock.calls).toEqual([
                    [ { type: 'LOAD_SALES_PENDING' } ],
                    [ {
                        type: 'LOAD_SALES_REJECTED',
                        payload: {
                            sort: {
                                field: 'sort',
                                dir: 'sort_dir',
                            },
                        },
                    } ],
                ]);
            });
    });

it('должен вызвать gateApi.getPage с корретными параметрами и вернуть корректный набор actions, ' +
    'если gateApi.getPage возвращает Promise.reject', () => {
    const dispatch = jest.fn();
    const getState = jest.fn(() => (
        {
            sales: {
                isFetching: false,
            },
        }
    ));
    const gateApi = require('auto-core/react/lib/gateApi');

    gateApi.getPage = jest.fn(() => Promise.reject());

    return load({
        status: 'active',
        sort: 'sort',
        sort_dir: 'sort_dir',
    })(dispatch, getState)
        .then(() => {
            expect(gateApi.getPage).toHaveBeenCalledWith('sales', {
                sort: 'sort',
                sort_dir: 'sort_dir',
                status: 'active',
                with_daily_counters: true,
                counters: true,
            });

            expect(dispatch.mock.calls).toEqual([
                [ { type: 'LOAD_SALES_PENDING' } ],
                [ {
                    type: 'LOAD_SALES_REJECTED',
                    payload: {
                        sort: {
                            field: 'sort',
                            dir: 'sort_dir',
                        },
                    },
                } ],
            ]);
        });
});

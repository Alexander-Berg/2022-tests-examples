const deleteAutostrategy = require('./deleteAutostrategy').default;
jest.mock('www-cabinet/react/dataDomain/notifier/actions', () => ({
    showErrorNotification: () => ({ type: 'SHOW_ERROR_NOTIFICATION' }),
    showInfoNotification: () => ({ type: 'SHOW_INFO_NOTIFICATION' }),
}));

it('Должен удалить автостратегию и перезапросить аукционы', () => {
    const dispatch = jest.fn();
    const getState = jest.fn();
    const gateApi = require('auto-core/react/lib/gateApi');

    gateApi.getResource = jest.fn(() => Promise.resolve({ SUCCESS: true }));

    return deleteAutostrategy({
        body: {
            context: { mark_code: 'CHERY', model_code: 'BONUS' },
        },
        dealerId: 20101,
    })(dispatch, getState).then(() => {
        expect(gateApi.getResource).toHaveBeenCalledWith('postDealerAuctionAutoStrategyDelete', {
            body: {
                context: { mark_code: 'CHERY', model_code: 'BONUS' },
            },
            dealer_id: 20101,
        });
        expect(gateApi.getResource).toHaveBeenCalledWith('getDealerAuctionCurrentState', { dealer_id: 20101 });
    });
});

it('Не должен вызывать showInfoNotification, если передан параметр silent', () => {
    const dispatch = jest.fn();
    const getState = jest.fn();
    const gateApi = require('auto-core/react/lib/gateApi');

    gateApi.getResource = jest.fn(() => Promise.resolve({ SUCCESS: true }));

    return deleteAutostrategy({
        body: {
            context: { mark_code: 'CHERY', model_code: 'BONUS' },
        },
        dealerId: 20101,
        silent: true,
    })(dispatch, getState).then(() => {
        expect(dispatch.mock.calls).toHaveLength(1);
        expect(dispatch.mock.calls[0][0].type).toBe('UPDATE_AUCTION');
    });
});

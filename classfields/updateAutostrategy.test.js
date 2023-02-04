const updateAutostrategy = require('./updateAutostrategy').default;

it('Должен обновить автостратегию и перезапросить аукционы', () => {
    const dispatch = jest.fn();
    const getState = jest.fn();
    const gateApi = require('auto-core/react/lib/gateApi');

    gateApi.getResource = jest.fn(() => Promise.resolve({ SUCCESS: true }));

    return updateAutostrategy({
        body: {
            context: { mark_code: 'CHERY', model_code: 'BONUS' },
            auto_strategy: { max_bid: 400, max_position_for_price: {} },
        },
        dealerId: 20101,
    })(dispatch, getState).then(() => {
        expect(gateApi.getResource).toHaveBeenCalledWith('postDealerAuctionAutoStrategyChange', {
            body: {
                context: { mark_code: 'CHERY', model_code: 'BONUS' },
                auto_strategy: { max_bid: 400, max_position_for_price: {} },
            },
            dealer_id: 20101,
        });
        expect(gateApi.getResource).toHaveBeenCalledWith('getDealerAuctionCurrentState', { dealer_id: 20101 });
    });
});

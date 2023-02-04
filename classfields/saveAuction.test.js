jest.mock('./deleteAutostrategy').default;

const auctions = require('./mock/auctions');

const saveAuction = require('./saveAuction').default;

it('Должен сделать несколько запросов в бекенд, а потом - обновить ставки аукционов', () => {
    const dispatch = jest.fn();
    const getState = jest.fn();
    const deleteAutostrategy = require('./deleteAutostrategy').default;
    const gateApi = require('auto-core/react/lib/gateApi');

    gateApi.getResource = jest.fn(() => Promise.resolve({ SUCCESS: true }));

    return saveAuction({
        ...auctions,
    })(dispatch, getState).then(() => {
        expect(deleteAutostrategy).toHaveBeenCalledWith({ body: {
            context: {
                mark_code: 'BMW',
                model_code: 'X5',
            },
        }, dealerId: undefined, silent: true });
        expect(gateApi.getResource).toHaveBeenCalledWith('postDealerAuctionPlaceBid', {
            body: {
                bid: 2500,
                context: {
                    mark_code: 'BMW',
                    model_code: 'X5',
                },
                dealer_id: undefined,
            },
        });
        expect(gateApi.getResource).toHaveBeenCalledWith('postDealerAuctionLeave', {
            body: {
                previous_bid: 2500,
                bid: 2000,
                context: {
                    mark_code: 'BMW',
                    model_code: 'X7',
                },
                dealer_id: undefined,
            },
        });
        expect(gateApi.getResource).toHaveBeenCalledWith('getDealerAuctionCurrentState', { dealer_id: undefined });
    });
});

const getMetrikaSellerType = require('auto-core/react/lib/offer/getMetrikaSellerType');

it('должен вернуть client для салона', () => {
    expect(getMetrikaSellerType({ seller_type: 'COMMERCIAL' })).toEqual('client');
});

it('должен вернуть user для частника', () => {
    expect(getMetrikaSellerType({ seller_type: 'PRIVATE' })).toEqual('user');
});

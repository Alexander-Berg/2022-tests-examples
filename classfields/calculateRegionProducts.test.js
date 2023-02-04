const calculateRegionProducts = require('www-cabinet/react/dataDomain/deliverySettings/helpers/calculateRegionProducts');

it('должен вернуть корректный объект', () => {
    expect(calculateRegionProducts({
        offers: {
            '111-111': [
                { product: 'product1', price: '100' },
                { product: 'product2', price: '200' },
                { product: 'product3', price: '300' },
                { product: 'product4', price: '400' },
            ],
            '222-222': [
                { product: 'product1', price: '100' },
                { product: 'product4', price: '400' },
            ],
            '333-222': [
                { product: 'product5', price: '500' },
            ],
        },
    })).toEqual([
        { title: 'product1', price: 200 },
        { title: 'product2', price: 200 },
        { title: 'product3', price: 300 },
        { title: 'product4', price: 800 },
        { title: 'product5', price: 500 },
    ]);
});

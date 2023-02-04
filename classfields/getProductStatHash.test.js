const getProductStatHash = require('./getProductStatHash');

it('должен правильно составлять хэш для статистики списаний по продукту', () => {
    const product = {
        product: 'placement',
        date: '2014-11-11',
    };

    expect(getProductStatHash(product)).toBe('placement_2014-11-11');
});

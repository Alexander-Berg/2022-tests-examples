const getSortForTradeIn = require('./getSortForTradeIn');

it('должен вернуть сортировку по актуальности, если отношение средней цены к оценочной стоимости автомобиля меньше 7', () => {
    const avgPrice = 100000;
    const tradeInEstimatedCost = 20000;
    expect(getSortForTradeIn(avgPrice, tradeInEstimatedCost)).toBe('fresh_relevance_1-desc');
});

it('должен вернуть сортировку по возрастанию цены, если отношение средней цены к оценочной стоимости автомобиля больше или равно 7', () => {
    const avgPrice = 100000;
    const tradeInEstimatedCost = 10000;
    expect(getSortForTradeIn(avgPrice, tradeInEstimatedCost)).toBe('price-asc');
});

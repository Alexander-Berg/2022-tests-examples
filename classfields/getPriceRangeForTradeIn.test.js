const MockDate = require('mockdate');

const getPriceRangeForTradeIn = require('./getPriceRangeForTradeIn');

beforeEach(() => {
    MockDate.set('2019-11-25');
});

it('должен вернуть минимальную цену на 20% меньше средней, максимальную на 40% больше средней, если возраст < 7', () => {
    const averagePrice = 100000;
    const year = 2017;
    const tradeInEstimatedCost = 50000;
    expect(getPriceRangeForTradeIn(averagePrice, year, tradeInEstimatedCost)).toStrictEqual([ 80000, 140000 ]);
});

it('должен вернуть минимальную цену на 40% меньше средней, максимальную на 20% больше средней, если возраст >= 7', () => {
    const averagePrice = 100000;
    const year = 2012;
    const tradeInEstimatedCost = 50000;
    expect(getPriceRangeForTradeIn(averagePrice, year, tradeInEstimatedCost)).toStrictEqual([ 60000, 120000 ]);
});

it(`должен вернуть минимальную цену на 10% больше оценочной стоимости автомобиля,
    а макcимальную в два раза больше оценочной стоимости,
    если расчитанная минимальная цена меньше оценочнной стоимости + 10%,
    а рассчитанная максимальная цена меньше двойной оценочной цены`, () => {
    const averagePrice = 100000;
    const year = 2017;
    const tradeInEstimatedCost = 100000;
    expect(getPriceRangeForTradeIn(averagePrice, year, tradeInEstimatedCost)).toStrictEqual([ 110000, 200000 ]);
});

it(`должен вернуть минимальную цену на 10% больше оценочной стоимости автомобиля,
    а макcимальную null, если средняя цена === 0`, () => {
    const averagePrice = 0;
    const year = 2017;
    const tradeInEstimatedCost = 100000;
    expect(getPriceRangeForTradeIn(averagePrice, year, tradeInEstimatedCost)).toStrictEqual([ 110000, null ]);
});

it(`должен вернуть минимальную и макcимальную как null, если средняя цена === 0 и оценочная стоимость не передана`, () => {
    const averagePrice = 0;
    const year = 2017;
    expect(getPriceRangeForTradeIn(averagePrice, year)).toStrictEqual([ null, null ]);
});

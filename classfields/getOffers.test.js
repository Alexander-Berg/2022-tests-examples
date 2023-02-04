const getOffers = require('./getOffers');
const offer = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');

it('Должен отдать null для оффера без цены', () => {
    const offers = [ {
        ...offer,
        price_info: {},
    } ];

    expect(getOffers(offers)[0]).toEqual(null);
});

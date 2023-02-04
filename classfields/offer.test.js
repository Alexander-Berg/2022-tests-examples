const offer = require('./offer');
const offerMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');

it('Должен отдать null для оффера без цены', () => {
    const mock = {
        ...offerMock,
        price_info: {},
    };

    expect(offer(mock)).toEqual(null);
});

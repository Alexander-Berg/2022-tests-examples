const MockDate = require('mockdate');

const getCurrentLoyaltyPromocode = require('./getCurrentLoyaltyPromocode');

const promoFeatures = [
    { count: 1, tag: 'special-offer', createTs: '2019-10-24T18:11:07.473+03:00' },
    { count: 2, tag: 'loyalty', createTs: '2019-10-24T18:11:07.473+03:00' },
];

it(`должен находить промокод лояльности за текущий месяц`, () => {
    MockDate.set('2019-10-05');

    const state = {
        promocodeFeatures: {
            features: promoFeatures,
        },
    };

    const newState = getCurrentLoyaltyPromocode(state);
    expect(newState).toEqual(promoFeatures[1]);
});

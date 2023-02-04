const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');
const offerMock = require('autoru-frontend/mockData/responses/offer.mock').offer;

const generatePriceSegmentGoalName = require('./generatePriceSegmentGoalName');

const CASES = [
    {
        price: 250000,
        goalName: 'PHONE_ALL_CARS2_PRICE-LOW-300',
    },
    {
        price: 300000,
        goalName: 'PHONE_ALL_CARS2_PRICE-300-500',
    },
    {
        price: 1000000,
        goalName: 'PHONE_ALL_CARS2_PRICE-500-1500',
    },
    {
        price: 2500000,
        goalName: 'PHONE_ALL_CARS2_PRICE-1500-HIGH',
    },
];

it('должен ничего не вернуть, если категория не cars', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withCategory('moto')
        .value();

    expect(generatePriceSegmentGoalName(offer)).toBeNull();
});

it('должен ничего не вернуть, если почему-то не пришла цена в рублях', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withPrice()
        .value();

    expect(generatePriceSegmentGoalName(offer)).toBeNull();
});

CASES.forEach((test) => {
    it(`должен правильно вернуть цель ${ test.goalName }`, () => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withPrice(test.price)
            .value();

        expect(generatePriceSegmentGoalName(offer)).toEqual(test.goalName);
    });
});

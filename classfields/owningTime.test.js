const owningTime = require('./owningTime');

const MockDate = require('mockdate');
const offerMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');

it('должен вернуть разницу между датами', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withPurcaseDate({
            year: 2009,
            month: 10,
            day: 1,
        })
        .value();

    MockDate.set('Jul 02 2020 16:28:40 GMT+0300');

    expect(owningTime(offer)).toBe('10 лет и 9 месяцев');
});

// https://github.com/iamkun/dayjs/issues/1433
it('должен обойти баг dayjs', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withPurcaseDate({
            year: 2009,
            month: 1,
        })
        .value();

    MockDate.set('Dec 30 2010 16:28:40 GMT+0300');

    expect(owningTime(offer)).toBe('1 год и 11 месяцев');
});

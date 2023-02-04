const getOffersForDistributeDealers = require('./getOffersForDistributeDealers');

const offerMock = require('autoru-frontend/mockData/responses/offer.mock').offer;
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');

const offer = cloneOfferWithHelpers(offerMock);

const SALON_MOCK_BMW = { code: 'salon_bmw' };
const SALON_MOCK_AUDI = { code: 'salon_audi' };
const SALON_MOCK_LADA = { code: 'salon_lada' };
const SALON_MOCK_KIA = { code: 'salon_kia' };

const offersWithAuctionPrice = [
    offer.withSalon(SALON_MOCK_BMW).withSaleId('111-222').withTags([ 'auction_price' ]).value(),
    offer.withSalon(SALON_MOCK_AUDI).withSaleId('111-444').withTags([ 'auction_price' ]).value(),
    offer.withSalon(SALON_MOCK_LADA).withSaleId('111-555').withTags([ 'auction_price' ]).value(),
];

const standardOffers = [
    offer.withSalon(SALON_MOCK_BMW).withSaleId('111-333').value(),
    offer.withSalon(SALON_MOCK_AUDI).withSaleId('222-222').value(),
    offer.withSalon(SALON_MOCK_LADA).withSaleId('333-333').value(),
];

it('должен вернуть стандартные офферы, если не было офферов, участвующих в аукционе', () => {
    expect(getOffersForDistributeDealers(standardOffers)).toEqual(standardOffers);
});

it('должен вернуть офферы, участвующие в аукционе, если они есть', () => {
    const standardOffers = [
        offer.withSalon(SALON_MOCK_BMW).withSaleId('111-333').value(),
        offer.withSalon(SALON_MOCK_AUDI).withSaleId('222-222').value(),
        ...offersWithAuctionPrice,
    ];

    const result = getOffersForDistributeDealers(standardOffers);

    expect(new Set(result))
        .toEqual(new Set(offersWithAuctionPrice));
    expect(new Set(offersWithAuctionPrice))
        .toEqual(new Set(result));

    expect(result).toHaveLength(3);
});

it('должен вернуть офферы, участвующие в аукционе, и дополнить массив до трех офферами, не участвующими в аукционе', () => {
    const offerWithAuctionPrice = offer.withSalon(SALON_MOCK_KIA).withSaleId('111-666').withTags([ 'auction_price' ]).value();
    const offers = [
        ...standardOffers,
        offerWithAuctionPrice,
    ];

    const result = getOffersForDistributeDealers(offers);

    expect(getOffersForDistributeDealers(result)).toContainEqual(offerWithAuctionPrice);
    expect(getOffersForDistributeDealers(result)).toHaveLength(3);
});

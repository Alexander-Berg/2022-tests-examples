const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');
const offerMock = require('autoru-frontend/mockData/responses/offer.mock');

const getUniqOffersForDistributeDealers = require('./getUniqOffersForDistributeDealers');

const DEALERS_BLACKLIST_BLANK = [];

const SALON_MOCK_BMW = { code: 'salon_bmw' };
const SALON_MOCK_AUDI = { code: 'salon_audi' };
const SALON_MOCK_LADA = { code: 'salon_lada' };

const offers = [
    cloneOfferWithHelpers(offerMock).withSalon(SALON_MOCK_BMW).withSaleId('111-111').value(),
    cloneOfferWithHelpers(offerMock).withSalon(SALON_MOCK_BMW).withSaleId('111-222').value(),
    cloneOfferWithHelpers(offerMock).withSalon(SALON_MOCK_BMW).withSaleId('111-333').value(),
    cloneOfferWithHelpers(offerMock).withSalon(SALON_MOCK_AUDI).withSaleId('222-111').value(),
    cloneOfferWithHelpers(offerMock).withSalon(SALON_MOCK_AUDI).withSaleId('222-222').value(),
    cloneOfferWithHelpers(offerMock).withSalon(SALON_MOCK_LADA).withSaleId('333-333').value(),
];

it('должен оставить всех дилеров, если нет повторных', () => {
    const offersUniq = [
        cloneOfferWithHelpers(offerMock).withSalon(SALON_MOCK_BMW).withSaleId('111-111').value(),
        cloneOfferWithHelpers(offerMock).withSalon(SALON_MOCK_AUDI).withSaleId('222-111').value(),
        cloneOfferWithHelpers(offerMock).withSalon(SALON_MOCK_LADA).withSaleId('333-333').value(),
    ];

    const result = getUniqOffersForDistributeDealers(offersUniq, DEALERS_BLACKLIST_BLANK);

    expect(result).toEqual(offersUniq);
});

it('должен не сломаться при пустом массиве офферов', () => {
    const offersBlank = [];

    const result = getUniqOffersForDistributeDealers(offersBlank, DEALERS_BLACKLIST_BLANK);

    expect(result).toEqual([]);
});

it('Должен отфильтровать салон из блеклиста', () => {
    const dealersBlacklistLada = [ SALON_MOCK_LADA.code ];

    const referenceOffers = [
        cloneOfferWithHelpers(offerMock).withSalon(SALON_MOCK_BMW).withSaleId('111-111').value(),
        cloneOfferWithHelpers(offerMock).withSalon(SALON_MOCK_AUDI).withSaleId('222-111').value(),
    ];

    const result = getUniqOffersForDistributeDealers(offers, dealersBlacklistLada);

    expect(result).toEqual(referenceOffers);
});

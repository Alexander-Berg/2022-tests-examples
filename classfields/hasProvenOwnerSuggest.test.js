const hasProvenOwnerSuggest = require('./hasProvenOwnerSuggest');

const cardMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');

let offer;
beforeEach(() => {
    offer = cloneOfferWithHelpers(cardMock)
        .withIsOwner(true)
        .withTags([ 'some_tag' ])
        .withSellerTypePrivate()
        .withStatus('ACTIVE');
});

it('должен вернуть true для активного объявления владельца, частника, не перекупа, без тега проверенного владельца', () => {
    expect(hasProvenOwnerSuggest(offer.value())).toEqual(true);
});

it('должен вернуть false для неактивного объявления владельца, частника, не перекупа, без тега проверенного владельца', () => {
    offer = offer
        .withStatus('INACTIVE')
        .value();
    expect(hasProvenOwnerSuggest(offer)).toEqual(false);
});

it('должен вернуть false для активного объявления невладельца, частника, не перекупа, без тега проверенного владельца', () => {
    offer = offer
        .withIsOwner(false)
        .value();
    expect(hasProvenOwnerSuggest(offer)).toEqual(false);
});

it('должен вернуть false для активного объявления владельца, не частника, не перекупа, без тега проверенного владельца', () => {
    offer = offer
        .withSellerTypeCommercial()
        .value();
    expect(hasProvenOwnerSuggest(offer)).toEqual(false);
});

it('должен вернуть false для активного объявления владельца, частника, перекупа, без тега проверенного владельца', () => {
    offer = offer.value();
    offer.service_prices.find(sp => sp.service === 'all_sale_activate').payment_reason = 'USER_QUOTA_EXCEED';
    expect(hasProvenOwnerSuggest(offer)).toEqual(false);
});

it('должен вернуть false для активного объявления владельца, частника, не перекупа, c тега проверенного владельца', () => {
    offer = offer
        .withTags([ 'proven_owner' ])
        .value();
    expect(hasProvenOwnerSuggest(offer)).toEqual(false);
});

it('должен вернуть false, если машина не зарегистрирована в РФ', () => {
    offer = offer
        .withNotRegisteredInRussia(true)
        .value();
    expect(hasProvenOwnerSuggest(offer)).toEqual(false);
});

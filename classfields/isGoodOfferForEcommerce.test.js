const _ = require('lodash');

const offerMock = require('autoru-frontend/mockData/responses/offer.mock').offer;

const isGoodOfferForEcommerce = require('./isGoodOfferForEcommerce');

it('должен вернуть true для активного оффера с фото, не битого, не мото', () => {
    expect(isGoodOfferForEcommerce(offerMock)).toEqual(true);
});

it('должен вернуть false для оффера из категории мото', () => {
    const badOfferMock = _.cloneDeep(offerMock);

    badOfferMock.category = 'moto';

    expect(isGoodOfferForEcommerce(badOfferMock)).toEqual(false);
});

it('должен вернуть false для оффера без фото (проверка gallery)', () => {
    const badOfferMock = _.cloneDeep(offerMock);

    badOfferMock.gallery = [];

    expect(isGoodOfferForEcommerce(badOfferMock)).toEqual(false);
});

it('должен вернуть false для оффера без фото (проверка image_urls)', () => {
    const badOfferMock = _.cloneDeep(offerMock);

    badOfferMock.state = {
        ..._.cloneDeep(offerMock.state),
        image_urls: [],
    };

    expect(isGoodOfferForEcommerce(badOfferMock)).toEqual(false);
});

it('должен вернуть false для оффера с битой тачкой (проверка id)', () => {
    const badOfferMock = _.cloneDeep(offerMock);

    badOfferMock.state = {
        ..._.cloneDeep(offerMock.state),
        id: 'BEATEN',
    };

    expect(isGoodOfferForEcommerce(badOfferMock)).toEqual(false);
});

it('должен вернуть false для оффера с битой тачкой (проверка state_not_beaten', () => {
    const badOfferMock = _.cloneDeep(offerMock);

    badOfferMock.state = {
        ..._.cloneDeep(offerMock.state),
        state_not_beaten: false,
    };

    expect(isGoodOfferForEcommerce(badOfferMock)).toEqual(false);
});

it('должен вернуть false для неактивного оффера', () => {
    const badOfferMock = _.cloneDeep(offerMock);

    badOfferMock.status = 'INACTIVE';

    expect(isGoodOfferForEcommerce(badOfferMock)).toEqual(false);
});

const shouldActivateWithMosRu = require('./shouldActivateWithMosRu');
const cardMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');
const getMarkCode = require('./getMarkCode');

const bunker = {
    marks: [ getMarkCode(cardMock) ],
    regions: [ '1' ],
};

it('возвращает true если выполнены все условия', () => {
    const offer = getPositiveCaseCondition()
        .value();

    expect(shouldActivateWithMosRu(offer, bunker, false)).toBe(true);
});

describe('возвращает false если', () => {
    it('продавец не частник', () => {
        const offer = getPositiveCaseCondition()
            .withSellerTypeCommercial()
            .value();

        expect(shouldActivateWithMosRu(offer, bunker, false)).toBe(false);
    });

    it('если марка не в экспе', () => {
        const offer = getPositiveCaseCondition()
            .value();

        expect(shouldActivateWithMosRu(offer, [ 'FERRARI' ])).toBe(false);
    });

    it('регион не в экспе', () => {
        const offer = getPositiveCaseCondition()
            .withSellerGeoParentsIds([ '2' ])
            .value();

        expect(shouldActivateWithMosRu(offer, bunker, false)).toBe(false);
    });

    it('если у пользователя есть мос.ру аккаунт', () => {
        const offer = getPositiveCaseCondition()
            .value();

        expect(shouldActivateWithMosRu(offer, bunker, true)).toBe(false);
    });

    it('если статус не "неактивно"', () => {
        const offer = getPositiveCaseCondition()
            .withStatus('BANNED')
            .value();

        expect(shouldActivateWithMosRu(offer, bunker, false)).toBe(false);
    });

    it('объява уже была активирована ранее', () => {
        const offer = getPositiveCaseCondition()
            .withWasActive(true)
            .value();

        expect(shouldActivateWithMosRu(offer, bunker, false)).toBe(false);
    });

    it('активация платная', () => {
        const offer = getPositiveCaseCondition()
            .withCustomVas({ service: 'all_sale_activate', price: 1 })
            .value();

        expect(shouldActivateWithMosRu(offer, bunker, false)).toBe(false);
    });

    it('есть активный вас', () => {
        const offer = getPositiveCaseCondition()
            .withCustomActiveServices([ { service: 'all_sale_color' } ])
            .value();

        expect(shouldActivateWithMosRu(offer, bunker, false)).toBe(false);
    });
});

function getPositiveCaseCondition() {
    return cloneOfferWithHelpers(cardMock)
        .withSellerTypePrivate()
        .withSellerGeoParentsIds([ '1' ])
        .withStatus('INACTIVE')
        .withWasActive(false)
        .withCustomVas({ service: 'all_sale_activate', price: 0 })
        .withCustomActiveServices([]);
}

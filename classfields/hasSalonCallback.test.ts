import type { TOfferMock } from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import hasSalonCallback from './hasSalonCallback';

let offer: TOfferMock;
beforeEach(() => {
    offer = cloneOfferWithHelpers({})
        .withIsOwner(false)
        .withCategory('cars')
        .withSellerTypeCommercial()
        .withSection('used')
        .withSalon({ phone_callback_forbidden: false });
});

it('должен вернуть true, если это салон, не владелец объявления и не выключен обратный звонок', () => {
    expect(hasSalonCallback(offer.value())).toEqual(true);
});

it('должен вернуть false, если это частник', () => {
    offer = offer.withSellerTypePrivate();
    expect(hasSalonCallback(offer.value())).toEqual(false);
});

it('должен вернуть false, если это владелец', () => {
    offer = offer.withIsOwner(true);
    expect(hasSalonCallback(offer.value())).toEqual(false);
});

it('должен вернуть false, если выключен обратный звонок', () => {
    offer = offer.withSalon({ phone_callback_forbidden: true });
    expect(hasSalonCallback(offer.value())).toEqual(false);
});

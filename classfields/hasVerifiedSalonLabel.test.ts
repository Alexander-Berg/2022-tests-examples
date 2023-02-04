import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import hasVerifiedSalonLabel from './hasVerifiedSalonLabel';

let offer: Offer;
beforeEach(() => {
    offer = cloneOfferWithHelpers({})
        .withCategory('cars')
        .withSection('used')
        .withSalon({ loyalty_program: true })
        .value();
});

it('должен вернуть true, если это автомобиль с пробегом и у салона есть флаг', () => {
    expect(hasVerifiedSalonLabel(offer)).toEqual(true);
});

it('должен вернуть false, если это новый автомобиль и у салона есть флаг', () => {
    offer.section = 'new';
    expect(hasVerifiedSalonLabel(offer)).toEqual(false);
});

it('должен вернуть false, если это грузовик с пробегом и у салона есть флаг', () => {
    offer.category = 'trucks';
    expect(hasVerifiedSalonLabel(offer)).toEqual(false);
});

it('должен вернуть false, если это автомобиль с пробегом и у салона нет флага', () => {
    offer.salon && (offer.salon.loyalty_program = false);
    expect(hasVerifiedSalonLabel(offer)).toEqual(false);
});

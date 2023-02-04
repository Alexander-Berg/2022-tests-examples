import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import * as salonOffersCountText from './salonOffersCountText';

let offer: Offer;
beforeEach(() => {
    offer = cloneOfferWithHelpers({})
        .withCategory('cars')
        .withSalon({
            offer_counters: {
                cars_all: 1,
                moto_all: 2,
                trucks_all: 0,
            },
        })
        .value();
});

describe('toShow()', () => {
    it('должен вернуть false, если машин нет', () => {
        expect(salonOffersCountText.toShow(cloneOfferWithHelpers({ category: 'trucks' }).value())).toEqual(false);
    });

    it('должен вернуть true, если машины есть', () => {
        expect(salonOffersCountText.toShow(offer)).toEqual(true);
    });
});

describe('getText()', () => {
    it('должен вернуть "1 авто в наличии" для авто', () => {
        expect(salonOffersCountText.getText(offer)).toEqual('1 авто в наличии');
    });

    it('должен вернууть "2 т.с. в наличии" для мото', () => {
        offer.category = 'moto';

        expect(salonOffersCountText.getText(offer)).toEqual('2 т.с. в наличии');
    });

    it('не должен ничего вернуть, если машин нет', () => {
        offer.category = 'trucks';

        expect(salonOffersCountText.getText(offer)).toEqual('');
    });
});

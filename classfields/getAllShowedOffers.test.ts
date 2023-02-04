import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import getAllShowedOffers from './getAllShowedOffers';

const mockOffer = {} as Offer;

const state = {
    listingLocatorCounters: {
        data: [
            {
                radius: 200,
                count: 1,
                offers: [ mockOffer ],
            },
            {
                radius: 500,
                count: 2,
                offers: [ mockOffer, mockOffer ],
            },
            {
                radius: 1000,
                count: 3,
                offers: [ mockOffer, mockOffer, mockOffer ],
            },
        ],
        pending: false,
    },
};

it('вернет массив из офферов из всех каунтеров', () => {
    expect(getAllShowedOffers(state)).toHaveLength(6);
});

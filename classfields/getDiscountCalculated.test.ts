import { SellerType } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import type { TOfferMock } from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import getDiscount from './getDiscountCalculated';

let offer: TOfferMock;
beforeEach(() => {
    offer = cloneOfferWithHelpers({
        price_info: {
            EUR: 100,
            USD: 200,
            RUR: 300,
        } as Offer['price_info'],
        seller_type: SellerType.COMMERCIAL,
    });
});

it('without discount should return original price', () => {
    expect(getDiscount(offer.value())).toBeNull();
});

it('with discount should return lower RUR price', () => {
    expect(
        getDiscount(offer.withDiscountOptions({ max_discount: 50 }).value()),
    ).toEqual({
        EUR: 100,
        USD: 200,
        RUR: 250,
    });
});

import { CardViewType } from 'realty-core/types/eventLog';
import { OfferCategory } from 'realty-core/types/common';

import { getOfferCardViewType } from '../getOfferCardViewType';

describe('getOfferCardViewType', () => {
    it('Работает корректно', () => {
        expect(getOfferCardViewType({ offerCategory: OfferCategory.COMMERCIAL })).toEqual(CardViewType.COMMERCIAL);
        expect(getOfferCardViewType({ offerCategory: OfferCategory.COMMERCIAL, newFlatSale: true })).toEqual(
            CardViewType.COMMERCIAL
        );

        expect(getOfferCardViewType({ newFlatSale: true })).toEqual(CardViewType.NEW);
        expect(getOfferCardViewType({ newFlatSale: false })).toEqual(CardViewType.USED);
        expect(getOfferCardViewType({ newFlatSale: undefined })).toEqual(CardViewType.USED);
    });
});

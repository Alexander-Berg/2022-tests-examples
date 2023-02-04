import features from '../mocks/features';

import prepareFeatures from './prepareFeatures';

it('должен ограничить размер скидки', () => {
    expect(prepareFeatures(
        features,
        {
            maxPercentLoyaltyDiscount: 2,
            maxPercentLoyaltyDiscountDealerIds: [
                16453,
            ],
        }, 16453).discount).toBe(2);
});

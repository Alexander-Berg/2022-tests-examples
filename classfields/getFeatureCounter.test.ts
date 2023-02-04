import type { Feature } from 'www-cabinet/react/lib/features/types';

import getFeatureCounter from './getFeatureCounter';
const feature = {
    count: 2147473814,
    createTs: '2022-02-24T16:40:28.763+03:00',
    deadline: '2022-03-01T16:40:28.763+03:00',
    id: 'loyalty_placement:api_156:u_autoru_client_16453',
    jsonPayload: {
        discount: {
            discountType: 'percent',
            value: 7,
        },
        featureType: 'loyalty',
        unit: 'items',
    },
    origin: {
        id: '156',
        type: 'Api',
    },
    tag: 'loyalty_placement',
    total: 2147483647,
    user: 'autoru_client_16453',
} as Feature;

it('Должен ограничить размер скидки сверху для клиентов из бункера', () => {
    expect(getFeatureCounter(
        feature,
        true,
        {
            maxPercentLoyaltyDiscount: 2,
            maxPercentLoyaltyDiscountDealerIds: [
                16453,
            ],
        },
        16453,
    )).toBe('2%');
});

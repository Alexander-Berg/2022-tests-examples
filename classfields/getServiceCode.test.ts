import { TOfferVas } from 'auto-core/types/proto/auto/api/api_offer_model';

import getServiceCode from './getServiceCode';

it('returns correct code for provided vas type', () => {
    expect(getServiceCode('fresh')).toBe(TOfferVas.FRESH);
});

it('is insensible to letter case', () => {
    expect(getServiceCode('FRESH')).toBe(TOfferVas.FRESH);
});

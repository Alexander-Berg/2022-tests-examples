import { TOfferVas } from 'auto-core/types/proto/auto/api/api_offer_model';

import getServiceType from './getServiceType';

it('returns correct vas type for provided code', () => {
    expect(getServiceType(TOfferVas.FRESH)).toBe('FRESH');
});

it('returns minuscule type if the option is specified', () => {
    expect(getServiceType(TOfferVas.FRESH, false)).toBe('fresh');
});

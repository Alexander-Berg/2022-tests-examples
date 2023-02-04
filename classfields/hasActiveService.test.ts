import { TOfferVas } from 'auto-core/types/proto/auto/api/api_offer_model';
import type { PaidService } from 'auto-core/types/proto/auto/api/api_offer_model';

import hasActiveService from './hasActiveService';

const service = TOfferVas.COLOR;

it('returns correct result if provided array has specified active service', () => {
    const result = hasActiveService([ { service, is_active: true } ] as Array<PaidService>, service);
    expect(result).toBe(true);
});

it('returns correct result if provided array has non-active specified service', () => {
    const result = hasActiveService([ { service, is_active: false } ] as Array<PaidService>, service);
    expect(result).toBe(false);
});

it('returns correct result if provided array doesn\'t have specified active service', () => {
    const result = hasActiveService([ { service: 'package_turbo', is_active: true } ] as Array<PaidService>, service);
    expect(result).toBe(false);
});

it('returns correct result if there is no active services provided', () => {
    const result = hasActiveService(undefined, service);
    expect(result).toBe(false);
});

import type { PaidService } from 'auto-core/types/proto/auto/api/api_offer_model';
import { TOfferVas } from 'auto-core/types/proto/auto/api/api_offer_model';

import packageServicesReducer from './packageServicesReducer';

it('уберет сервисы, включенные в пакет', () => {
    const services = [ TOfferVas.FRESH, TOfferVas.SPECIAL, TOfferVas.TOP, TOfferVas.EXPRESS ].map((service) => ({ service })) as Array<PaidService>;
    const result = services.reduce(packageServicesReducer, []);
    expect(result).toEqual([ TOfferVas.FRESH, TOfferVas.EXPRESS ].map((service) => ({ service })));
});

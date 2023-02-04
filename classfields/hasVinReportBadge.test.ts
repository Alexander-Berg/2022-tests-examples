import { Status } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_resolution_enums';
import { OfferStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import hasVinReportBadge from './hasVinReportBadge';

it('должен быть бейдж', () => {
    expect(hasVinReportBadge(offerMock)).toBe(true);
});

it('не должно быть бейджа о наличии отчёта, если это не указано в vin_resolution', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withSection('used')
        .withVinResolution(Status.INVALID)
        .value();

    expect(hasVinReportBadge(offer)).toBe(false);
});

it('не должно быть бейджа для неактивного оффера', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withStatus(OfferStatus.INACTIVE)
        .withVinResolution(Status.OK)
        .value();

    expect(hasVinReportBadge(offer)).toBe(false);
});

it('не должно быть бейджа для заброннированного оффера', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withBookingStatus('BOOKED')
        .withVinResolution(Status.OK)
        .value();

    expect(hasVinReportBadge(offer)).toBe(false);
});

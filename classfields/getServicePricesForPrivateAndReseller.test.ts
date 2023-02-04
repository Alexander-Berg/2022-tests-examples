import _ from 'lodash';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import mockOffer from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import { TOfferVas } from 'auto-core/types/proto/auto/api/api_offer_model';

import getServicePricesForPrivateAndReseller from './getServicePricesForPrivateAndReseller';

it('вернет васы в нужном порядке и уберет лишние', () => {
    const offer = cloneOfferWithHelpers(mockOffer)
        .withServicePrices(_.shuffle(mockOffer.service_prices))
        .withActiveVas([])
        .value();

    expect(getServicePricesForPrivateAndReseller(offer).map(item => item.service))
        .toEqual([ TOfferVas.VIP, TOfferVas.TURBO, TOfferVas.STORIES, TOfferVas.TOP, TOfferVas.FRESH ]);
});

it('добавит активный ВАС, даже если у него не проставлен recommendation_priority', () => {
    const offer = cloneOfferWithHelpers(mockOffer)
        .withServicePrices([ {
            service: TOfferVas.TURBO,
            price: 999999,
            days: 61,
            description: '',
            name: '',
            recommendation_priority: 0,
        } ])
        .withActiveVas([ TOfferVas.TURBO ])
        .value();

    expect(getServicePricesForPrivateAndReseller(offer).map(item => item.service)).toEqual([ TOfferVas.TURBO ]);
});

it('если куплен пакет, то в ВАСы, которые в него входят, добавит код этого пакета', () => {
    const offer = cloneOfferWithHelpers(mockOffer)
        .withServicePrices(mockOffer.service_prices.filter(service => service.service !== TOfferVas.TURBO))
        .withActiveVas([ TOfferVas.VIP ])
        .value();

    expect(getServicePricesForPrivateAndReseller(offer).map(({ activeParentPackage, service }) => ({ service, activeParentPackage })))
        .toEqual([
            { service: TOfferVas.VIP, activeParentPackage: undefined },
            { service: TOfferVas.STORIES, activeParentPackage: undefined },
            { service: TOfferVas.TOP, activeParentPackage: TOfferVas.VIP },
            { service: TOfferVas.FRESH, activeParentPackage: TOfferVas.VIP },
        ]);
});

it('если пакет VIP активен, то проставит TURBO disabled', () => {
    const offer = cloneOfferWithHelpers(mockOffer)
        .withServicePrices(mockOffer.service_prices.map(service => service.service !== TOfferVas.TURBO ? service : { ...service, recommendation_priority: 0 }))
        .withActiveVas([ TOfferVas.VIP ])
        .value();

    expect(getServicePricesForPrivateAndReseller(offer).map(({ disabled, service }) => ({ service, disabled })))
        .toEqual([
            { service: TOfferVas.VIP, disabled: false },
            { service: TOfferVas.TURBO, disabled: true },
            { service: TOfferVas.STORIES, disabled: false },
            { service: TOfferVas.TOP, disabled: false },
            { service: TOfferVas.FRESH, disabled: false },
        ]);
});

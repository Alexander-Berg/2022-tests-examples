import _ from 'lodash';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import cardMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';

import { TOfferVas } from 'auto-core/types/proto/auto/api/api_offer_model';

import isServicePackage from './isServicePackage';

it('вернет true, если у сервиса есть заполненный package_services', () => {
    expect(isServicePackage(cardMock, TOfferVas.VIP)).toBe(true);
});

it('вернет false, если у сервиса package_services пустой', () => {
    const offer = cloneOfferWithHelpers(cardMock)
        .withServicePrices(cardMock.service_prices.map(service => ({
            ...service,
            package_services: service.service !== TOfferVas.VIP ? service.package_services : [],
        })))
        .value();
    expect(isServicePackage(offer, TOfferVas.VIP)).toBe(false);
});

it('вернет false, если у сервиса не приходит package_services', () => {
    const offer = cloneOfferWithHelpers(cardMock)
        .withServicePrices(cardMock.service_prices.map(service => {
            if (service.service === TOfferVas.VIP) {
                return _.omit(service, [ 'package_services' ]);
            } else {
                return service;
            }
        }))
        .value();
    expect(isServicePackage(offer, TOfferVas.VIP)).toBe(false);
});

import mockdate from 'mockdate';

import type { RegionInfo } from '@vertis/schema-registry/ts-types-snake/auto/api/common_model';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';

import { DEFAULT_FROM_YEAR } from 'auto-core/data/car-tinder/car_tinder';

import getDefaultParameters from './getDefaultParameters';

beforeEach(() => {
    mockdate.set('2022-01-23');
});

it('возвращает дефолтные параметры', () => {
    const testOffer = cloneOfferWithHelpers(offerMock)
        .withPrice(1000000)
        .withYear(2015)
        .withSellerLocation({
            geobase_id: '14',
            region_info: { id: '213', name: 'Москва' } as RegionInfo,
        })
        .value();

    expect(getDefaultParameters(testOffer).year_from).toEqual(2010);
    expect(getDefaultParameters(testOffer).year_to).toEqual(2020);
    expect(getDefaultParameters(testOffer).price_from).toEqual(700000);
    expect(getDefaultParameters(testOffer).price_to).toEqual(1300000);
    expect(getDefaultParameters(testOffer).km_age_to).toEqual(150000);
    expect(getDefaultParameters(testOffer).geo_id).toEqual([ 14 ]);
    expect(getDefaultParameters(testOffer).geo_radius).toEqual(200);
});

it('возвращает диапазон лет в соответствие с коэффициентом и годом выпуска тачки', () => {
    const testOffer = cloneOfferWithHelpers(offerMock).withYear(2015).value();

    expect(getDefaultParameters(testOffer).year_from).toEqual(2010);
    expect(getDefaultParameters(testOffer).year_to).toEqual(2020);
});

it('вернёт минимально возможный год если тачка младше него меньше, чем на 5 лет', () => {
    const testOffer = cloneOfferWithHelpers(offerMock).withYear(DEFAULT_FROM_YEAR + 3).value();

    expect(getDefaultParameters(testOffer).year_from).toEqual(DEFAULT_FROM_YEAR);
});

it('вернёт текущий год если тачка произведена меньше 5 лет назад', () => {
    const currentYear = (new Date()).getFullYear();
    const testOffer = cloneOfferWithHelpers(offerMock).withYear(currentYear - 3).value();

    expect(getDefaultParameters(testOffer).year_to).toEqual(currentYear);
});

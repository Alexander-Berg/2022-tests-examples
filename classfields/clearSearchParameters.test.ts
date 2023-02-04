jest.mock('./getDefaultParameters', () => jest.fn().mockImplementation(() => ({
    year_from: 2012,
    year_to: 2017,
    price_from: 200000,
    price_to: 600000,
})));

import mockdate from 'mockdate';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';

import type { TSearchParameters } from 'auto-core/types/TSearchParameters';

import clearSearchParameters from './clearSearchParameters';

beforeEach(() => {
    mockdate.set('2022-01-23');
});

it('уберёт self_offer_id и дефолтные параметры, а остальное оставит', () => {
    const testParams: TSearchParameters = {
        section: 'used',
        category: 'cars',
        power_from: 1300,
        power_to: 2000,
        year_from: 2012,
        year_to: 2017,
        price_from: 200000,
        price_to: 600000,
        self_offer_id: '123-321',
    };

    expect(clearSearchParameters({ offer: offerMock, searchParameters: testParams })).toEqual({
        section: 'used',
        category: 'cars',
        power_from: 1300,
        power_to: 2000,
    });
});

it('не удалит параметры с дефолтными ключами, но недефолтными значениями', () => {
    const testParams: TSearchParameters = {
        section: 'used',
        category: 'cars',
        power_from: 1300,
        power_to: 2000,
        year_from: 2015,
        year_to: 2016,
        price_from: 100000,
        price_to: 700000,
        self_offer_id: '123-321',
    };

    expect(clearSearchParameters({ offer: offerMock, searchParameters: testParams })).toEqual({
        section: 'used',
        category: 'cars',
        power_from: 1300,
        power_to: 2000,
        year_from: 2015,
        year_to: 2016,
        price_from: 100000,
        price_to: 700000,
    });
});

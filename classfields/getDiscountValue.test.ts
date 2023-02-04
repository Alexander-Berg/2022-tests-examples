import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import { TOfferVas } from 'auto-core/types/proto/auto/api/api_offer_model';

import getDiscountValue from './getDiscountValue';

it('вернет 0, если уже куплен один из пакетов', () => {
    const offer = cloneOfferWithHelpers(offerMock).withActiveVas([ TOfferVas.TURBO ]).value();
    const result = getDiscountValue(offer);
    expect(result).toBe(0);
});

it('вернет 0, если у пакетов нет скидок', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withCustomVas({
            service: TOfferVas.VIP,
            recommendation_priority: 0,
        })
        .withCustomVas({
            service: TOfferVas.TURBO,
            recommendation_priority: 5,
            original_price: undefined,
        })
        .withCustomVas({
            service: TOfferVas.EXPRESS,
            recommendation_priority: 3,
            original_price: undefined,
        })
        .withActiveVas([])
        .value();
    const result = getDiscountValue(offer);

    expect(result).toBe(0);
});

it('если есть хотя бы одна реальная скидка, вернет минимальную', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withCustomVas({
            service: TOfferVas.VIP,
            recommendation_priority: 10,
            original_price: 100,
            price: 30,
        })
        .withCustomVas({
            service: TOfferVas.TURBO,
            recommendation_priority: 5,
            original_price: 100,
            price: 50,
        })
        .withCustomVas({
            service: TOfferVas.EXPRESS,
            recommendation_priority: 3,
            original_price: 100,
            price: 60, // это фейковая скидка, не должен ее учитывать
        })
        .withActiveVas([])
        .value();
    const result = getDiscountValue(offer);

    expect(result).toBe(50);
});

it('если нет реальных скидок, вернет 40', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withCustomVas({
            service: TOfferVas.VIP,
            recommendation_priority: 0,
            original_price: 100,
            price: 30,
        })
        .withCustomVas({
            service: TOfferVas.TURBO,
            recommendation_priority: 5,
            original_price: 100,
            price: 60, // это фейковая скидка, не должен ее учитывать
        })
        .withCustomVas({
            service: TOfferVas.EXPRESS,
            recommendation_priority: 3,
            original_price: 100,
            price: 60, // это фейковая скидка, не должен ее учитывать
        })
        .withActiveVas([])
        .value();
    const result = getDiscountValue(offer);

    expect(result).toBe(40);
});

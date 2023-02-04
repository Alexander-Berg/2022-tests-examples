import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';

import type { TSearchParameters } from 'auto-core/types/TSearchParameters';

import getLinkParamsToSalon from './getLinkParamsToSalon';

it('для частника возвращает пустой объект', () => {
    const result = getLinkParamsToSalon(offerMock);
    expect(result).toEqual({});
});

it('правильно формирует параметры для официального дилера', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withSellerTypeCommercial()
        .withSalon({ is_oficial: true })
        .value();
    const result = getLinkParamsToSalon(offer);

    expect(result).toMatchSnapshot();
});

it('правильно формирует параметры для неофициального дилера', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withSellerTypeCommercial()
        .withSalon({ is_oficial: false })
        .value();
    const result = getLinkParamsToSalon(offer);

    expect(result).toMatchSnapshot();
});

it('добавляет необходимые параметры поиска', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withSellerTypeCommercial()
        .withSalon({ is_oficial: false })
        .value();
    const searchParams: TSearchParameters = {
        has_image: false,
        customs_state_group: 'CLEARED',
        damage_group: 'NOT_BEATEN',
        price_to: 2000000,
    };

    const result = getLinkParamsToSalon(offer, searchParams);

    expect(result).toMatchSnapshot();
});

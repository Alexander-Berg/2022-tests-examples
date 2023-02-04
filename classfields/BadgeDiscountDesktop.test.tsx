import React from 'react';
import { shallow } from 'enzyme';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import Badge from 'auto-core/react/components/common/Badges/Badge/Badge';

import BadgeDiscountDesktop from './BadgeDiscountDesktop';

it('не должен рисовать бейдж "Скидки" для нового авто', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withDiscountOptions({ max_discount: 100500 })
        .withSection('new')
        .value();
    const page = shallow(
        <BadgeDiscountDesktop offer={ offer } color={ Badge.COLOR.BLUE_GRAY_LIGHT_EXTRA }/>,
    );

    expect(page).toBeEmptyRender();
});

it('не должен рисовать бейдж "Скидки" если не указана максимальная скидка', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withDiscountOptions({ })
        .withSection('used')
        .value();
    const page = shallow(
        <BadgeDiscountDesktop offer={ offer } color={ Badge.COLOR.BLUE_GRAY_LIGHT_EXTRA }/>,
    );

    expect(page).toBeEmptyRender();
});

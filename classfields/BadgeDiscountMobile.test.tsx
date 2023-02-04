import React from 'react';
import { shallow } from 'enzyme';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import BadgeDiscountMobile from './BadgeDiscountMobile';

it('не должен рисовать бейдж "Скидки" для нового авто', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withDiscountOptions({ max_discount: 100500 })
        .withSection('new')
        .value();
    const page = shallow(
        <BadgeDiscountMobile offer={ offer }/>,
    );

    expect(page).toBeEmptyRender();
});

it('не должен рисовать бейдж "Скидки" если не указана максимальная скидка', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withDiscountOptions({ })
        .withSection('used')
        .value();
    const page = shallow(
        <BadgeDiscountMobile offer={ offer }/>,
    );

    expect(page).toBeEmptyRender();
});

import React from 'react';
import { shallow } from 'enzyme';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import CardReviewsMobile from './CardReviewsMobile';

it('отрисует блок для объявления бу легковых', () => {
    const tree = shallow(
        <CardReviewsMobile offer={ offerMock }/>,
    );

    expect(tree.isEmptyRender()).toBe(false);
});

it('не отрисует блок для объявления бу не легковых', () => {
    const offer = cloneOfferWithHelpers(offerMock).withCategory('moto').value();
    const tree = shallow(
        <CardReviewsMobile offer={ offer }/>,
    );

    expect(tree.isEmptyRender()).toBe(true);
});

it('не отрисует блок для объявления новых легковых', () => {
    const offer = cloneOfferWithHelpers(offerMock).withSection('new').value();
    const tree = shallow(
        <CardReviewsMobile offer={ offer }/>,
    );

    expect(tree.isEmptyRender()).toBe(true);
});

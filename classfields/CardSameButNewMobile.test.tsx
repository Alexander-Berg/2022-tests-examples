import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import CardSameButNewMobile from './CardSameButNewMobile';

it('отрисует блок для бу легковых', () => {
    const tree = shallow(
        <CardSameButNewMobile offer={ offerMock }/>,
    );

    expect(tree.isEmptyRender()).toBe(false);
});

it('не отрисует блок, если тачка новая', () => {
    const offer = cloneOfferWithHelpers(offerMock).withSection('new').value();

    const tree = shallow(
        <CardSameButNewMobile offer={ offer }/>,
    );

    expect(tree.isEmptyRender()).toBe(true);
});

it('не отрисует блок, если тачка не легковая', () => {
    const offer = cloneOfferWithHelpers(offerMock).withCategory('moto').value();

    const tree = shallow(
        <CardSameButNewMobile offer={ offer }/>,
    );

    expect(tree.isEmptyRender()).toBe(true);
});

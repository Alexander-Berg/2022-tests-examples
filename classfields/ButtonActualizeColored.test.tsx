import { shallow } from 'enzyme';
import { shallowToJson } from 'enzyme-to-json';
import MockDate from 'mockdate';
import React from 'react';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import ButtonActualizeColored from './ButtonActualizeColored';

beforeEach(() => {
    MockDate.set('2020-04-20');
});

afterEach(() => {
    MockDate.reset();
});

it('должен нарисовать "да, продаю", если дней больше 3', () => {
    const offer = cloneOfferWithHelpers({}).withBaseDate('2020-04-17');
    const wrapper = shallow(
        <ButtonActualizeColored offer={ offer.value() }/>,
    );

    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('должен нарисовать "актуально", если дней меньше 3', () => {
    const offer = cloneOfferWithHelpers({}).withBaseDate('2020-04-20');
    const wrapper = shallow(
        <ButtonActualizeColored offer={ offer.value() }/>,
    );

    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

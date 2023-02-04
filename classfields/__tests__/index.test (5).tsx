import React from 'react';
import { mount } from 'enzyme';
import MockDate from 'mockdate';

import * as redux from 'react-redux';

import { Microdata } from '../index';
import { Organization } from '../components/Organization';

import {
    MOCK_STORE_NEWBUILDING,
    MOCK_STORE_NEWBUILDING_ZERO_OFFERS,
    MOCK_STORE_OFFER_CARD,
    MOCK_STORE_OFFER_CARD_INACTIVE,
    MOCK_SEARCH_STORE,
    MOCK_STORE_WITHOUT_WEBSITE_RATING,
    MOCK_USUAL_STORE,
    MOCK_MOSKVA,
    MOCK_MOSKVA_I_MO,
    MOCK_NOT_MOSKVA_OR_MOSKVA_I_MO,
} from './mock';

beforeEach(() => {
    MockDate.set('2021-12-01T21:00');
});

afterEach(() => {
    MockDate.reset();
});

it('страница Newbuilding', () => {
    jest.spyOn(redux, 'useSelector').mockImplementation(() => MOCK_STORE_NEWBUILDING);
    const wrapper = mount(<Microdata />);

    expect(wrapper).toMatchSnapshot();
});

it('страница Newbuilding 0 офферов', () => {
    jest.spyOn(redux, 'useSelector').mockImplementation(() => MOCK_STORE_NEWBUILDING_ZERO_OFFERS);
    const wrapper = mount(<Microdata />);

    expect(wrapper).toMatchSnapshot();
});

it('страница Offer', () => {
    jest.spyOn(redux, 'useSelector').mockImplementation(() => MOCK_STORE_OFFER_CARD);
    const wrapper = mount(<Microdata />);

    expect(wrapper).toMatchSnapshot();
});

it('страница Offer. Неактивный оффер', () => {
    jest.spyOn(redux, 'useSelector').mockImplementation(() => MOCK_STORE_OFFER_CARD_INACTIVE);
    const wrapper = mount(<Microdata />);

    expect(wrapper).toMatchSnapshot();
});

it('страница Search', () => {
    jest.spyOn(redux, 'useSelector').mockImplementation(() => MOCK_SEARCH_STORE);
    const wrapper = mount(<Microdata />);

    expect(wrapper).toMatchSnapshot();
});

it('страница для desktop и mobile должна содержать microData', () => {
    jest.spyOn(redux, 'useSelector').mockImplementation(() => MOCK_SEARCH_STORE);
    const wrapper = mount(<Microdata />).find(Organization);

    expect(wrapper).toHaveLength(1);
});

it('страница для AMP не должна содержать microData', () => {
    jest.spyOn(redux, 'useSelector').mockImplementation(() => MOCK_SEARCH_STORE);
    const wrapper = mount(<Microdata isAmp={true} />).find(Organization);

    expect(wrapper).toHaveLength(0);
});

it('страница с исключением websiteRating', () => {
    jest.spyOn(redux, 'useSelector').mockImplementation(() => MOCK_STORE_WITHOUT_WEBSITE_RATING);
    const wrapper = mount(<Microdata />);

    expect(wrapper).toMatchSnapshot();
});

it('обычная страница без доп условий', () => {
    jest.spyOn(redux, 'useSelector').mockImplementation(() => MOCK_USUAL_STORE);
    const wrapper = mount(<Microdata />);

    expect(wrapper).toMatchSnapshot();
});

it('Москва проверка адреса', () => {
    jest.spyOn(redux, 'useSelector').mockImplementation(() => MOCK_MOSKVA);
    const wrapper = mount(<Microdata />);

    expect(wrapper).toMatchSnapshot();
});

it('Москва и МО проверка адреса', () => {
    jest.spyOn(redux, 'useSelector').mockImplementation(() => MOCK_MOSKVA_I_MO);
    const wrapper = mount(<Microdata />);

    expect(wrapper).toMatchSnapshot();
});

it('Не Москва или Москва и МО проверка адреса', () => {
    jest.spyOn(redux, 'useSelector').mockImplementation(() => MOCK_NOT_MOSKVA_OR_MOSKVA_I_MO);
    const wrapper = mount(<Microdata />);

    expect(wrapper).toMatchSnapshot();
});

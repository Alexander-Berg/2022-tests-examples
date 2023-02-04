/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { mount } from 'enzyme';

import AmpRequiredTags from './AmpRequiredTags';

it('должен отреднерить правильный html с обязательными тегами', () => {
    const wrapper = mount(<AmpRequiredTags/>);

    expect(wrapper.html()).toMatchSnapshot();
});

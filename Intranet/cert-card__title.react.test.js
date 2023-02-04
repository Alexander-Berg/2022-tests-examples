import React from 'react';
import Title from 'b:cert-card e:title';
import {mount} from 'enzyme';

it('should render passed text', () => {
    const wrapper = mount(<Title text="haha" />);

    expect(wrapper).toMatchSnapshot();
    wrapper.unmount();
});

it('should render a hyphen if passed text is empty', () => {
    const wrapper = mount(<Title text="" />);

    expect(wrapper).toMatchSnapshot();
    wrapper.unmount();
});

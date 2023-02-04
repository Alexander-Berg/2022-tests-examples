import React from 'react';
import Field from 'b:cert-card e:field-val m:type=text';
import {mount} from 'enzyme';

it('should contain a proper content', () => {
    const wrapper = mount(
        <Field
            type="text"
            data="foo"
        />
    );

    expect(wrapper).toMatchSnapshot();
    wrapper.unmount();
});

it('should print a hyphen in case of empty text', () => {
    const wrapper = mount(
        <Field
            type="text"
            data=""
        />
    );

    expect(wrapper).toMatchSnapshot();
    wrapper.unmount();
});

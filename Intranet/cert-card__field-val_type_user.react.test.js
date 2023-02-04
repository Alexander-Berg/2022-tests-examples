import React from 'react';
import Field from 'b:cert-card e:field-val m:type=user';
import {mount} from 'enzyme';

it('should contain a proper content', () => {
    const wrapper = mount(
        <Field
            type="user"
            data="remnev"
        />
    );

    expect(wrapper).toMatchSnapshot();
    wrapper.unmount();
});

it('should print a hyphen in case of empty user', () => {
    const wrapper = mount(
        <Field
            type="user"
            data=""
        />
    );

    expect(wrapper).toMatchSnapshot();
    wrapper.unmount();
});

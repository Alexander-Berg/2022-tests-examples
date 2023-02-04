import React from 'react';
import Field from 'b:cert-card e:field-val m:type=date';
import {mount} from 'enzyme';

it('should contain a proper content', () => {
    const wrapper = mount(
        <Field
            type="date"
            data="2018-06-28T19:37:53+03:00"
        />
    );

    expect(wrapper).toMatchSnapshot();
    wrapper.unmount();
});

it('should print a hyphen in case of invalid date', () => {
    const wrapper = mount(
        <Field
            type="date"
            data={null}
        />
    );

    expect(wrapper).toMatchSnapshot();
    wrapper.unmount();
});

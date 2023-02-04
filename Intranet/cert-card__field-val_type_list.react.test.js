import React from 'react';
import Field from 'b:cert-card e:field-val m:type=list';
import {mount} from 'enzyme';

it('should contain a proper content', () => {
    const wrapper = mount(
        <Field
            type="list"
            data={['a', 'b']}
        />
    );

    expect(wrapper).toMatchSnapshot();
    wrapper.unmount();
});

it('should print a hyphen in case of empty list', () => {
    const wrapper = mount(
        <Field
            type="list"
            data={[]}
        />
    );

    expect(wrapper).toMatchSnapshot();
    wrapper.unmount();
});

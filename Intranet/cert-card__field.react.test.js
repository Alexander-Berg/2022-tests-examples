import React from 'react';
import Field from 'b:cert-card e:field';
import {mount} from 'enzyme';

it('should contain a proper content', () => {
    const wrapper = mount(
        <Field
            type="text"
            fieldKey="status"
            fieldVal="haha :)"
        />
    );

    expect(wrapper).toMatchSnapshot();
    wrapper.unmount();
});

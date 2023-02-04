import React from 'react';
import FieldVal from 'b:cert-card e:field-val';
import {mount} from 'enzyme';

it('should be a span', () => {
    const wrapper = mount(<FieldVal />);

    expect(wrapper).toMatchSnapshot();
    wrapper.unmount();
});

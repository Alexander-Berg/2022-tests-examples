import React from 'react';
import FieldKey from 'b:cert-card e:field-key';
import {mount} from 'enzyme';

it('should contain a proper content', () => {
    const wrapper = mount(<FieldKey slug="status" />);

    expect(wrapper).toMatchSnapshot();
    wrapper.unmount();
});

import React from 'react';
import { shallow } from 'enzyme';

import { Organization } from '../';

test('render microdata component', () => {
    const wrapper = shallow(<Organization />);

    expect(wrapper).toMatchSnapshot();
});

test('render microdata component without address', () => {
    const wrapper = shallow(<Organization excludeAddress />);

    expect(wrapper).toMatchSnapshot();
});

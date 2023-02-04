import React from 'react';
import { shallow } from 'enzyme';

import { Person } from '../';

describe('Render Person test', () => {
    test('Render Person if name exists', () => {
        const wrapper = shallow(<Person name={'OLEG'} />);

        expect(wrapper).toMatchSnapshot();
    });

    test('Do not Render Person if name not provided', () => {
        const wrapper = shallow(<Person />);

        expect(wrapper).toMatchSnapshot();
    });
});

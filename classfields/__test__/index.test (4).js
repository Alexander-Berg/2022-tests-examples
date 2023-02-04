import React from 'react';
import { shallow } from 'enzyme';

import { Rating } from '../';

describe('Render Rating test', () => {
    test('Render Person if name exists', () => {
        const wrapper = shallow(<Rating ratingValue={5} />);

        expect(wrapper).toMatchSnapshot();
    });

    test('Do not Render Rating if ratingValue not provided', () => {
        const wrapper = shallow(<Rating />);

        expect(wrapper).toMatchSnapshot();
    });
});

import React from 'react';
import { shallow } from 'enzyme';

import { Review } from '../';

describe('Render Microdata Review test', () => {
    test('should render Review if reviewRating and Author Provided', () => {
        const wrapper = shallow(<Review reviewRating={{ ratingValue: 5 }} author={{ name: 'Tester' }} />);

        expect(wrapper).toMatchSnapshot();
    });

    test('Do not Render Rating if reviewRating and Author are not provided', () => {
        const wrapper1 = shallow(<Review reviewRating={{ ratingValue: 5 }} />);

        expect(wrapper1).toMatchSnapshot();

        const wrapper2 = shallow(<Review author={{ name: 'Tester' }} />);

        expect(wrapper2).toMatchSnapshot();
    });
});

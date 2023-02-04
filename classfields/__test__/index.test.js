import React from 'react';
import { shallow } from 'enzyme';

import { AggregateOffer } from '../';

describe('AggregateOffer test', () => {
    test('Render Aggregate Offer with all props', () => {
        const wrapper = shallow(
            <AggregateOffer
                highPrice={1300}
                lowPrice={600}
                offerCount={20}
                priceCurrency={'RUR'}
                availability={'https://schema.org/InStock'}
            />);

        expect(wrapper).toMatchSnapshot();
    });

    test('Render Aggregate Offer with only required props', () => {
        const wrapper = shallow(
            <AggregateOffer
                priceCurrency={'RUR'}
            />);

        expect(wrapper).toMatchSnapshot();
    });
});

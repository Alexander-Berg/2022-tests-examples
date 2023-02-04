import React from 'react';
import { shallow } from 'enzyme';

import { TaxiBannerComponent } from '../';

describe('TaxiBanner', () => {
    it('renders component when user is in spb', () => {
        const wrapper = shallow(
            <TaxiBannerComponent
                bannerIndex={0}
                slideIndex={0}
                geo={{ rgid: 741965 }}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('renders component when user is in penza', () => {
        const wrapper = shallow(
            <TaxiBannerComponent
                bannerIndex={0}
                slideIndex={0}
                geo={{ rgid: 539870 }}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('renders component when offer is in spb', () => {
        const wrapper = shallow(
            <TaxiBannerComponent
                bannerIndex={0}
                slideIndex={0}
                offerLocation={{ rgid: 741965 }}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('renders component when offer is in penza', () => {
        const wrapper = shallow(
            <TaxiBannerComponent
                bannerIndex={0}
                slideIndex={0}
                offerLocation={{ rgid: 539870 }}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('renders component when user is in a city that is in MO', () => {
        const wrapper = shallow(
            <TaxiBannerComponent
                bannerIndex={0}
                slideIndex={0}
                geo={{ rgid: '***', parents: [ { rgid: 741964 } ] }}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('renders component when offer is in MO', () => {
        const wrapper = shallow(
            <TaxiBannerComponent
                bannerIndex={0}
                slideIndex={0}
                offerLocation={{ rgid: '***', subjectFederationId: 1 }}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('renders nothing when offer is not in listed region', () => {
        const wrapper = shallow(
            <TaxiBannerComponent
                bannerIndex={0}
                slideIndex={0}
                offerLocation={{ rgid: 666 }}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('renders nothing when user is not in listed region', () => {
        const wrapper = shallow(
            <TaxiBannerComponent
                bannerIndex={0}
                slideIndex={0}
                geo={{ rgid: 666 }}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });
});

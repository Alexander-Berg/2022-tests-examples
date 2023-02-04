import React from 'react';
import { mount } from 'enzyme';

import Spinner from 'vertis-react/components/Spinner';
import { PageParanja } from '../';

describe('PageParanja', () => {
    it('shows spinner if isOfferFormSubmitting is true', () => {
        const wrapper = mount(
            <PageParanja
                isOfferFormSubmitting
                isPageBlocked={false}
                isBlockable
            />
        );

        expect(wrapper.find(Spinner).length).toBe(1);
    });

    it('does not show spinner if isOfferFormSubmitting is undefined', () => {
        const wrapper = mount(
            <PageParanja
                isOfferFormSubmitting={undefined}
                isPageBlocked
                isBlockable
            />
        );

        expect(wrapper.find(Spinner).length).toBe(0);
    });
});

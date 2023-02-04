import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';

import listingPageMock from 'auto-core/react/dataDomain/mag/listingPage';

import PageListingTitle from './PageListingTitle';

it('строит заголовок h1 тегом', () => {
    const wrapper = shallow(
        <PageListingTitle
            listingPage={ listingPageMock.value() }
            partnerName="tochka"
        />,
        { context: contextMock },
    );

    expect(wrapper.find('.PageListingTitle__text').prop('tag')).toBe('h1');
});

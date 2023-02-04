import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';

import { CategoryItems, SectionItems } from 'auto-core/types/TSearchParameters';

import Link from 'www-mag/react/components/common/Link/Link';

import OffersCarouselFooter from './OffersCarouselFooter';

it('рендерит футер с правильным урлом ссылки', () => {
    const wrapper = shallow(
        <OffersCarouselFooter
            firstOffer={ offerMock }
            searchParams={{ section: SectionItems.ALL, category: CategoryItems.CARS }}
        />,
        { context: contextMock },
    );

    expect(wrapper.find(Link).prop('href')).toBe('link/listing/?section=all&category=cars&from=mag.ad-carousel');
});

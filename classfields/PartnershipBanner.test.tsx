/**
 * @jest-environment node
 */
import React from 'react';
import { shallow } from 'enzyme';

import { Link } from 'core/client/components/Link/Link';
import { Image } from 'core/client/components/Image/Image';
import { IMAGE_MOCK_1 } from 'core/mocks/image.mock';
import { ATTRIBUTES_FOR_EXTERNAL_LINKS } from 'core/client/constants/externalUrls';

import { PartnershipBanner } from './PartnershipBanner';

describe('правильно выставляет атрибуты', () => {
    const wrapper = shallow(
        <PartnershipBanner
            partnershipImage={ IMAGE_MOCK_1 }
            partnershipLink="https://example.com"
            partnershipTitle="ОАО «Константин»"
        />
    );

    const link = wrapper.find(Link);

    it('для внешних ссылок и href', () => {
        expect(link.prop('attributes')).toEqual(ATTRIBUTES_FOR_EXTERNAL_LINKS);

        expect(link.prop('href')).toBe('https://example.com');
    });

    it('alt, sizes, title у картинки', () => {
        expect(link.find(Image).prop('imageAttributes')).toEqual({
            alt: 'Логотип ОАО «Константин»',
            sizes: '258px',
            title: 'ОАО «Константин»',
        });
    });
});

/**
 * @jest-environment node
 */
import React from 'react';
import { shallow } from 'enzyme';

import { IMAGE_MOCK_1 } from 'core/mocks/image.mock';
import { getImageSrcSet } from 'core/client/lib/getImageSrcSet';

import { HelmetPreloadImage } from './HelmetPreloadImage';

describe('выставляет атрибуты для предзагрузки изображения', () => {
    const wrapper = shallow(
        <HelmetPreloadImage imageSizes="100vw" sizes={ IMAGE_MOCK_1.sizes }/>
    );

    const link = wrapper.find('link');

    it('rel', () => {
        expect(link.prop('rel')).toBe('preload');
    });

    it('as', () => {
        expect(link.prop('as')).toBe('image');
    });

    it('href', () => {
        expect(link.prop('href')).toBe(IMAGE_MOCK_1.sizes.orig.path);
    });

    it('imagesrcset', () => {
        expect(link.prop('imagesrcset')).toBe(getImageSrcSet(IMAGE_MOCK_1.sizes));
    });

    it('sizes', () => {
        expect(link.prop('sizes')).toBe('100vw');
    });
});

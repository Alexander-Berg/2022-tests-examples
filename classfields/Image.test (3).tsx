/**
 * @jest-environment node
 */
import React from 'react';
import { shallow } from 'enzyme';

import { IMAGE_MOCK_1 } from 'core/mocks/image.mock';

import { Image } from './Image';

describe('рендерится с обязательными атрибутами', () => {
    const wrapper = shallow(
        <Image
            sizes={ IMAGE_MOCK_1.sizes }
        />
    );

    const imgComponent = wrapper.find('AspectRatio.availableSpace').dive().find('img');

    it('loading', () => {
        expect(imgComponent.prop('loading')).toBe('lazy');
    });

    it('srcSet', () => {
        expect(imgComponent.prop('srcSet')).toBe(' 338w, 439w, 571w, 650w, 845w, 1098w, 1252w, 1428w, 1600w, 1920w, 3840w');
    });
});

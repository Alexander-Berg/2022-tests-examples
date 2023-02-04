/**
 * @jest-environment node
 */
import React from 'react';
import { shallow } from 'enzyme';

import { HelmetDescription } from './HelmetDescription';

describe('рендерит meta теги', () => {
    const description = 'Моника Геллер Рейчел Грин';

    const descriptionWithCheckmark = `✅ ${ description }`;

    const wrapper = shallow(
        <HelmetDescription description={ description }/>
    );

    it('description', () => {
        expect(wrapper.find('meta[name="description"]').prop('content')).toBe(descriptionWithCheckmark);
    });

    it('og:description', () => {
        expect(wrapper.find('meta[property="og:description"]').prop('content')).toBe(descriptionWithCheckmark);
    });
});

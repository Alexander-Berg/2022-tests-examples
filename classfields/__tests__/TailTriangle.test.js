import React from 'react';
import { mount } from 'enzyme';

import { TailTriangle } from '../NewTail';

describe('TailTriangle', () => {
    it('should match snapshot when width > height', () => {
        const wrapper = mount(
            <TailTriangle
                width={16}
                height={8}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('should match snapshot when width < height', () => {
        const wrapper = mount(
            <TailTriangle
                width={6}
                height={18}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });
});

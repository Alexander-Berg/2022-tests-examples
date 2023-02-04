import React from 'react';
import { mount } from 'view/libs/test-helpers';
import { WithSeparator } from '..';

describe('WithSeparator component', () => {
    // unsupported by enzyme yet https://github.com/airbnb/enzyme/issues/1799
    it.skip('should render separator', async() => {
        const { dom, time } = mount(
            <WithSeparator separator=', '>
                <span>1</span>
                <span>2</span>
                <span>3</span>
            </WithSeparator>
        );

        await time.tick();
        expect(dom.text()).toBe('1, 2, 3');
    });

    it('should not render separator for only child', async() => {
        const { dom, time } = mount(
            <WithSeparator separator=', '>
                <span key={1}>1</span>
            </WithSeparator>
        );

        await time.tick();
        expect(dom.text()).toBe('1');
    });
});

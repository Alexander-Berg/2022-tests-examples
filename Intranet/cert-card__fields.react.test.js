import React from 'react';
import Fields from 'b:cert-card e:fields';
import {mount} from 'enzyme';

it('should contain a proper content', () => {
    const wrapper = mount(
        <Fields
            fields={[
                {
                    slug: 'cn',
                    type: 'text',
                    data: 'common_name'
                }
            ]}
        />
    );

    expect(wrapper).toMatchSnapshot();
    wrapper.unmount();
});

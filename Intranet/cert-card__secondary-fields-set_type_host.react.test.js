import React from 'react';
import SecondaryFieldsSet from 'b:cert-card e:secondary-fields-set m:type=host';
import {mount} from 'enzyme';

it('should contain a proper content', () => {
    const wrapper = mount(
        <SecondaryFieldsSet
            type="host"
            cert={{
                common_name: 'remnev.ru',
                id: '100500',
                requester: 'remnev',
                added: '2018-06-28T19:37:53+03:00',
                issued: '2018-06-28T19:37:53+03:00',
                used_template: 'template',
                tags: [{name: 'a', is_active: true}, {name: 'b', is_active: false}],
                priv_key_deleted_at: '2018-06-28T19:37:53+03:00',
                abcServiceId: '100500',
                available_actions: []
            }}
            onAbcServiceTooltipOutsideClick={jest.fn()}
        />
    );

    expect(wrapper).toMatchSnapshot();
    wrapper.unmount();
});

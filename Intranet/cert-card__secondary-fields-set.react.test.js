import React from 'react';
import SecondaryFieldsSet from 'b:cert-card e:secondary-fields-set';
import {mount} from 'enzyme';

it('should contain a proper content', () => {
    const wrapper = mount(
        <SecondaryFieldsSet
            cert={{
                common_name: 'remnev.ru',
                id: '100500',
                requester: {username: 'remnev'},
                added: '2018-06-28T19:37:53+03:00',
                issued: '2018-06-28T19:37:53+03:00',
                used_template: 'template',
                tags: [{name: 'a', is_active: true}, {name: 'b', is_active: false}],
                priv_key_deleted_at: '2018-06-28T19:37:53+03:00',
                available_actions: []
            }}
        />
    );

    expect(wrapper).toMatchSnapshot();
    wrapper.unmount();
});

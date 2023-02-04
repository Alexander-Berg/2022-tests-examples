import React from 'react';
import SecondaryFieldsSet from 'b:cert-card e:secondary-fields-set m:type=pc';
import {mount} from 'enzyme';

it('should contain a proper content', () => {
    const wrapper = mount(
        <SecondaryFieldsSet
            type="pc"
            cert={{
                common_name: 'remnev.ru',
                id: '100500',
                requester: {username: 'remnev'},
                added: '2018-06-28T19:37:53+03:00',
                issued: '2018-06-28T19:37:53+03:00',
                used_template: 'template',
                tags: [{name: 'a', is_active: true}, {name: 'b', is_active: false}],
                priv_key_deleted_at: '2018-06-28T19:37:53+03:00',
                user: 'remnev',
                pc_hostname: 'pc-hostname',
                pc_os: 'pc-os',
                pc_serial_number: 'pc-serial-number',
                pc_mac: 'pc-mac',
                pc_inum: 'pc-inum',
                available_actions: []
            }}
        />
    );

    expect(wrapper).toMatchSnapshot();
    wrapper.unmount();
});

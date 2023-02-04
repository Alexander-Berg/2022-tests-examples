import React from 'react';
import SecondaryFieldsSet from 'b:cert-card e:secondary-fields-set m:type=host-certum-prod';
import {mount} from 'enzyme';

it('should contain a proper content', () => {
    const wrapper = mount(
        <SecondaryFieldsSet
            type="host-certum-prod"
            cert={{
                common_name: 'remnev.ru',
                id: '100500',
                requester: 'remnev',
                added: '2018-06-28T19:37:53+03:00',
                issued: '2018-06-28T19:37:53+03:00',
                used_template: 'template',
                tags: [{name: 'a', is_active: true}, {name: 'b', is_active: false}],
                priv_key_deleted_at: '2018-06-28T19:37:53+03:00',
                request_id: 'request-id',
                st_issue_key: 'CERTOR-111',
                available_actions: []
            }}
            onAbcServiceTooltipOutsideClick={jest.fn()}
        />
    );

    expect(wrapper).toMatchSnapshot();
    wrapper.unmount();
});

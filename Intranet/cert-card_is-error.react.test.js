import React from 'react';
import CertCard from 'b:cert-card m:is-error';
import {mount} from 'enzyme';

it('should contain a proper content', () => {
    const wrapper = mount(
        <CertCard
            is-error
            fetchError={new Error('Error')}
            onCloseClick={jest.fn()}
            onRetryClick={jest.fn()}
            cert={{
                serial_number: 'serial-number',
                common_name: 'common name',
                ca_name: 'ca-name',
                type_human: {ru: 'type'},
                status_human: {ru: 'status'},
                device_platform: 'platform',
                end_date: '2018-06-28T19:37:53+03:00'
            }}
        />
    );

    expect(wrapper).toMatchSnapshot();
    wrapper.unmount();
});

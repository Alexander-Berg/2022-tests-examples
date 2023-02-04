import React from 'react';
import Field from 'b:cert-card e:field-val m:type=tags';
import {mount} from 'enzyme';

const tags = [{
    name: 'Office.8021X.Search',
    is_active: true,
    source: 'filters'
}, {
    name: 'Office.8021X.Staff',
    is_active: true,
    source: 'filters'
}, {
    name: 'Office.WiFi.Yandex',
    is_active: true,
    source: 'filters'
}, {
    name: 'Office.WiFi.PDAS',
    is_active: false,
    source: 'cert_type'
}, {
    name: 'Office.VPN',
    is_active: false,
    source: 'cert_type'
}];

it('should contain a proper content (filter not active tags)', () => {
    const wrapper = mount(
        <Field
            type="tags"
            data={tags}
        />
    );

    expect(wrapper).toMatchSnapshot();
    wrapper.unmount();
});

it('should print a hyphen in case of empty list', () => {
    const wrapper = mount(
        <Field
            type="tags"
            data={[]}
        />
    );

    expect(wrapper).toMatchSnapshot();
    wrapper.unmount();
});

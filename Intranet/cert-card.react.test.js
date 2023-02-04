import React from 'react';
import CertCard from 'b:cert-card';
import {mount} from 'enzyme';

it('should contain a proper content', () => {
    const wrapper = mount(
        <CertCard
            cert={{
                id: 11,
                available_actions: [
                    {
                        id: 'download',
                        name: {ru: 'Скачать'}
                    }
                ],
                serial_number: 'serial-number',
                common_name: 'common name',
                ca_name: 'ca-name',
                type_human: {ru: 'type'},
                status_human: {ru: 'status'},
                device_platform: 'platform',
                end_date: '2018-06-28T19:37:53+03:00',
                priv_key_deleted_at: '2018-06-28T19:37:53+03:00',
                added: '2018-06-28T19:37:53+03:00',
                issued: '2018-06-28T19:37:53+03:00',
                requester: {username: 'remnev'},
                tags: [{name: 'a', is_active: true}, {name: 'b', is_active: false}],
                helpdesk_ticket: 'TEST-123'
            }}
            actionError={{
                id: '',
                text: ''
            }}
            isActionInProgress={false}
            onTooltipOutsideClick={jest.fn()}
            onActionClick={jest.fn()}
            onCloseClick={jest.fn()}
            onAbcServiceTooltipOutsideClick={jest.fn()}
            isAdditionalFieldsConfigLoading={false}
            onReissueButtonClick={jest.fn()}
        />
    );

    expect(wrapper).toMatchSnapshot();
    wrapper.unmount();
});

describe('secondary fields set for:', () => {
    test('Certum Production', () => {
        const wrapper = mount(
            <CertCard
                cert={{
                    id: 11,
                    available_actions: [
                        {
                            id: 'download',
                            name: {ru: 'Скачать'}
                        }
                    ],
                    serial_number: 'serial-number',
                    common_name: 'common name',
                    ca_name: 'CertumProductionCA',
                    type: 'host',
                    type_human: {ru: 'type'},
                    status_human: {ru: 'status'},
                    device_platform: 'platform',
                    end_date: '2018-06-28T19:37:53+03:00',
                    priv_key_deleted_at: '2018-06-28T19:37:53+03:00',
                    added: '2018-06-28T19:37:53+03:00',
                    issued: '2018-06-28T19:37:53+03:00',
                    requester: {username: 'remnev'},
                    tags: [{name: 'a', is_active: true}, {name: 'b', is_active: false}],
                    helpdesk_ticket: 'TEST-12'
                }}
                actionError={{
                    id: '',
                    text: ''
                }}
                isActionInProgress={false}
                onTooltipOutsideClick={jest.fn()}
                onActionClick={jest.fn()}
                onCloseClick={jest.fn()}
                onAbcServiceTooltipOutsideClick={jest.fn()}
                isAdditionalFieldsConfigLoading={false}
                onReissueButtonClick={jest.fn()}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    test('PC', () => {
        const wrapper = mount(
            <CertCard
                cert={{
                    id: 11,
                    available_actions: [
                        {
                            id: 'download',
                            name: {ru: 'Скачать'}
                        }
                    ],
                    serial_number: 'serial-number',
                    common_name: 'common name',
                    ca_name: 'ca-name',
                    type: 'pc',
                    type_human: {ru: 'type'},
                    status_human: {ru: 'status'},
                    device_platform: 'platform',
                    end_date: '2018-06-28T19:37:53+03:00',
                    priv_key_deleted_at: '2018-06-28T19:37:53+03:00',
                    added: '2018-06-28T19:37:53+03:00',
                    issued: '2018-06-28T19:37:53+03:00',
                    user: {username: 'remnev'},
                    requester: {username: 'remnev'},
                    tags: [{name: 'a', is_active: true}, {name: 'b', is_active: false}],
                    helpdesk_ticket: 'TEST_88'
                }}
                actionError={{
                    id: '',
                    text: ''
                }}
                isActionInProgress={false}
                onTooltipOutsideClick={jest.fn()}
                onActionClick={jest.fn()}
                onCloseClick={jest.fn()}
                isAdditionalFieldsConfigLoading={false}
                onReissueButtonClick={jest.fn()}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    test('Linux PC', () => {
        const wrapper = mount(
            <CertCard
                cert={{
                    id: 11,
                    available_actions: [
                        {
                            id: 'download',
                            name: {ru: 'Скачать'}
                        }
                    ],
                    serial_number: 'serial-number',
                    common_name: 'common name',
                    ca_name: 'ca-name',
                    type: 'linux-pc',
                    type_human: {ru: 'type'},
                    status_human: {ru: 'status'},
                    device_platform: 'platform',
                    end_date: '2018-06-28T19:37:53+03:00',
                    priv_key_deleted_at: '2018-06-28T19:37:53+03:00',
                    added: '2018-06-28T19:37:53+03:00',
                    issued: '2018-06-28T19:37:53+03:00',
                    user: {username: 'remnev'},
                    requester: {username: 'remnev'},
                    tags: [{name: 'a', is_active: true}, {name: 'b', is_active: false}]
                }}
                actionError={{
                    id: '',
                    text: ''
                }}
                isActionInProgress={false}
                onTooltipOutsideClick={jest.fn()}
                onActionClick={jest.fn()}
                onCloseClick={jest.fn()}
                isAdditionalFieldsConfigLoading={false}
                onReissueButtonClick={jest.fn()}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});


import React from 'react';
import Field from 'b:cert-card e:field-val m:type=abc-service';
import {mount} from 'enzyme';

it('should contain a proper content', () => {
    const wrapper = mount(
        <Field
            type="abc-service"
            data={{
                cert: {
                    available_actions: ['update']
                },
                onAbcServiceChange: jest.fn(),
                onAbcServiceTooltipOutsideClick: jest.fn(),
                abcServiceUpdateError: null,
                abcServiceId: ''
            }}
        />
    );

    expect(wrapper).toMatchSnapshot();
    wrapper.unmount();
});

it('should pass value to the abc-service control', () => {
    const wrapper = mount(
        <Field
            type="abc-service"
            data={{
                cert: {
                    available_actions: ['update']
                },
                onAbcServiceChange: jest.fn(),
                onAbcServiceTooltipOutsideClick: jest.fn(),
                abcServiceUpdateError: null,
                abcServiceId: '100500'
            }}
        />
    );

    expect(wrapper).toMatchSnapshot();
    wrapper.unmount();
});

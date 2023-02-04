import React from 'react';
import Controls from 'b:cert-card e:controls';
import {mount} from 'enzyme';

it('should contain a proper content', () => {
    const wrapper = mount(
        <Controls
            cert={{
                id: 11,
                available_actions: [
                    {id: 'download'}
                ]
            }}
            onCloseClick={jest.fn()}
        />
    );

    expect(wrapper).toMatchSnapshot();
    wrapper.unmount();
});

it('should not draw the download button if there is no relevant available action', () => {
    const wrapper = mount(
        <Controls
            cert={{
                id: 11,
                available_actions: []
            }}
            onCloseClick={jest.fn()}
        />
    );

    expect(wrapper).toMatchSnapshot();
    wrapper.unmount();
});

it('should return correct url', () => {
    const execCommand = document.execCommand;

    document.execCommand = jest.fn();

    const wrapper = mount(
        <Controls
            cert={{
                id: 11,
                available_actions: [],
                serial_number: '641F6C8800010024F041'
            }}
            onCloseClick={jest.fn()}
        />
    );

    wrapper.find('.copy-link .button2').simulate('click');

    const copyLinkInput = document.querySelector('.copy-link__popup').getElementsByClassName('textinput__control')[0];

    expect(copyLinkInput.value).toEqual('http://localhost/certificates/11?serial_number=641F6C8800010024F041');

    document.execCommand = execCommand;
    wrapper.unmount();
});

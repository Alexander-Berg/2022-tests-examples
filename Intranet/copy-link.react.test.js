import React from 'react';
import CopyLink from 'b:copy-link';
import {mount} from 'enzyme';
import wait from '@crt/wait';

document.execCommand = jest.fn();

const URL = 'https://127.0.0.1';
const getURL = () => URL;

describe('copy-link', () => {
    it('should show copy link popup and copy url to clipboard', () => {
        const wrapper = mount(
            <CopyLink
                getURL={getURL}
            />
        );

        const copyLinkPopup = document.querySelector('.copy-link__popup');

        expect(copyLinkPopup.classList.contains('popup2_visible_yes')).toBe(false);

        wrapper.find('.button2').simulate('click');

        expect(copyLinkPopup.classList.contains('popup2_visible_yes')).toBe(true);

        return wait()
            .then(() => {
                expect(document.execCommand.mock.calls.length).toEqual(1);
            })
            .finally(() => {
                wrapper.unmount();
            });
    });

    it('should hide copy link popup on outside click', () => {
        const wrapper = mount(
            <CopyLink
                getURL={getURL}
            />
        );

        const copyLinkPopup = document.querySelector('.copy-link__popup');

        expect(copyLinkPopup.classList.contains('popup2_visible_yes')).toBe(false);

        wrapper.find('.button2').simulate('click');

        expect(copyLinkPopup.classList.contains('popup2_visible_yes')).toBe(true);

        wrapper.instance().handlePopupOutsideClick();

        return wait()
            .then(() => {
                expect(copyLinkPopup.classList.contains('popup2_visible_yes')).toBe(false);
            })
            .finally(() => {
                wrapper.unmount();
            });
    });
});

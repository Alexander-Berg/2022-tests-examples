/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { shallow } from 'enzyme';

import type { Props } from './VasFormUserNotification';
import VasFormUserNotification, { AUTO_HIDE_TIMEOUT } from './VasFormUserNotification';

let props: Props & { onClose: jest.Mock };

beforeEach(() => {
    props = {
        text: 'foo',
        onClose: jest.fn(),
    };

    jest.useFakeTimers();
});

it('покажет нотификашку через таймаут', () => {
    const page = shallowRenderComponent({ props });
    expect(page.hasClass('VasFormUserNotification_visible')).toBe(false);

    jest.advanceTimersByTime(300);
    expect(page.hasClass('VasFormUserNotification_visible')).toBe(true);
});

it('автоматически скроет нотифайку после открытия', () => {
    const page = shallowRenderComponent({ props });
    jest.advanceTimersByTime(300);

    expect(page.hasClass('VasFormUserNotification_hidden')).toBe(false);

    jest.advanceTimersByTime(AUTO_HIDE_TIMEOUT);

    expect(page.hasClass('VasFormUserNotification_hidden')).toBe(true);
    expect(props.onClose).toHaveBeenCalledTimes(1);
});

it('при закрытии скроет нотификашку и через таймаут дернет проп', () => {
    const page = shallowRenderComponent({ props });
    const closer = page.find('.VasFormUserNotification__closer');

    expect(page.hasClass('VasFormUserNotification_hidden')).toBe(false);

    closer.simulate('click');

    expect(page.hasClass('VasFormUserNotification_hidden')).toBe(true);
    expect(props.onClose).toHaveBeenCalledTimes(0);

    jest.advanceTimersByTime(300);

    expect(props.onClose).toHaveBeenCalledTimes(1);
});

function shallowRenderComponent({ props }: { props: Props }) {
    const page = shallow(
        <VasFormUserNotification { ...props }/>,
    );

    return page;
}

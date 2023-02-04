/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { shallow } from 'enzyme';
import _ from 'lodash';

import contextMock from 'autoru-frontend/mocks/contextMock';

import type { Props } from './PanoramaPullToSlide';
import PanoramaPullToSlide, { SHOW_TIME_WINDOW } from './PanoramaPullToSlide';

let clickCallbacks: Array<() => void>;
let paintCallbacks: Array<() => void>;
const callTimes = (times: number) => (callback: () => void) => _.times(times, callback);

let props: Props;

beforeEach(() => {
    clickCallbacks = [];
    paintCallbacks = [];

    props = {
        isFirstTime: false,
        onHide: jest.fn(),
        registerOnContainerClickCallback: jest.fn((callback: () => void) => {
            clickCallbacks.push(callback);
        }),
        registerOnPaintCallback: jest.fn((callback: () => void) => {
            paintCallbacks.push(callback);
        }),
        unregisterOnContainerClickCallback: jest.fn((callback: () => void) => {
            _.pull(clickCallbacks, callback);
        }),
        unregisterOnPaintCallback: jest.fn((callback: () => void) => {
            _.pull(paintCallbacks, callback);
        }),
    };

    jest.useFakeTimers();
});

describe('в первый раз', () => {
    beforeEach(() => {
        props.isFirstTime = true;
    });

    it('покажет анимацию через 10 сек в любом случае', () => {
        const page = shallowRenderComponent({ props });

        expect(page.find('.PanoramaPullToSlide').isEmptyRender()).toBe(true);

        jest.advanceTimersByTime(0);
        // имитируем вращение на 3 кадра
        paintCallbacks.forEach(callTimes(3));

        jest.advanceTimersByTime(SHOW_TIME_WINDOW);
        expect(page.find('.PanoramaPullToSlide').isEmptyRender()).toBe(false);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'show-gallery', 'panorama-shows', 'exterior', 'gallery-animation', 'first_time' ]);
    });
});

describe('не в первый раз', () => {
    it('не покажет анимацию если я просто вращаю панораму', () => {
        const page = shallowRenderComponent({ props });

        expect(page.find('.PanoramaPullToSlide').isEmptyRender()).toBe(true);

        jest.advanceTimersByTime(0);
        // имитируем вращение на 3 кадра
        paintCallbacks.forEach(callTimes(3));

        jest.advanceTimersByTime(SHOW_TIME_WINDOW);
        expect(page.find('.PanoramaPullToSlide').isEmptyRender()).toBe(true);
    });

    it('покажет анимацию если я ничего не делаю', () => {
        const page = shallowRenderComponent({ props });

        expect(page.find('.PanoramaPullToSlide').isEmptyRender()).toBe(true);

        jest.advanceTimersByTime(SHOW_TIME_WINDOW);
        expect(page.find('.PanoramaPullToSlide').isEmptyRender()).toBe(false);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'show-gallery', 'panorama-shows', 'exterior', 'gallery-animation', 'no_action' ]);
    });

    it('покажет анимацию если я просто тапнул', () => {
        const page = shallowRenderComponent({ props });

        expect(page.find('.PanoramaPullToSlide').isEmptyRender()).toBe(true);

        jest.advanceTimersByTime(0);
        clickCallbacks.forEach(callTimes(1));

        jest.advanceTimersByTime(SHOW_TIME_WINDOW);
        expect(page.find('.PanoramaPullToSlide').isEmptyRender()).toBe(false);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'show-gallery', 'panorama-shows', 'exterior', 'gallery-animation', 'only_taps' ]);
    });

    it('покажет анимацию если я тапнул 3 раза и что-то вращал', () => {
        const page = shallowRenderComponent({ props });

        expect(page.find('.PanoramaPullToSlide').isEmptyRender()).toBe(true);

        jest.advanceTimersByTime(0);
        paintCallbacks.forEach(callTimes(5));
        clickCallbacks.forEach(callTimes(3));
        paintCallbacks.forEach(callTimes(5));

        jest.advanceTimersByTime(SHOW_TIME_WINDOW);
        expect(page.find('.PanoramaPullToSlide').isEmptyRender()).toBe(false);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'show-gallery', 'panorama-shows', 'exterior', 'gallery-animation', '3_taps' ]);
    });

    it('покажет анимацию если между вращением я тапнул один раз', () => {
        const page = shallowRenderComponent({ props });

        expect(page.find('.PanoramaPullToSlide').isEmptyRender()).toBe(true);

        jest.advanceTimersByTime(0);
        paintCallbacks.forEach(callTimes(5));
        clickCallbacks.forEach(callTimes(1));
        paintCallbacks.forEach(callTimes(5));

        jest.advanceTimersByTime(SHOW_TIME_WINDOW);
        expect(page.find('.PanoramaPullToSlide').isEmptyRender()).toBe(false);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'show-gallery', 'panorama-shows', 'exterior', 'gallery-animation', 'swipe_and_tap' ]);
    });
});

it('почистит за собой если ничего не надо показывать', () => {
    const page = shallowRenderComponent({ props });

    expect(page.find('.PanoramaPullToSlide').isEmptyRender()).toBe(true);

    jest.advanceTimersByTime(0);
    // имитируем вращение на 3 кадра
    paintCallbacks.forEach(callTimes(3));

    jest.advanceTimersByTime(SHOW_TIME_WINDOW);
    expect(props.unregisterOnContainerClickCallback).toHaveBeenCalledTimes(1);
    expect(props.unregisterOnPaintCallback).toHaveBeenCalledTimes(1);
});

function shallowRenderComponent({ props }: { props: Props }) {
    const page = shallow(
        <PanoramaPullToSlide { ...props }/>
        ,
        { context: contextMock },
    );

    return page;
}

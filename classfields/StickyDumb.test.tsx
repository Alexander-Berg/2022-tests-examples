/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { mount, shallow } from 'enzyme';
import { shallowToJson } from 'enzyme-to-json';

import StickyDumb from './StickyDumb';

let originalRequestAnimationFrame: any;
beforeEach(() => {
    originalRequestAnimationFrame = global.requestAnimationFrame;

    global.requestAnimationFrame = (callback) => {
        callback(0);
        return 1;
    };
});

afterEach(() => {
    global.requestAnimationFrame = originalRequestAnimationFrame;
});

it('должен проставлять правильное значение top-сдвига на высоту хэдера', () => {
    const tree = shallow(
        <StickyDumb isVisibleSearchOfferForm={ false }>
            <div>foo</div>
        </StickyDumb>,
    );

    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен проставлять правильное значение top-сдвига при открытой форме поиска', () => {
    const tree = shallow(
        <StickyDumb isVisibleSearchOfferForm={ true }>
            <div>foo</div>
        </StickyDumb>,
    );

    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен плюсовать offset к top-сдвигу', () => {
    const tree = shallow(
        <StickyDumb isVisibleSearchOfferForm={ true } offset={ 25 }>
            <div>foo</div>
        </StickyDumb>,
    );

    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен вызывать коллбэк с true, когда компонент прилипает', () => {
    const handler = jest.fn();

    const instance = renderInstanceWithScrollHandler(handler);

    setClientRectTop(instance, 20);

    instance.onScroll();

    expect(handler).toHaveBeenCalledWith(true);
});

it('должен вызывать коллбэк с false, когда компонент отлипает', () => {
    const handler = jest.fn();

    const instance = renderInstanceWithScrollHandler(handler);

    setClientRectTop(instance, 20);
    instance.onScroll();

    setClientRectTop(instance, 170);
    instance.onScroll();

    expect(handler).toHaveBeenCalledWith(false);
});

it('не должен вызывать коллбэк, когда компонент не меняет состояние', () => {
    const handler = jest.fn();

    const instance = renderInstanceWithScrollHandler(handler);

    setClientRectTop(instance, 170);
    instance.onScroll();

    expect(handler).not.toHaveBeenCalled();
});

function setClientRectTop(instance: StickyDumb, top: number) {
    if (instance.containerRef.current) {
        instance.containerRef.current.getBoundingClientRect = () => ({ top: top } as DOMRect);
    }
}

function renderInstanceWithScrollHandler(handler: ((state: boolean) => void)) {
    const tree = mount<StickyDumb>(
        <StickyDumb
            isVisibleSearchOfferForm={ false }
            onStickyChanged={ handler }
        >
            <div>foo</div>
        </StickyDumb>,
    );

    return tree.instance();
}

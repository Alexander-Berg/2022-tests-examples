/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/onDomContentLoaded', () => {
    return (callback: () => void) => callback();
});

import React from 'react';
import { shallow } from 'enzyme';
import type { DebouncedFunc } from 'lodash';

import contextMock from 'autoru-frontend/mocks/contextMock';

import type { Props } from './PageIndexDumb';
import PageIndexDumb from './PageIndexDumb';

const eventMap: Record<string, EventListenerOrEventListenerObject> = {};

beforeEach(() => {
    contextMock.hasExperiment.mockReset();
    jest.spyOn(global, 'addEventListener').mockImplementation((eventType, callback) => {
        eventMap[eventType] = callback;
    });
});

afterEach(() => {
    jest.restoreAllMocks();
});

describe('фаб', () => {
    it('покажет фаб при первом рендере', () => {
        const wrapper = shallowRenderComponent();
        const fab = wrapper.find('.PageIndex__addOfferFab');
        expect(fab.isEmptyRender()).toBe(false);
    });

    it('не покажет фаб при скролле вниз', () => {
        global.scrollY = 0;
        const wrapper = shallowRenderComponent();

        global.scrollY = 100;
        (eventMap.scroll as EventListener)({} as Event);

        const fab = wrapper.find('.PageIndex__addOfferFab');
        expect(fab.isEmptyRender()).toBe(true);
    });

    it('покажет фаб при скролле вверх', () => {
        global.scrollY = 0;
        const wrapper = shallowRenderComponent();

        global.scrollY = 100;
        (eventMap.scroll as EventListener)({} as Event);

        global.scrollY = 50;
        (eventMap.scroll as EventListener)({} as Event);

        const fab = wrapper.find('.PageIndex__addOfferFab');
        expect(fab.isEmptyRender()).toBe(false);
    });
});

describe('текст в фабе', () => {
    it('правильный для не-дилера-не-перекупа', () => {
        const wrapper = shallowRenderComponent();
        const fabControl = wrapper.find('.PageIndex__fabControl');

        expect(fabControl.children().text()).toBe('Разместить бесплатно');
    });

    it('правильный для дилера', () => {
        const wrapper = shallowRenderComponent({ props: { isClient: true }, context: contextMock });
        const fabControl = wrapper.find('.PageIndex__fabControl');

        expect(fabControl.children().text()).toBe('Разместить объявление');
    });

    it('правильный для перекупа', () => {
        const wrapper = shallowRenderComponent({ props: { isReseller: true }, context: contextMock });
        const fabControl = wrapper.find('.PageIndex__fabControl');

        expect(fabControl.children().text()).toBe('Разместить объявление');
    });
});

function shallowRenderComponent({ context, props }: { context?: typeof contextMock; props?: Partial<Props> } = { }) {

    class NonThrottledComponent extends PageIndexDumb {
        throttledHandleWindowScroll = this.handleWindowScroll as DebouncedFunc<() => void>;
    }

    const wrapper = shallow(
        <NonThrottledComponent
            pageParams={{ category: 'moto' }}
            searchTagsDictionary={ [] }
            openAddOfferModal={ jest.fn() }
            openAddOfferNavigateModal={ jest.fn() }
            mmmInfo={ [] }
            { ...props }
        />, { context: context || contextMock },
    );

    return wrapper;
}

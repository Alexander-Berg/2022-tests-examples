import React from 'react';
import { mount } from 'enzyme';

import sleep from 'auto-core/lib/sleep';

import ScrollableSidebar from './ScrollableSidebar';

const eventMap: Record<string, EventListenerOrEventListenerObject> = {};
let originalWindowInnerHeight: number;

beforeEach(() => {
    jest.spyOn(global, 'addEventListener').mockImplementation((eventType, callback) => {
        eventMap[eventType] = callback;
    });

    originalWindowInnerHeight = global.innerHeight;
    global.innerHeight = 1000;
});

afterEach(() => {
    jest.restoreAllMocks();
    global.innerHeight = originalWindowInnerHeight;
});

const renderComponent = (wrapHeight: number, adHeight: number, topRect: number, bottomRect: number) => {
    const page = mount(
        <div style={{ height: wrapHeight, width: 320 }}>
            <div style={{ height: 500 }}></div>
            <ScrollableSidebar>
                <div style={{ height: adHeight, width: 320 }}>Рекламка</div>
            </ScrollableSidebar>
        </div>,
    );

    const component = page.find(ScrollableSidebar);

    const instance = component.instance() as ScrollableSidebar;

    instance.containerRef = {
        current: {
            getBoundingClientRect: () => ({
                width: 320,
                height: adHeight,
                top: topRect,
                bottom: bottomRect,
                left: 0,
                right: 0,
                x: 0,
                y: 0,
            }),
            parentElement: {
                clientHeight: wrapHeight,
            },
        },
    } as unknown as React.RefObject<HTMLDivElement>;

    instance.componentDidMount();

    return instance;
};

it('не вешает никакие стили на сайдбар без скролла', () => {
    global.scrollY = 0;
    const instance = renderComponent(6000, 700, 500, 1000);

    expect(instance.state).toStrictEqual({ stickySide: 'none', styles: {} });
});

it('при скролле вешает sticky, если реклама умещается в экран', () => {
    global.scrollY = 0;
    const instance = renderComponent(6000, 700, 500, 1000);

    global.scrollY = 1000;
    (eventMap.scroll as EventListener)({} as Event);

    expect(instance.state).toStrictEqual({ stickySide: 'none', styles: { position: 'sticky', top: '24px' } });
});

describe('если реклама не умещается в экран', () => {
    it('при скролле вниз до конца рекламы вешает fixed, если мы не в конце страницы', async() => {
        global.scrollY = 0;
        const instance = renderComponent(6000, 1200, 500, 0);

        global.scrollY = 3000;
        (eventMap.scroll as EventListener)({} as Event);

        await sleep(50);

        expect(instance.state).toStrictEqual({ stickySide: 'bottom', styles: { position: 'fixed', top: 'auto', bottom: '24px', left: '0px', width: '320px' } });
    });

    it('фризит если скроллим вверх с прилипшей внизу рекламой', async() => {
        global.scrollY = 0;
        const instance = renderComponent(6000, 1200, 500, 0);

        global.scrollY = 3000;
        (eventMap.scroll as EventListener)({} as Event);

        await sleep(50);

        global.scrollY = 2900;
        (eventMap.scroll as EventListener)({} as Event);

        await sleep(50);

        expect(instance.state).toStrictEqual({ stickySide: 'none', styles: { position: 'relative', top: 'auto', transform: 'translate3d(0, 2900px, 0)' } });
    });

    it('при скролле вверх до конца рекламы вешает fixed, если мы не в начале страницы', async() => {
        global.scrollY = 4000;
        const instance = renderComponent(6000, 1200, 100, 1500);

        global.scrollY = 3200;
        (eventMap.scroll as EventListener)({} as Event);

        await sleep(50);

        global.scrollY = 2200;
        (eventMap.scroll as EventListener)({} as Event);

        await sleep(50);

        expect(instance.state).toStrictEqual({ stickySide: 'top', styles: { position: 'fixed', top: '24px', bottom: 'auto', left: '0px', width: '320px' } });
    });

    it('фризит если скроллим вниз с прилипшей вверху рекламой', async() => {
        global.scrollY = 4000;
        const instance = renderComponent(6000, 1200, 100, 1500);

        global.scrollY = 3200;
        (eventMap.scroll as EventListener)({} as Event);

        await sleep(50);

        global.scrollY = 2200;
        (eventMap.scroll as EventListener)({} as Event);

        await sleep(50);

        global.scrollY = 2300;
        (eventMap.scroll as EventListener)({} as Event);

        await sleep(50);

        expect(instance.state).toStrictEqual({ stickySide: 'none', styles: { position: 'relative', top: 'auto', transform: 'translate3d(0, 2300px, 0)' } });
    });
});

/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import type { ShallowWrapper } from 'enzyme';
import { shallow } from 'enzyme';
import { InView } from 'react-intersection-observer';

import panoramaExteriorMock from 'auto-core/models/panoramaExterior/mocks';

import type { Props } from './OfferPanorama';
import OfferPanorama from './OfferPanorama';

type OfferPanoramaShallowWrapper = ShallowWrapper<OfferPanorama['props'], OfferPanorama['state'], OfferPanorama>;

let props: Props;
let videoNodeMock: HTMLVideoElement;
let videoRectMock: DOMRect;
let eventMap: Record<string, EventListenerOrEventListenerObject>;
let originalWindowInnerHeight: number;
beforeEach(() => {
    eventMap = {};

    props = {
        panorama: panoramaExteriorMock.value(),
        isMobile: false,
        imageNum: 12,
        shouldSpinOnScroll: false,
        spinDurationLimit: 1,
    };

    videoRectMock = {
        width: 400,
        height: 300,
        top: 150,
        bottom: 450,
        left: 100,
        right: 500,
        x: 100,
        y: 150,
    } as DOMRect;

    videoNodeMock = {
        load: jest.fn(),
        duration: 1,
        getBoundingClientRect: () => videoRectMock,
        readyState: 1,
        currentTime: 0,
    } as unknown as HTMLVideoElement;

    jest.spyOn(global, 'addEventListener').mockImplementation((eventType: string, callback: EventListenerOrEventListenerObject) => {
        eventMap[eventType] = callback;
    });
    jest.spyOn(global, 'requestAnimationFrame').mockImplementation((callback: FrameRequestCallback) => {
        callback(1);
        return 1;
    });
    jest.spyOn(global, 'cancelAnimationFrame');
    originalWindowInnerHeight = global.innerHeight;
    global['innerHeight'] = 1000;
});

afterEach(() => {
    jest.restoreAllMocks();
    global['innerHeight'] = originalWindowInnerHeight;
});

it('когда видео попадет в область видимости начнет загружать его но только один раз', () => {
    const page = shallowRenderComponent({ props });

    const observer = page.find(InView);
    observer.simulate('change', true);
    expect(videoNodeMock.load).toHaveBeenCalledTimes(1);

    observer.simulate('change', false);
    observer.simulate('change', true);
    expect(videoNodeMock.load).toHaveBeenCalledTimes(1);
});

it('при смене id панорамы загрузит новую', () => {
    const page = shallowRenderComponent({ props });

    const observer = page.find(InView);
    observer.simulate('change', true);

    const panorama = panoramaExteriorMock.withId('foo').value();
    page.setProps({ panorama });

    expect(videoNodeMock.load).toHaveBeenCalledTimes(2);
});

describe('после загрузки видео', () => {
    it('если видео не играется по скроллу, поставит ему текущее время на средний кадр', () => {
        const page = shallowRenderComponent({ props });
        const video = page.find('video');

        // видео пока не готово проиграться, ничего не должно произойти
        video.simulate('canplaythrough');
        expect(videoNodeMock.currentTime).toBe(0);

        // видео готово, должно поменяться время
        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-ignore
        videoNodeMock['readyState'] = 4;
        video.simulate('canplaythrough');
        expect(videoNodeMock.currentTime).toBe(0.48);
    });

    it('если видео играется по скроллу, оставит ему начальное время', () => {
        props.shouldSpinOnScroll = true;
        const page = shallowRenderComponent({ props });

        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-ignore
        videoNodeMock['readyState'] = 4;
        const video = page.find('video');
        video.simulate('canplaythrough');

        expect(videoNodeMock.currentTime).toBe(0);
    });

    it('если видео играется по скроллу и переданн начальный кадр, установит соответствующее время', () => {
        props.shouldSpinOnScroll = true;
        props.initialFrame = 12;
        const page = shallowRenderComponent({ props });

        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-ignore
        videoNodeMock['readyState'] = 4;
        const video = page.find('video');
        video.simulate('canplaythrough');

        expect(videoNodeMock.currentTime).toBe(0.48);
    });
});

describe('при движении мыши по контейнеру с видео', () => {
    it('если видео не загружено то ничего не произойдет', () => {
        const page = shallowRenderComponent({ props });
        const container = page.find('.OfferPanorama');
        container.simulate('mousemove', { persist: () => { }, clientX: videoRectMock.left + 20 });

        expect(videoNodeMock.currentTime).toBe(0);
    });

    it('если видео загружено, изменит его время', () => {
        const page = shallowRenderComponent({ props });
        simulateVideoLoad(page);
        const container = page.find('.OfferPanorama');
        container.simulate('mousemove', { persist: () => { }, clientX: videoRectMock.left + 20 });

        expect(videoNodeMock.currentTime).toBe(0.05);
    });

    it('если движение незначительное относительно предыдущего раза, ничего не будет делать', () => {
        const page = shallowRenderComponent({ props });
        simulateVideoLoad(page);
        const container = page.find('.OfferPanorama');
        container.simulate('mousemove', { persist: () => { }, clientX: videoRectMock.left + 20 });
        container.simulate('mousemove', { persist: () => { }, clientX: videoRectMock.left + 10 });

        expect(videoNodeMock.currentTime).toBe(0.05);
    });

    it('если мы подходим к концу контейнера, то покажет инфо о кол-во доступных фото', () => {
        const page = shallowRenderComponent({ props });
        simulateVideoLoad(page);
        const container = page.find('.OfferPanorama');
        const brazzersMore = page.find('BrazzersMore');

        expect(brazzersMore.isEmptyRender()).toBe(true);

        container.simulate('mousemove', { persist: () => { }, clientX: videoRectMock.left + 350 });
        const updatedBrazzersMore = page.find('BrazzersMore');

        expect(updatedBrazzersMore.isEmptyRender()).toBe(false);
    });
});

describe('при скролле', () => {
    it('если видео не должно проигрываться по скроллу, не подпишеться на событие скролла', () => {
        const page = shallowRenderComponent({ props });
        simulateVideoLoad(page);

        expect(eventMap.scroll).toBeUndefined();
    });

    describe('если видео должно проигрывать по скроллу', () => {
        beforeEach(() => {
            props.shouldSpinOnScroll = true;
        });

        it('если видео не загрузилось, ничего не будет происходить', () => {
            const page = shallowRenderComponent({ props });
            scrollVideoInView(page);
            // eslint-disable-next-line @typescript-eslint/ban-ts-comment
            // @ts-ignore
            videoRectMock['bottom'] = 750;
            (eventMap.scroll as EventListener)({} as Event);

            expect(videoNodeMock.currentTime).toBe(0);
        });

        it('если разница в скролле относительно небольшая, изменит время без использования raf', () => {
            const page = shallowRenderComponent({ props });
            simulateVideoLoad(page);
            // eslint-disable-next-line @typescript-eslint/ban-ts-comment
            // @ts-ignore
            videoRectMock['bottom'] = 925;
            (eventMap.scroll as EventListener)({} as Event);

            expect(videoNodeMock.currentTime).toBe(0.04166666666666652);
        });

        it('если разница в скролле большая, изменит время с использованием raf', () => {
            const page = shallowRenderComponent({ props });
            simulateVideoLoad(page);
            // eslint-disable-next-line @typescript-eslint/ban-ts-comment
            // @ts-ignore
            videoRectMock['bottom'] = 750;
            (eventMap.scroll as EventListener)({} as Event);

            expect(global.requestAnimationFrame).toHaveBeenCalledTimes(10);
            expect(videoNodeMock.currentTime).toBe(0.36);
        });

        it('правильно рассчитает кадр, если видео должно прокрутится не полностью в указанном диапазоне', () => {
            props.spinDurationLimit = 0.5;
            props.initialFrame = 4;
            const page = shallowRenderComponent({ props });
            simulateVideoLoad(page);
            // eslint-disable-next-line @typescript-eslint/ban-ts-comment
            // @ts-ignore
            videoRectMock['bottom'] = 750;
            (eventMap.scroll as EventListener)({} as Event);

            expect(videoNodeMock.currentTime).toBe(0.36);
        });

        it('в мобилке при достижении последнего фрейма покажет иконку', () => {
            props.isMobile = true;
            const page = shallowRenderComponent({ props });
            simulateVideoLoad(page);
            // eslint-disable-next-line @typescript-eslint/ban-ts-comment
            // @ts-ignore
            videoRectMock['bottom'] = 150;

            const spinIcon = page.find('GalleryPanoramaLabel');
            expect(spinIcon.isEmptyRender()).toBe(true);

            (eventMap.scroll as EventListener)({} as Event);

            expect(videoNodeMock.currentTime).toBe(1);

            const updatedSpinIcon = page.find('GalleryPanoramaLabel');
            expect(updatedSpinIcon.isEmptyRender()).toBe(false);
        });
    });

});

function simulateVideoLoad(page: OfferPanoramaShallowWrapper) {
    scrollVideoInView(page);

    const video = page.find('video');
    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    videoNodeMock['readyState'] = 4;
    video.simulate('canplaythrough');
}

function scrollVideoInView(page: OfferPanoramaShallowWrapper) {
    const observer = page.find(InView);
    observer.simulate('change', true);
}

function shallowRenderComponent({ props }: { props: Props }) {
    const page = shallow<OfferPanorama>(<OfferPanorama { ...props }/>, { lifecycleExperimental: true });
    const instance = page.instance();
    instance.video = videoNodeMock;
    return page;
}

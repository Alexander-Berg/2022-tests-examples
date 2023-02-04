/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import type { ShallowWrapper } from 'enzyme';
import { shallow } from 'enzyme';
import _ from 'lodash';

import contextMock from 'autoru-frontend/mocks/contextMock';
import flushPromises from 'autoru-frontend/jest/unit/flushPromises';

import type { Props, Item, InjectedProps } from './StoriesGallery';
import StoriesGallery, {
    RENDERED_SLIDES_NUM,
    MAX_SCALE,
    MIN_SCALE_X,
    MIN_SCALE_Y,
    SLIDE_HEIGHT,
    SLIDE_WIDTH,
    MAX_CONTAINER_OPACITY,
    MIN_CONTAINER_OPACITY,
    DEFAULT_ANIMATION_TIME,
    SLOW_ANIMATION_TIME,
    FRAME_DURATION,
    SWIPE_TO_CHANGE_THRESHOLD,
    SWIPE_TO_CLOSE_THRESHOLD,
} from './StoriesGallery';

const SlideContent = ({ currentPage, item }: InjectedProps<Item>) => {
    return (
        <div className="SlideContent">slide id={ item.data.id }, page #{ currentPage }</div>
    );
};

let props: Props<Item>;

function generateItem(index: number): Item {
    return { data: { id: `foo-${ index }`, raw: { pages: [ { duration: 3000 } ] } }, status: 'initial' };
}

const CLIENT_RECT_WIDTH = 450;
const CLIENT_RECT_HEIGHT = 800;

beforeEach(() => {
    props = {
        activeSlideIndex: 0,
        items: _.times(15, generateItem),
        onRequestHide: jest.fn(),
        onSlideChange: jest.fn(),
        renderSlideContent: SlideContent,
    };

    jest.spyOn(global, 'requestAnimationFrame').mockImplementation((callback) => {
        callback(0);
        return 0;
    });
});

afterEach(() => {
    jest.restoreAllMocks();
});

describe('добавляет только необходимые слайды', () => {
    it('если активный слайд в начале галереи', () => {
        const { page } = shallowRenderComponent({ props });

        const slides = page.find('.StoriesGallery ForwardRef');
        const isActiveProp = slides.map((slide) => slide.prop('isActive'));

        expect(isActiveProp).toEqual([ true, ..._.times(RENDERED_SLIDES_NUM, () => false) ]);
    });

    it('если активный слайд в середине галереи', () => {
        props.activeSlideIndex = 8;
        const { page } = shallowRenderComponent({ props });

        const slides = page.find('.StoriesGallery ForwardRef');
        const isActiveProp = slides.map((slide) => slide.prop('isActive'));

        expect(isActiveProp).toEqual([ ..._.times(RENDERED_SLIDES_NUM, () => false), true, ..._.times(RENDERED_SLIDES_NUM, () => false) ]);
    });

    it('если активный слайд в конце галереи', () => {
        props.activeSlideIndex = props.items.length - 1;
        const { page } = shallowRenderComponent({ props });

        const slides = page.find('.StoriesGallery ForwardRef');
        const isActiveProp = slides.map((slide) => slide.prop('isActive'));

        expect(isActiveProp).toEqual([ ..._.times(RENDERED_SLIDES_NUM, () => false), true ]);
    });
});

it('правильно определяет активный слайд', () => {
    props.activeSlideIndex = 3;
    const { page } = shallowRenderComponent({ props });

    const activeSlide = page.find('.StoriesGallery ForwardRef').at(props.activeSlideIndex);
    const notActiveSlide = page.find('.StoriesGallery ForwardRef').at(props.activeSlideIndex - 1);

    expect(activeSlide.prop('isActive')).toBe(true);
    expect(activeSlide.hasClass('StoriesGallery__slide_active')).toBe(true);

    expect(notActiveSlide.prop('isActive')).toBe(false);
    expect(notActiveSlide.hasClass('StoriesGallery__slide_active')).toBe(false);
});

describe('пауза', () => {
    it('ставит на паузу незагруженные слайды', () => {
        const { page } = shallowRenderComponent({ props });

        const firstSlide = page.find('.StoriesGallery ForwardRef').at(0);
        expect(firstSlide.prop('isPaused')).toBe(true);
    });

    it('не ставит на паузу загруженные слайды', () => {
        props.items[0].status = 'success';
        const { page } = shallowRenderComponent({ props });

        const firstSlide = page.find('.StoriesGallery ForwardRef').at(0);
        expect(firstSlide.prop('isPaused')).toBe(false);
    });
});

describe('горизонтальный свайп', () => {
    beforeEach(() => {
        props.activeSlideIndex = 1;
    });

    it('добавляет трансформацию слайдам при движении', () => {
        const { instance, page } = shallowRenderComponent({ props });
        waitForOpenAnimation(page);

        const gallery = page.find('.StoriesGallery');
        const currentSlide = instance.slides[props.activeSlideIndex].ref.current;
        const nextSlide = instance.slides[props.activeSlideIndex + 1].ref.current;
        const prevSlide = instance.slides[props.activeSlideIndex - 1].ref.current;

        const diff = (200 - 110) / CLIENT_RECT_WIDTH;
        gallery.simulate('touchStart', { touches: [ { clientX: 200, clientY: 200 } ] });

        // проверяем движение справа налево
        gallery.simulate('touchMove', { touches: [ { clientX: 110, clientY: 200 } ] });
        expect(currentSlide?.style.transform).toBe(`translate(-${ SLIDE_WIDTH }%, 0%) scale(${ MAX_SCALE - diff * (MAX_SCALE - MIN_SCALE_X) })`);
        expect(nextSlide?.style.transform).toBe(`translate(-${ SLIDE_WIDTH * (1 + diff) }%, 0%) scale(${ MAX_SCALE })`);
        expect(prevSlide?.style.transform).toBe(`translate(0%, 0%) scale(${ MIN_SCALE_X })`);

        // проверяем движение слева направо
        gallery.simulate('touchMove', { touches: [ { clientX: 290, clientY: 200 } ] });
        expect(currentSlide?.style.transform).toBe(`translate(-${ SLIDE_WIDTH * (1 - diff) }%, 0%) scale(${ MAX_SCALE })`);
        expect(nextSlide?.style.transform).toBe(`translate(-${ SLIDE_WIDTH * (1 + diff) }%, 0%) scale(${ MAX_SCALE })`);
        expect(prevSlide?.style.transform).toBe(`translate(0%, 0%) scale(${ MIN_SCALE_X + diff * (MAX_SCALE - MIN_SCALE_X) })`);
    });

    it('не будет реагировать на незначительные смещения', () => {
        const { instance, page } = shallowRenderComponent({ props });
        waitForOpenAnimation(page);

        const gallery = page.find('.StoriesGallery');
        gallery.simulate('touchStart', { touches: [ { clientX: 200, clientY: 200 } ] });
        gallery.simulate('touchMove', { touches: [ { clientX: 190, clientY: 200 } ] });

        const currentSlide = instance.slides[props.activeSlideIndex].ref.current;
        const nextSlide = instance.slides[props.activeSlideIndex + 1].ref.current;

        expect(currentSlide?.style.transform).toBe(`translate(-${ SLIDE_WIDTH }%, 0%) scale(${ MAX_SCALE })`);
        expect(nextSlide?.style.transform).toBe(`translate(-${ SLIDE_WIDTH }%, 0%) scale(${ MAX_SCALE })`);
    });

    describe('при отпускании, когда вели справа налево', () => {
        it('плавно сменит слайд на следующий, если провели достаточно', async() => {
            const { instance, page } = shallowRenderComponent({ props });
            waitForOpenAnimation(page);

            const gallery = page.find('.StoriesGallery');
            const startX = 200;
            const endX = startX - SWIPE_TO_CHANGE_THRESHOLD * CLIENT_RECT_WIDTH - 1;
            gallery.simulate('touchStart', { touches: [ { clientX: startX, clientY: 200 } ] });
            gallery.simulate('touchMove', { touches: [ { clientX: endX, clientY: 200 } ] });
            gallery.simulate('touchEnd', { touches: [], changedTouches: [ { clientX: endX, clientY: 200 } ] });

            await flushPromises();

            // проверяем статус и смещения слайдов
            const nextSlideIndex = props.activeSlideIndex + 1;
            const currentSlide = page.find('.StoriesGallery ForwardRef').at(props.activeSlideIndex);
            const nextSlide = page.find('.StoriesGallery ForwardRef').at(nextSlideIndex);

            expect(currentSlide.prop('isActive')).toBe(false);
            expect(instance.slides[props.activeSlideIndex].ref.current?.style.transform).toBe(`translate(-${ SLIDE_WIDTH }%, 0%) scale(${ MIN_SCALE_X })`);

            expect(nextSlide.prop('isActive')).toBe(true);
            expect(instance.slides[nextSlideIndex].ref.current?.style.transform)
                .toBe(`translate(-${ SLIDE_WIDTH * nextSlideIndex }%, 0%) scale(${ MAX_SCALE })`);

            // как протестировать что оно плавно меняется непонятно
            // пока просто проверяем что raf вызвался эн-раз
            const animationStepsNum = getAnimationStepsNum(DEFAULT_ANIMATION_TIME);
            expect(global.requestAnimationFrame).toHaveBeenCalledTimes(animationStepsNum);

            // проверяем что вызвали коллбэк из пропсов
            expect(props.onSlideChange).toHaveBeenCalledTimes(1);
            expect(props.onSlideChange).toHaveBeenCalledWith(`foo-${ nextSlideIndex }`);
        });

        it('плавно вернет слайды на место, если провели недостаточно', async() => {
            const { instance, page } = shallowRenderComponent({ props });
            waitForOpenAnimation(page);

            const gallery = page.find('.StoriesGallery');
            const startX = 200;
            const endX = startX - SWIPE_TO_CHANGE_THRESHOLD * CLIENT_RECT_WIDTH + 1;
            gallery.simulate('touchStart', { touches: [ { clientX: startX, clientY: 200 } ] });
            gallery.simulate('touchMove', { touches: [ { clientX: endX, clientY: 200 } ] });
            gallery.simulate('touchEnd', { touches: [], changedTouches: [ { clientX: endX, clientY: 200 } ] });

            await flushPromises();

            // проверяем статус и смещения слайдов
            const nextSlideIndex = props.activeSlideIndex + 1;
            const currentSlide = page.find('.StoriesGallery ForwardRef').at(props.activeSlideIndex);
            const nextSlide = page.find('.StoriesGallery ForwardRef').at(nextSlideIndex);

            expect(currentSlide.prop('isActive')).toBe(true);
            expect(instance.slides[props.activeSlideIndex].ref.current?.style.transform).toBe(`translate(-${ SLIDE_WIDTH }%, 0%) scale(${ MAX_SCALE })`);

            expect(nextSlide.prop('isActive')).toBe(false);
            expect(instance.slides[nextSlideIndex].ref.current?.style.transform).toBe(`translate(-${ SLIDE_WIDTH }%, 0%) scale(${ MAX_SCALE })`);

            // как протестировать что оно плавно меняется непонятно
            // пока просто проверяем что raf вызвался эн-раз
            const animationStepsNum = getAnimationStepsNum(DEFAULT_ANIMATION_TIME);
            // +1 = оххх, тут оно для одного слайда точно не попало в конечную точку без доп шага
            expect(global.requestAnimationFrame).toHaveBeenCalledTimes(animationStepsNum + 1);

            // проверяем что вызвали коллбэк из пропсов
            expect(props.onSlideChange).toHaveBeenCalledTimes(0);
        });
    });

    describe('при отпускании, когда вели слева направо', () => {
        it('плавно сменит слайд на предыдущий, если провели достаточно', async() => {
            const { instance, page } = shallowRenderComponent({ props });
            waitForOpenAnimation(page);

            const gallery = page.find('.StoriesGallery');
            const startX = 200;
            const endX = startX + SWIPE_TO_CHANGE_THRESHOLD * CLIENT_RECT_WIDTH + 1;
            gallery.simulate('touchStart', { touches: [ { clientX: startX, clientY: 200 } ] });
            gallery.simulate('touchMove', { touches: [ { clientX: endX, clientY: 200 } ] });
            gallery.simulate('touchEnd', { touches: [], changedTouches: [ { clientX: endX, clientY: 200 } ] });

            await flushPromises();

            // проверяем статус и смещения слайдов
            const prevSlideIndex = props.activeSlideIndex - 1;
            const currentSlide = page.find('.StoriesGallery ForwardRef').at(props.activeSlideIndex);
            const prevSlide = page.find('.StoriesGallery ForwardRef').at(prevSlideIndex);

            expect(currentSlide.prop('isActive')).toBe(false);
            expect(instance.slides[props.activeSlideIndex].ref.current?.style.transform).toBe(`translate(0%, 0%) scale(${ MAX_SCALE })`);

            expect(prevSlide.prop('isActive')).toBe(true);
            expect(instance.slides[prevSlideIndex].ref.current?.style.transform).toBe(`translate(0%, 0%) scale(${ MAX_SCALE })`);

            // как протестировать что оно плавно меняется непонятно
            // пока просто проверяем что raf вызвался эн-раз
            const animationStepsNum = getAnimationStepsNum(DEFAULT_ANIMATION_TIME);
            expect(global.requestAnimationFrame).toHaveBeenCalledTimes(animationStepsNum);

            // проверяем что вызвали коллбэк из пропсов
            expect(props.onSlideChange).toHaveBeenCalledTimes(1);
            expect(props.onSlideChange).toHaveBeenCalledWith(`foo-${ prevSlideIndex }`);
        });

        it('плавно вернет слайды на место, если провели недостаточно', async() => {
            const { instance, page } = shallowRenderComponent({ props });
            waitForOpenAnimation(page);

            const gallery = page.find('.StoriesGallery');
            const startX = 200;
            const endX = startX + SWIPE_TO_CHANGE_THRESHOLD * CLIENT_RECT_WIDTH - 1;
            gallery.simulate('touchStart', { touches: [ { clientX: startX, clientY: 200 } ] });
            gallery.simulate('touchMove', { touches: [ { clientX: endX, clientY: 200 } ] });
            gallery.simulate('touchEnd', { touches: [], changedTouches: [ { clientX: endX, clientY: 200 } ] });

            await flushPromises();

            // проверяем статус и смещения слайдов
            const prevSlideIndex = props.activeSlideIndex - 1;
            const currentSlide = page.find('.StoriesGallery ForwardRef').at(props.activeSlideIndex);
            const prevSlide = page.find('.StoriesGallery ForwardRef').at(prevSlideIndex);

            expect(currentSlide.prop('isActive')).toBe(true);
            expect(instance.slides[props.activeSlideIndex].ref.current?.style.transform).toBe(`translate(-${ SLIDE_WIDTH }%, 0%) scale(${ MAX_SCALE })`);

            expect(prevSlide.prop('isActive')).toBe(false);
            expect(instance.slides[prevSlideIndex].ref.current?.style.transform).toBe(`translate(0%, 0%) scale(${ MIN_SCALE_X })`);

            // как протестировать что оно плавно меняется непонятно
            // пока просто проверяем что raf вызвался эн-раз
            const animationStepsNum = getAnimationStepsNum(DEFAULT_ANIMATION_TIME);
            // +1 = оххх, тут оно для одного слайда точно не попало в конечную точку без доп шага
            expect(global.requestAnimationFrame).toHaveBeenCalledTimes(animationStepsNum + 1);

            // проверяем что вызвали коллбэк из пропсов
            expect(props.onSlideChange).toHaveBeenCalledTimes(0);
        });
    });
});

describe('вертикальный свайп', () => {
    it('добавляет стили текущему слайду и контейнеру при движении', () => {
        const { instance, page } = shallowRenderComponent({ props });
        waitForOpenAnimation(page);

        const gallery = page.find('.StoriesGallery');
        const currentSlide = instance.slides[props.activeSlideIndex].ref.current;
        const container = instance.container.ref.current;

        const diff = (310 - 200) / CLIENT_RECT_HEIGHT;
        gallery.simulate('touchStart', { touches: [ { clientX: 200, clientY: 200 } ] });

        // проверяем движение сверху вниз
        gallery.simulate('touchMove', { touches: [ { clientX: 200, clientY: 310 } ] });
        expect(currentSlide?.style.transform).toBe(`translate(0%, ${ diff * 100 }%) scale(${ MAX_SCALE - diff * (MAX_SCALE - MIN_SCALE_Y) })`);
        const nextBgOpacity = MAX_CONTAINER_OPACITY - diff * (MAX_CONTAINER_OPACITY - MIN_CONTAINER_OPACITY);
        expect(container?.style.backgroundColor).toBe(`rgba(0,0,0,${ nextBgOpacity })`);

        // проверяем движение снизу вверх (заведомо ведем больше, чтобы проверить границу)
        gallery.simulate('touchMove', { touches: [ { clientX: 200, clientY: 150 } ] });
        expect(currentSlide?.style.transform).toBe(`translate(0%, 0%) scale(${ MAX_SCALE })`);
    });

    it('не будет реагировать на незначительные смещения', () => {
        const { instance, page } = shallowRenderComponent({ props });
        waitForOpenAnimation(page);

        const gallery = page.find('.StoriesGallery');
        const currentSlide = instance.slides[props.activeSlideIndex].ref.current;
        const container = instance.container.ref.current;

        gallery.simulate('touchStart', { touches: [ { clientX: 200, clientY: 200 } ] });
        gallery.simulate('touchMove', { touches: [ { clientX: 200, clientY: 210 } ] });
        expect(currentSlide?.style.transform).toBe(`translate(0%, 0%) scale(${ MAX_SCALE })`);
        expect(container?.style.backgroundColor).toBeUndefined();
    });

    it('не будет реагировать на свайп вверх', () => {
        const { instance, page } = shallowRenderComponent({ props });
        waitForOpenAnimation(page);

        const gallery = page.find('.StoriesGallery');
        const currentSlide = instance.slides[props.activeSlideIndex].ref.current;
        const container = instance.container.ref.current;

        gallery.simulate('touchStart', { touches: [ { clientX: 200, clientY: 200 } ] });
        gallery.simulate('touchMove', { touches: [ { clientX: 200, clientY: 110 } ] });
        expect(currentSlide?.style.transform).toBe(`translate(0%, 0%) scale(${ MAX_SCALE })`);
        expect(container?.style.backgroundColor).toBe(`rgba(0,0,0,${ MAX_CONTAINER_OPACITY })`);
    });

    describe('при отпускании, когда вели сверху вниз', () => {
        it('плавно закроет галерею, если провели достаточно', async() => {
            const { instance, page } = shallowRenderComponent({ props });
            waitForOpenAnimation(page);

            const gallery = page.find('.StoriesGallery');
            const currentSlide = instance.slides[props.activeSlideIndex].ref.current;
            const container = instance.container.ref.current;

            const startY = 200;
            const endY = startY + SWIPE_TO_CLOSE_THRESHOLD * CLIENT_RECT_HEIGHT + 1;

            gallery.simulate('touchStart', { touches: [ { clientX: 200, clientY: startY } ] });
            gallery.simulate('touchMove', { touches: [ { clientX: 200, clientY: endY } ] });
            gallery.simulate('touchEnd', { touches: [], changedTouches: [ { clientX: 200, clientY: endY } ] });

            await flushPromises();

            // как протестировать, что оно плавно меняется непонятно
            // пока просто проверяем что raf вызвался эн-раз
            const animationStepsNum = getAnimationStepsNum(DEFAULT_ANIMATION_TIME);
            // +1 = оххх, тут оно для одного слайда точно не попало в конечную точку без доп шага
            expect(global.requestAnimationFrame).toHaveBeenCalledTimes(animationStepsNum + 1);

            // смотрим что текущий слайд опустился вниз, а у контейнера поменялась bgOpacity
            expect(currentSlide?.style.transform).toBe(`translate(0%, ${ SLIDE_HEIGHT }%) scale(${ MIN_SCALE_Y })`);
            expect(container?.style.backgroundColor).toBe(`rgba(0,0,0,${ MIN_CONTAINER_OPACITY })`);

            const overlay = page.find('Overlay');
            expect(overlay.prop('visible')).toBe(false);

            expect(props.onRequestHide).toHaveBeenCalledTimes(1);
        });

        it('вернет слайд на место, если провели не достаточно', async() => {
            const { instance, page } = shallowRenderComponent({ props });
            waitForOpenAnimation(page);

            const gallery = page.find('.StoriesGallery');
            const currentSlide = instance.slides[props.activeSlideIndex].ref.current;
            const container = instance.container.ref.current;

            const startY = 200;
            const endY = startY + SWIPE_TO_CLOSE_THRESHOLD * CLIENT_RECT_HEIGHT - 1;

            gallery.simulate('touchStart', { touches: [ { clientX: 200, clientY: startY } ] });
            gallery.simulate('touchMove', { touches: [ { clientX: 200, clientY: endY } ] });
            gallery.simulate('touchEnd', { touches: [], changedTouches: [ { clientX: 200, clientY: endY } ] });

            await flushPromises();

            // как протестировать, что оно плавно меняется непонятно
            // пока просто проверяем что raf вызвался эн-раз
            const animationStepsNum = getAnimationStepsNum(DEFAULT_ANIMATION_TIME);
            // +1 = оххх, тут оно для одного слайда точно не попало в конечную точку без доп шага
            expect(global.requestAnimationFrame).toHaveBeenCalledTimes(animationStepsNum + 1);

            // смотрим что текущий слайд опустился вниз, а у контейнера поменялась bgOpacity
            expect(currentSlide?.style.transform).toBe(`translate(0%, 0%) scale(${ MAX_SCALE })`);
            expect(container?.style.backgroundColor).toBe(`rgba(0,0,0,${ MAX_CONTAINER_OPACITY })`);

            const overlay = page.find('Overlay');
            expect(overlay.prop('visible')).toBe(true);

            expect(props.onRequestHide).toHaveBeenCalledTimes(0);
        });
    });
});

describe('направление свайпа', () => {
    it('не будет реагировать на вертикальные смещения, если начали вести горизонтально', () => {
        props.activeSlideIndex = 1;
        const { instance, page } = shallowRenderComponent({ props });
        waitForOpenAnimation(page);

        const gallery = page.find('.StoriesGallery');
        const currentSlide = instance.slides[props.activeSlideIndex].ref.current;

        const diff = (200 - 110) / CLIENT_RECT_WIDTH;
        gallery.simulate('touchStart', { touches: [ { clientX: 200, clientY: 200 } ] });

        // ведем значимо горизонтально
        gallery.simulate('touchMove', { touches: [ { clientX: 110, clientY: 200 } ] });
        // ведем значимо вертикально
        gallery.simulate('touchMove', { touches: [ { clientX: 110, clientY: 310 } ] });

        // проверяем, что текущий слайд никак не среагировал на вертикальное смещение
        expect(currentSlide?.style.transform).toBe(`translate(-${ SLIDE_WIDTH }%, 0%) scale(${ MAX_SCALE - diff * (MAX_SCALE - MIN_SCALE_X) })`);
    });

    it('не будет реагировать на горизонтальные смещения, если начали вести вертикально', () => {
        const { instance, page } = shallowRenderComponent({ props });
        waitForOpenAnimation(page);

        const gallery = page.find('.StoriesGallery');
        const currentSlide = instance.slides[props.activeSlideIndex].ref.current;

        const diff = (310 - 200) / CLIENT_RECT_HEIGHT;
        gallery.simulate('touchStart', { touches: [ { clientX: 200, clientY: 200 } ] });

        // ведем значимо вертикально
        gallery.simulate('touchMove', { touches: [ { clientX: 200, clientY: 310 } ] });
        // ведем значимо горизонтально
        gallery.simulate('touchMove', { touches: [ { clientX: 110, clientY: 310 } ] });
        expect(currentSlide?.style.transform).toBe(`translate(0%, ${ diff * 100 }%) scale(${ MAX_SCALE - diff * (MAX_SCALE - MIN_SCALE_Y) })`);
    });
});

describe('смена страниц внутри слайда', () => {
    describe('вперед', () => {
        it('переключить плавно на следующий, если мы не в конце', async() => {
            const { instance, page } = shallowRenderComponent({ props });
            waitForOpenAnimation(page);

            const firstSlide = page.find('.StoriesGallery ForwardRef').first();
            firstSlide.simulate('nextSlide');
            await flushPromises();

            const updatedFirstSlide = page.find('.StoriesGallery ForwardRef').first();
            const secondSlide = page.find('.StoriesGallery ForwardRef').at(1);

            // проверяем статус и смещения слайдов
            expect(updatedFirstSlide.prop('isActive')).toBe(false);
            expect(instance.slides[0].ref.current?.style.transform).toBe(`translate(0%, 0%) scale(${ MIN_SCALE_X })`);

            expect(secondSlide.prop('isActive')).toBe(true);
            expect(instance.slides[1].ref.current?.style.transform).toBe(`translate(-${ SLIDE_WIDTH }%, 0%) scale(${ MAX_SCALE })`);

            // как протестировать что оно плавно меняется непонятно
            // пока просто проверяем что raf вызвался эн-раз
            const animationStepsNum = getAnimationStepsNum(SLOW_ANIMATION_TIME);
            // -1 = оххх, тут оно для одного слайда точно попало в конечную точку без доп шага
            expect(global.requestAnimationFrame).toHaveBeenCalledTimes(animationStepsNum - 1);

            // проверяем что вызвали коллбэк из пропсов
            expect(props.onSlideChange).toHaveBeenCalledTimes(1);
            expect(props.onSlideChange).toHaveBeenCalledWith('foo-1');
        });

        it('закроет галерею, если мы в конце', async() => {
            const lasIndex = props.items.length - 1;
            props.activeSlideIndex = lasIndex;
            const { instance, page } = shallowRenderComponent({ props });
            waitForOpenAnimation(page);

            const lastSlide = page.find('.StoriesGallery ForwardRef').last();
            lastSlide.simulate('nextSlide');

            // проверяем что предыдущие (на примере первого) сдвинуты вниз еще до анимации перехода
            expect(instance.slides[0].dyp).toBe(SLIDE_HEIGHT);

            await flushPromises();

            // как протестировать что оно плавно меняется непонятно
            // пока просто проверяем что raf вызвался эн-раз
            const animationStepsNum = getAnimationStepsNum(SLOW_ANIMATION_TIME);
            expect(global.requestAnimationFrame).toHaveBeenCalledTimes(animationStepsNum);

            // смотрим что текущий слайд опустился вниз, а у контейнера поменялась bgOpacity
            expect(instance.slides[lasIndex].ref.current?.style.transform)
                .toBe(`translate(-${ RENDERED_SLIDES_NUM * SLIDE_WIDTH }%, ${ SLIDE_HEIGHT }%) scale(${ MIN_SCALE_Y })`);
            expect(instance.container.ref.current?.style.backgroundColor).toBe(`rgba(0,0,0,${ MIN_CONTAINER_OPACITY })`);

            const overlay = page.find('Overlay');
            expect(overlay.prop('visible')).toBe(false);

            expect(props.onRequestHide).toHaveBeenCalledTimes(1);
        });
    });

    describe('назад', () => {
        it('закроет галерею, если мы в начале', async() => {
            const { instance, page } = shallowRenderComponent({ props });
            waitForOpenAnimation(page);

            const firstSlide = page.find('.StoriesGallery ForwardRef').first();
            firstSlide.simulate('prevSlide');
            await flushPromises();

            // как протестировать что оно плавно меняется непонятно
            // пока просто проверяем что raf вызвался эн-раз
            const animationStepsNum = getAnimationStepsNum(SLOW_ANIMATION_TIME);
            expect(global.requestAnimationFrame).toHaveBeenCalledTimes(animationStepsNum);

            // смотрим что текущий слайд опустился вниз, а у контейнера поменялась bgOpacity
            expect(instance.slides[0].ref.current?.style.transform).toBe(`translate(0%, ${ SLIDE_HEIGHT }%) scale(${ MIN_SCALE_Y })`);
            expect(instance.container.ref.current?.style.backgroundColor).toBe(`rgba(0,0,0,${ MIN_CONTAINER_OPACITY })`);

            const overlay = page.find('Overlay');
            expect(overlay.prop('visible')).toBe(false);

            expect(props.onRequestHide).toHaveBeenCalledTimes(1);
        });

        it('переключить плавно на предыдущий, если мы не в начале', async() => {
            const currentIndex = 3;
            props.activeSlideIndex = currentIndex;
            const { instance, page } = shallowRenderComponent({ props });
            waitForOpenAnimation(page);

            const activeSlide = page.find('.StoriesGallery ForwardRef').at(currentIndex);
            activeSlide.simulate('prevSlide');
            await flushPromises();

            const newActiveSlide = page.find('.StoriesGallery ForwardRef').at(currentIndex - 1);
            const nextSlide = page.find('.StoriesGallery ForwardRef').at(currentIndex);

            // проверяем статус и смещения слайдов
            expect(nextSlide.prop('isActive')).toBe(false);
            expect(instance.slides[currentIndex].ref.current?.style.transform)
                .toBe(`translate(-${ (currentIndex - 1) * SLIDE_WIDTH }%, 0%) scale(${ MAX_SCALE })`);

            expect(newActiveSlide.prop('isActive')).toBe(true);
            expect(instance.slides[currentIndex - 1].ref.current?.style.transform)
                .toBe(`translate(-${ (currentIndex - 1) * SLIDE_WIDTH }%, 0%) scale(${ MAX_SCALE })`);

            // как протестировать что оно плавно меняется непонятно
            // пока просто проверяем что raf вызвался эн-раз
            const animationStepsNum = getAnimationStepsNum(SLOW_ANIMATION_TIME);
            expect(global.requestAnimationFrame).toHaveBeenCalledTimes(animationStepsNum);

            // проверяем что вызвали коллбэк из пропсов
            expect(props.onSlideChange).toHaveBeenCalledTimes(1);
            expect(props.onSlideChange).toHaveBeenCalledWith(`foo-${ currentIndex - 1 }`);
        });
    });
});

it('при клике на крестик закроет галерею', async() => {
    const { instance, page } = shallowRenderComponent({ props });
    waitForOpenAnimation(page);

    const firstSlide = page.find('.StoriesGallery ForwardRef').first();
    firstSlide.simulate('closerClick');
    await flushPromises();

    // как протестировать что оно плавно меняется непонятно
    // пока просто проверяем что raf вызвался эн-раз
    const animationStepsNum = getAnimationStepsNum(DEFAULT_ANIMATION_TIME);
    // +2 = оххх, тут оно для слайда и контейнера точно не попало в конечную точку без доп шага
    expect(global.requestAnimationFrame).toHaveBeenCalledTimes(animationStepsNum + 2);

    // смотрим что текущий слайд опустился вниз, а у контейнера поменялась bgOpacity
    expect(instance.slides[props.activeSlideIndex].ref.current?.style.transform).toBe(`translate(0%, ${ SLIDE_HEIGHT }%) scale(${ MIN_SCALE_Y })`);
    expect(instance.container.ref.current?.style.backgroundColor).toBe(`rgba(0,0,0,${ MIN_CONTAINER_OPACITY })`);

    const overlay = page.find('Overlay');
    expect(overlay.prop('visible')).toBe(false);

    expect(props.onRequestHide).toHaveBeenCalledTimes(1);
});

function waitForOpenAnimation(page: ShallowWrapper) {
    const gallery = page.find('.StoriesGallery');
    gallery.simulate('animationEnd', { animationName: 'StoriesGalleryShowAnimation' });
}

function getAnimationStepsNum(time: number) {
    // тут мы анимируем 2 слайда или 1 слайд и бэкграунд (при закрытии) каждый за одинаковое время time
    // общее кол-во шагов будет таким
    // +1 - это доп шаг, чтобы убедится, что мы перевалили за конечную точку анимации
    // ибо промежуточные показатели выражены в дробях и не всегда итоговая сумма будет точно равна желаемому результату
    // справедливо для чисел кратных 3 (но может и еще для каких)
    return (Math.ceil(time / FRAME_DURATION) + (time % 3 ? 0 : 1)) * 2;
}

function shallowRenderComponent({ props }: { props: Props<Item> }) {
    const page = shallow(
        <StoriesGallery { ...props }/>,
        { disableLifecycleMethods: true, context: contextMock },
    );

    const instance = page.instance() as StoriesGallery<Item>;

    instance.container.ref = {
        current: {
            getBoundingClientRect: () => ({
                width: CLIENT_RECT_WIDTH,
                height: CLIENT_RECT_HEIGHT,
                top: 0,
                bottom: CLIENT_RECT_HEIGHT,
                left: 0,
                right: CLIENT_RECT_WIDTH,
                x: 0,
                y: 0,
            }),
            style: {},
        },
    } as unknown as React.RefObject<HTMLDivElement>;

    instance.slides.forEach((slide) => {
        slide.ref = {
            current: {
                style: {},
            },
        } as unknown as React.RefObject<HTMLDivElement>;
    });

    if (typeof instance.componentDidMount === 'function') {
        instance.componentDidMount();
    }

    return {
        page,
        instance,
    };
}

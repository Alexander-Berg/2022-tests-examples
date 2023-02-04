/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import type { DebouncedFunc } from 'lodash';
import type { ShallowWrapper } from 'enzyme';
import { shallow } from 'enzyme';
import _ from 'lodash';
import { InView } from 'react-intersection-observer';

import contextMock from 'autoru-frontend/mocks/contextMock';

import type { PanoramaFrame } from '../types';

import type { Props } from './PanoramaExteriorPainter';
import PanoramaExteriorPainter from './PanoramaExteriorPainter';

const drawImageMock = jest.fn();
const FILE_NAME_REGEXP = /_(0000)\./;

const images = _.range(100).map((index) => {
    const image = new Image() as PanoramaFrame;
    image.isLoaded = true;
    image.src = 'http://s3.yandex.net/panorama/1200_0000.webp'.replace(FILE_NAME_REGEXP, `_${ _.padStart(String(index), 4, '0') }.`);

    return image;
});

const eventMock = {
    stopPropagation: () => { },
    preventDefault: () => { },
    target: { classList: { contains: () => true } },
    currentTarget: { classList: { contains: () => true } },
    nativeEvent: {},
    clientX: 400,
    clientY: 500,
};
const touchEventMock = {
    touches: [ { clientX: 500, clientY: 500 } ],
    stopPropagation: () => { },
    preventDefault: () => { },
};
const containerClientRect = {
    width: 800,
    height: 600,
    top: 150,
    bottom: 950,
    left: 100,
    right: 700,
    x: 100,
    y: 150,
};
const eventMap: Record<string, EventListenerOrEventListenerObject> = {};
let originalWindowInnerHeight: number;

let props: Props;

beforeEach(() => {
    props = {
        containerRef: {
            current: {
                getBoundingClientRect: () => containerClientRect,
            },
        } as React.RefObject<HTMLDivElement>,
        isAddHotSpotMode: false,
        isFrozen: false,
        isInGallery: false,
        isWindows: false,
        isZoomOnScrollEnabled: false,
        images: images,
        shouldSpinOnScroll: false,
        onDragStateChange: jest.fn(),
    };

    drawImageMock.mockClear();

    jest.spyOn(global, 'addEventListener').mockImplementation((eventType, callback) => {
        eventMap[eventType] = callback;
    });
    jest.spyOn(global, 'requestAnimationFrame').mockImplementation((callback) => {
        callback(0);
        return 0;
    });
    jest.spyOn(global, 'cancelAnimationFrame');

    originalWindowInnerHeight = global.innerHeight;
    global.innerHeight = 1000;
});

afterEach(() => {
    jest.restoreAllMocks();
    global.innerHeight = originalWindowInnerHeight;
});

describe('при маунте правильно отправит метрику показа', () => {
    it('на странице', () => {
        shallowRenderComponent({ props });

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendPageEvent.mock.calls[0]).toMatchSnapshot();
    });

    it('в галерее', () => {
        props.isInGallery = true;
        shallowRenderComponent({ props });

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendPageEvent.mock.calls[0]).toMatchSnapshot();
    });
});

it('при маунте нарисует фрейм с поворотом на 1/8 влево', () => {
    shallowRenderComponent({ props });

    expect(drawImageMock).toHaveBeenCalledTimes(1);
    const drawnImageIndex = getImageIndexInPath(drawImageMock.mock.calls[0][0].src);
    expect(Number(drawnImageIndex)).toBe(12);
});

describe('при дрэге мышкой', () => {
    it('если панорама не зумирована прокрутит машину циклично', () => {
        // нужно помнить что мы вращаем машину в противоположную сторону движения мыши
        // нам важно проверить цикличность, поэтому делаем все в одном тест-кейсе и последовательно
        const { page } = shallowRenderComponent({ props });
        drawImageMock.mockClear();

        // проверяем на расстоянии меньше чем один оборот
        const canvas = page.find('canvas');
        canvas.simulate('mousedown', { ...eventMock, clientX: 500 });
        canvas.simulate('mousemove', { ...eventMock, clientX: 250 });
        canvas.simulate('mouseup');

        expect(drawImageMock).toHaveBeenCalledTimes(1);
        const drawnImageIndex = getImageIndexInPath(drawImageMock.mock.calls[0][0].src);
        expect(Number(drawnImageIndex)).toBe(5);

        // вот тут он должен перескочить на начало
        drawImageMock.mockClear();
        canvas.simulate('mousedown', { ...eventMock, clientX: 250 });
        canvas.simulate('mousemove', { ...eventMock, clientX: 150 });
        canvas.simulate('mouseup');
        const drawnImageIndex2 = getImageIndexInPath(drawImageMock.mock.calls[0][0].src);
        expect(Number(drawnImageIndex2)).toBe(42);

        // вот тут должен вернуться обратно
        drawImageMock.mockClear();
        canvas.simulate('mousedown', { ...eventMock, clientX: 150 });
        canvas.simulate('mousemove', { ...eventMock, clientX: 250 });
        canvas.simulate('mouseup');
        const drawnImageIndex3 = getImageIndexInPath(drawImageMock.mock.calls[0][0].src);
        expect(Number(drawnImageIndex3)).toBe(5);
    });

    it('при вращении вперед и назад скорость будет одинаковая', () => {
        const { page } = shallowRenderComponent({ props });
        drawImageMock.mockClear();

        const canvas = page.find('canvas');

        // сдвигаем мышь вправо и возвращаем назад
        // проверяем что мы вернулись в 0
        canvas.simulate('mousedown', { ...eventMock, clientX: 500 });
        canvas.simulate('mousemove', { ...eventMock, clientX: 530 });
        canvas.simulate('mousemove', { ...eventMock, clientX: 500 });
        canvas.simulate('mouseup');

        const drawnImageIndex2 = getImageIndexInPath(drawImageMock.mock.calls[1][0].src);
        expect(Number(drawnImageIndex2)).toBe(12);
    });

    it('панорама будет вращаться при медленном движении мыши', () => {
        const { page } = shallowRenderComponent({ props });
        drawImageMock.mockClear();

        const canvas = page.find('canvas');

        expect(drawImageMock).toHaveBeenCalledTimes(0);
        // тут значимое смещение рассчитывается так
        // ширина контейнера / (кол-во картинок * спид-фактор) = 800 / (100 * 3) = 2,67 пикселя
        // смещаем по пикселю и смотрим что в конце-концов мы нарисовали один фрейм
        canvas.simulate('mousedown', { ...eventMock, clientX: 500 });
        canvas.simulate('mousemove', { ...eventMock, clientX: 499 });
        canvas.simulate('mousemove', { ...eventMock, clientX: 498 });
        canvas.simulate('mousemove', { ...eventMock, clientX: 497 });

        expect(drawImageMock).toHaveBeenCalledTimes(1);
        const drawnImageIndex = getImageIndexInPath(drawImageMock.mock.calls[0][0].src);
        expect(Number(drawnImageIndex)).toBe(13);
    });

    it('отправит метрику один раз при вращении на значимый угол', () => {
        const { page } = shallowRenderComponent({ props });

        const canvas = page.find('canvas');
        contextMock.metrika.sendPageEvent.mockReset();

        // сдвинули мало не должны логировать
        canvas.simulate('mousedown', { ...eventMock, clientX: 500 });
        canvas.simulate('mousemove', { ...eventMock, clientX: 530 });
        canvas.simulate('mouseup');
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(0);

        // меняем направление и сдвигаем значимо, проверяем что залогировали правильно
        canvas.simulate('mousedown', { ...eventMock, clientX: 500 });
        canvas.simulate('mousemove', { ...eventMock, clientX: 530 });
        canvas.simulate('mousemove', { ...eventMock, clientX: 250 });
        canvas.simulate('mouseup');
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendPageEvent.mock.calls[0]).toMatchSnapshot();

        // проверяем что логируется только первое значимое смещение
        contextMock.metrika.sendPageEvent.mockReset();
        canvas.simulate('mousedown', { ...eventMock, clientX: 500 });
        canvas.simulate('mousemove', { ...eventMock, clientX: 250 });
        canvas.simulate('mousemove', { ...eventMock, clientX: 0 });
        canvas.simulate('mouseup');
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
    });

    it('если панорама зумирована будет перетаскивать картинку', () => {
        props.isZoomOnScrollEnabled = true;
        // специально берем контейнер больше чем сам канвас
        (props.containerRef.current as HTMLElement).getBoundingClientRect = () => ({
            ...containerClientRect,
            height: 1400,
        }) as DOMRect;
        // const wheelEventMock = { ...eventMock, clientX: 500, clientY: 500, nativeEvent: { deltaY: -100 } };
        const { componentInstance, page } = shallowRenderComponent({ props });

        const canvas = page.find('canvas');
        componentInstance.handleZoomButtonClick(true);

        // видим что оно заскейлилось
        expect(componentInstance.canvas.current?.style.transform).toBe('translate(-400px, 100px) scale(2)');

        // начинаем водить мышкой
        canvas.simulate('mousedown', { ...eventMock, clientX: 500, clientY: 500 });
        canvas.simulate('mousemove', { ...eventMock, clientX: 250, clientY: 400 });
        canvas.simulate('mouseup');
        expect(componentInstance.canvas.current?.style.transform).toBe('translate(-650px, 0px) scale(2)');

        // ведем очень далеко влево, проверяем границу в нижнем правом углу
        canvas.simulate('mousedown', { ...eventMock, clientX: 850, clientY: 750 });
        canvas.simulate('mousemove', { ...eventMock, clientX: 150, clientY: 200 });
        canvas.simulate('mouseup');
        expect(componentInstance.canvas.current?.style.transform).toBe('translate(-800px, 0px) scale(2)');

        // ведем очень далеко вправо, проверяем границу в верхнем левом углу
        canvas.simulate('mousedown', { ...eventMock, clientX: 150, clientY: 200 });
        canvas.simulate('mousemove', { ...eventMock, clientX: 800, clientY: 750 });
        canvas.simulate('mouseup');
        canvas.simulate('mousedown', { ...eventMock, clientX: 150, clientY: 200 });
        canvas.simulate('mousemove', { ...eventMock, clientX: 800, clientY: 750 });
        canvas.simulate('mouseup');
        expect(componentInstance.canvas.current?.style.transform).toBe('translate(0px, 200px) scale(2)');
    });
});

describe('при вращении колеса мыши', () => {
    let wheelEventMock: typeof eventMock;

    beforeEach(() => {
        wheelEventMock = { ...eventMock, clientX: 500, clientY: 500, nativeEvent: { deltaY: -100 } };
    });

    it('не будет зумить если зум по скроллу не разрешен', () => {
        const { componentInstance, page } = shallowRenderComponent({ props });

        const canvas = page.find('canvas');
        canvas.simulate('wheel', wheelEventMock);

        expect(componentInstance.canvas.current?.style.transform).toBe('translate(0px, 0px) scale(1)');
    });

    describe('при зуме', () => {
        let componentInstance: PanoramaExteriorPainter;
        let canvas: ShallowWrapper;

        beforeEach(() => {
            props.isZoomOnScrollEnabled = true;
            const wrapper = shallowRenderComponent({ props });
            componentInstance = wrapper.componentInstance;
            canvas = wrapper.page.find('canvas');
        });

        it('правильно рассчитает трансформацию', () => {
            canvas.simulate('wheel', wheelEventMock);

            expect(componentInstance.canvas.current?.style.transform).toBe('translate(-400px, -350px) scale(2)');

            // проверяем также многократный зум
            const wheelEventMock2 = { ...eventMock, clientX: 300, clientY: 700, nativeEvent: { deltaY: -50 } };
            canvas.simulate('wheel', wheelEventMock2);

            expect(componentInstance.canvas.current?.style.transform).toBe('translate(-550px, -575px) scale(2.5)');
        });

        it('будет зумить только до максимально обозначенного значения', () => {
            const wheelEventMock = { ...eventMock, clientX: 300, clientY: 700, nativeEvent: { deltaY: -500 } };
            canvas.simulate('wheel', wheelEventMock);

            expect(componentInstance.canvas.current?.style.transform).toBe('translate(-600px, -1650px) scale(4)');
        });

        it('будет зумить обратно только для минимального значения', () => {
            canvas.simulate('wheel', wheelEventMock);
            const wheelEventMock2 = { ...eventMock, clientX: 300, clientY: 700, nativeEvent: { deltaY: 200 } };
            canvas.simulate('wheel', wheelEventMock2);

            expect(componentInstance.canvas.current?.style.transform).toBe('translate(0px, 0px) scale(1)');
        });
    });
});

describe('данные для кнопок зума', () => {
    it('знает когда зум минимальный', () => {
        props.isZoomOnScrollEnabled = true;
        const { componentInstance, page } = shallowRenderComponent({ props });

        const canvas = page.find('canvas');
        canvas.simulate('wheel', { ...eventMock, clientX: 500, clientY: 500, nativeEvent: { deltaY: -400 } });
        canvas.simulate('wheel', { ...eventMock, clientX: 500, clientY: 500, nativeEvent: { deltaY: 400 } });

        expect(componentInstance.state.isMaxZoomed).toBe(false);
        expect(componentInstance.state.isMinZoomed).toBe(true);
    });

    it('знает когда зум максимальный', () => {
        props.isZoomOnScrollEnabled = true;
        const { componentInstance, page } = shallowRenderComponent({ props });

        const canvas = page.find('canvas');
        canvas.simulate('wheel', { ...eventMock, clientX: 500, clientY: 500, nativeEvent: { deltaY: -400 } });

        expect(componentInstance.state.isMaxZoomed).toBe(true);
        expect(componentInstance.state.isMinZoomed).toBe(false);
    });

    it('при клике на контролы зум меняется на один шаг', () => {
        const { componentInstance } = shallowRenderComponent({ props });

        componentInstance.handleZoomButtonClick(true);
        componentInstance.handleZoomButtonClick(true);
        expect(componentInstance.canvas.current?.style.transform).toBe('translate(-800px, -600px) scale(3)');

        componentInstance.handleZoomButtonClick(false);
        expect(componentInstance.canvas.current?.style.transform).toBe('translate(-400px, -300px) scale(2)');
    });
});

describe('в таче по дрэгу', () => {

    it('крутит панораму', () => {
        const { page } = shallowRenderComponent({ props });
        drawImageMock.mockClear();

        // дрэг, начинаем вести пальцем
        const canvas = page.find('canvas');
        canvas.simulate('touchstart', touchEventMock);
        canvas.simulate('touchmove', { ...touchEventMock, touches: [ { clientX: 300, clientY: 500 } ] });
        canvas.simulate('touchend', touchEventMock);

        expect(drawImageMock).toHaveBeenCalledTimes(1);
        const drawnImageIndex = getImageIndexInPath(drawImageMock.mock.calls[0][0].src);
        expect(Number(drawnImageIndex)).toBe(37);
    });

    it('отправит метрику один раз при вращении на значимый угол', () => {
        const { page } = shallowRenderComponent({ props });
        contextMock.metrika.sendPageEvent.mockClear();

        const canvas = page.find('canvas');

        // сдвинули мало не должны логировать
        canvas.simulate('touchstart', { ...touchEventMock, touches: [ { clientX: 100, clientY: 500 } ] });
        canvas.simulate('touchmove', { ...touchEventMock, touches: [ { clientX: 70, clientY: 500 } ] });
        canvas.simulate('touchend', touchEventMock);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(0);

        // меняем направление и сдвигаем значимо, проверяем что залогировали правильно
        canvas.simulate('touchstart', { ...touchEventMock, touches: [ { clientX: 100, clientY: 500 } ] });
        canvas.simulate('touchmove', { ...touchEventMock, touches: [ { clientX: 70, clientY: 500 } ] });
        canvas.simulate('touchmove', { ...touchEventMock, touches: [ { clientX: 350, clientY: 500 } ] });
        canvas.simulate('touchend', touchEventMock);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendPageEvent.mock.calls[0]).toMatchSnapshot();

        // проверяем что логируется только первое значимое смещение
        contextMock.metrika.sendPageEvent.mockReset();
        canvas.simulate('touchstart', { ...touchEventMock, touches: [ { clientX: 100, clientY: 500 } ] });
        canvas.simulate('touchmove', { ...touchEventMock, touches: [ { clientX: 350, clientY: 500 } ] });
        canvas.simulate('touchmove', { ...touchEventMock, touches: [ { clientX: 700, clientY: 500 } ] });
        canvas.simulate('touchend', touchEventMock);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
    });

    it('тащит панораму если она зазумлена', () => {
        const { componentInstance, page } = shallowRenderComponent({ props });

        // пинч, начинаем вести двумя пальцами
        const pinchStartEventMock = { ...touchEventMock, touches: [ { clientX: 300, clientY: 600 }, { clientX: 700, clientY: 300 } ] };
        const pinchMoveEventMock = { ...touchEventMock, touches: [ { clientX: 180, clientY: 510 }, { clientX: 720, clientY: 290 } ] };
        const canvas = page.find('canvas');
        canvas.simulate('touchstart', pinchStartEventMock);
        canvas.simulate('touchmove', pinchMoveEventMock);
        canvas.simulate('touchend', touchEventMock);

        // видим что оно заскейлилось, проверяем смещения
        expect(componentInstance.canvas.current?.style.transform).toBe('translate(-58px, -42px) scale(1.16619037896906)');

        // дрэг, начинаем тащить
        const dragStartEventMock = { ...touchEventMock, touches: [ { clientX: 200, clientY: 500 } ] };
        const dragMoveEventMock = { ...touchEventMock, touches: [ { clientX: 250, clientY: 450 } ] };
        canvas.simulate('touchstart', dragStartEventMock);
        canvas.simulate('touchmove', dragMoveEventMock);
        canvas.simulate('touchend', touchEventMock);

        // видим что смещения относительно обычного зума изменились а скейл нет
        expect(componentInstance.canvas.current?.style.transform).toBe('translate(-8px, -92px) scale(1.16619037896906)');
    });
});

it('в таче при пинче зумит панораму', () => {
    const { componentInstance, page } = shallowRenderComponent({ props });
    drawImageMock.mockClear();

    // пинч, начинаем вести двумя пальцами
    const pinchStartEventMock = { ...touchEventMock, touches: [ { clientX: 200, clientY: 500 }, { clientX: 700, clientY: 300 } ] };
    const pinchMoveEventMock = { ...touchEventMock, touches: [ { clientX: 180, clientY: 510 }, { clientX: 720, clientY: 290 } ] };
    const canvas = page.find('canvas');
    canvas.simulate('touchstart', pinchStartEventMock);
    canvas.simulate('touchmove', pinchMoveEventMock);
    canvas.simulate('touchend', touchEventMock);

    expect(drawImageMock).toHaveBeenCalledTimes(0);
    expect(componentInstance.canvas.current?.style.transform).toBe('translate(-29px, -21px) scale(1.0827805840074194)');
});

it('при маунте если контейнер по высоте больше канваса, расположит канвас в центре', () => {
    (props.containerRef.current as HTMLElement).getBoundingClientRect = () => ({
        ...containerClientRect,
        height: 1400,
    }) as DOMRect;
    const { componentInstance } = shallowRenderComponent({ props });

    expect(componentInstance.canvas.current?.style.transform).toBe('translate(0px, 400px) scale(1)');
});

describe('метод playPanoramaToIndex() вращает панораму до заданного фрейма наименьшим путём', () => {
    it('по часовой стрелке', () => {
        const { componentInstance } = shallowRenderComponent({ props });
        drawImageMock.mockClear();

        componentInstance.playPanoramaToIndex(20);
        const drawnImageIndexes = drawImageMock.mock.calls.map(([ image ]) => getImageIndexInPath(image.src));
        expect(drawnImageIndexes).toHaveLength(8);
    });

    it('против часовой стрелке', () => {
        const { componentInstance } = shallowRenderComponent({ props });
        drawImageMock.mockClear();

        const promise = componentInstance.playPanoramaToIndex(90);
        return promise
            .then(() => {
                const drawnImageIndexes = drawImageMock.mock.calls.map(([ image ]) => getImageIndexInPath(image.src));
                expect(drawnImageIndexes).toHaveLength(22);
            });
    });
});

it('метод animateYMove() инкрементально сдвинет канвас до нужного положения', () => {
    const { componentInstance } = shallowRenderComponent({ props });

    // сначала зумим, чтобы можно было двигать
    componentInstance.handleZoomButtonClick(true);
    const promise = componentInstance.animateYMove(-200);
    return promise
        .then(() => {
            expect(global.requestAnimationFrame).toHaveBeenCalledTimes(5);
        });
});

it('метод animateZoom() инкрементально изменить масштаб до нужного', () => {
    const { componentInstance } = shallowRenderComponent({ props });

    const promise = componentInstance.animateZoom(2, { x: 400, y: 300 });
    return promise
        .then(() => {
            expect(global.requestAnimationFrame).toHaveBeenCalledTimes(4);
        });
});

describe('вращение при скролле страницы', () => {
    it('если разница в кадрах больше 1, отрисует все промежуточные кадры', () => {
        props.shouldSpinOnScroll = true;
        global.scrollY = 0;

        const { page } = shallowRenderComponent({ props });
        drawImageMock.mockClear();

        const observer = page.find(InView);
        observer.simulate('change', true);

        global.scrollY = 800;
        (props.containerRef.current as HTMLElement).getBoundingClientRect = () => ({
            ...containerClientRect,
            bottom: 150,
        }) as DOMRect;
        (eventMap.scroll as EventListener)({} as Event);

        expect(drawImageMock).toHaveBeenCalledTimes(5);

        const drawnImageIndex = drawImageMock.mock.calls.map((call) => call[0].src).map(getImageIndexInPath);
        expect(drawnImageIndex).toEqual([ '013', '014', '015', '016', '017' ]);

        // также проверяем что оно норм варащается в обратном направлении
        drawImageMock.mockClear();

        global.scrollY = 0;
        (props.containerRef.current as HTMLElement).getBoundingClientRect = () => ({
            ...containerClientRect,
            bottom: 950,
        }) as DOMRect;
        (eventMap.scroll as EventListener)({} as Event);

        expect(drawImageMock).toHaveBeenCalledTimes(5);

        const drawnImageIndex2 = drawImageMock.mock.calls.map((call) => call[0].src).map(getImageIndexInPath);
        expect(drawnImageIndex2).toEqual([ '016', '015', '014', '013', '012' ]);
    });

    it('если разница в кадрах равна 1, отрисует только один кадр', () => {
        props.shouldSpinOnScroll = true;
        (props.containerRef.current as HTMLElement).getBoundingClientRect = () => ({
            ...containerClientRect,
            bottom: 920,
        }) as DOMRect;
        global.scrollY = 0;

        const { page } = shallowRenderComponent({ props });
        drawImageMock.mockClear();

        const observer = page.find(InView);
        observer.simulate('change', true);

        global.scrollY = 250;
        (eventMap.scroll as EventListener)({} as Event);

        expect(drawImageMock).toHaveBeenCalledTimes(1);

        const drawnImageIndex = drawImageMock.mock.calls.map((call) => call[0].src).map(getImageIndexInPath);
        expect(drawnImageIndex).toEqual([ '013' ]);
    });

    it('если панораму вращали, при дальнейшем скролле продолжит с текущего кадра', () => {
        props.shouldSpinOnScroll = true;
        (props.containerRef.current as HTMLElement).getBoundingClientRect = () => ({
            ...containerClientRect,
            bottom: 920,
        }) as DOMRect;
        global.scrollY = 0;

        const { page } = shallowRenderComponent({ props });

        const canvas = page.find('canvas');
        const observer = page.find(InView);
        observer.simulate('change', true);
        canvas.simulate('mousedown', { ...eventMock, clientX: 500 });
        canvas.simulate('mousemove', { ...eventMock, clientX: 540 });
        canvas.simulate('mouseup');
        drawImageMock.mockClear();

        global.scrollY = 250;
        (props.containerRef.current as HTMLElement).getBoundingClientRect = () => ({
            ...containerClientRect,
            bottom: 750,
        }) as DOMRect;
        // (containerRectMock as unknown as Record<string, number>).top = 200;
        (eventMap.scroll as EventListener)({} as Event);

        expect(drawImageMock).toHaveBeenCalledTimes(4);

        const drawnImageIndex = drawImageMock.mock.calls.map((call) => call[0].src).map(getImageIndexInPath);
        expect(drawnImageIndex).toEqual([ '098', '099', '000', '001' ]);
    });

    it('если панорама вне поля видимости, ничего не будет делать', () => {
        props.shouldSpinOnScroll = true;
        global.scrollY = 0;
        (props.containerRef.current as HTMLElement).getBoundingClientRect = () => ({
            ...containerClientRect,
            bottom: 920,
        }) as DOMRect;

        shallowRenderComponent({ props });
        drawImageMock.mockClear();

        global.scrollY = 250;
        (eventMap.scroll as EventListener)({} as Event);

        expect(drawImageMock).toHaveBeenCalledTimes(0);
    });

    it('при дрэге мышкой, не будет дополнительно вращать по скроллу', () => {
        props.shouldSpinOnScroll = true;
        global.scrollY = 0;
        (props.containerRef.current as HTMLElement).getBoundingClientRect = () => ({
            ...containerClientRect,
            bottom: 920,
        }) as DOMRect;

        const { page } = shallowRenderComponent({ props });

        const canvas = page.find('canvas');
        const observer = page.find(InView);
        observer.simulate('change', true);
        canvas.simulate('mousedown', { ...eventMock, clientX: 500 });
        canvas.simulate('mousemove', { ...eventMock, clientX: 550 });
        drawImageMock.mockClear();

        global.scrollY = 250;
        (eventMap.scroll as EventListener)({} as Event);

        expect(drawImageMock).toHaveBeenCalledTimes(0);
    });

    it('если панорама зазумлена, не будет вращать по скроллу', () => {
        props.shouldSpinOnScroll = true;
        props.isZoomOnScrollEnabled = true;
        global.scrollY = 0;
        (props.containerRef.current as HTMLElement).getBoundingClientRect = () => ({
            ...containerClientRect,
            bottom: 920,
        }) as DOMRect;

        const { page } = shallowRenderComponent({ props });

        const canvas = page.find('canvas');
        const observer = page.find(InView);
        observer.simulate('change', true);
        canvas.simulate('wheel', { ...eventMock, clientX: 500, clientY: 500, nativeEvent: { deltaY: -100 } });
        drawImageMock.mockClear();

        global.scrollY = 250;
        (eventMap.scroll as EventListener)({} as Event);

        expect(drawImageMock).toHaveBeenCalledTimes(0);
    });
});

function getImageIndexInPath(str: string) {
    return str.slice(-8, -5);
}

function shallowRenderComponent({ props }: { props: Props }) {
    class NonThrottledComponent extends PanoramaExteriorPainter {
        throttledHandleMouseMove = this.handleMouseMove as DebouncedFunc<(event: React.MouseEvent<HTMLCanvasElement, MouseEvent>) => void>;
        throttledHandleWheelMove = this.handleWheelMove as DebouncedFunc<(event: React.MouseEvent<HTMLCanvasElement, MouseEvent>) => void>;
        throttledHandleScroll = this.handleScroll as DebouncedFunc<() => void>;
    }

    const page = shallow(
        <NonThrottledComponent { ...props }/>,
        { disableLifecycleMethods: true, context: contextMock },
    );

    const componentInstance = page.instance() as PanoramaExteriorPainter;

    componentInstance.canvas = {
        current: {
            width: 800,
            height: 600,
            style: {
                transform: '',
            },
            getContext: () => ({
                drawImage: drawImageMock,
            }),
            getBoundingClientRect: () => ({
                width: 800,
                height: 600,
                top: 50,
                bottom: 650,
                left: 100,
                right: 900,
                x: 100,
                y: 50,
            }),
        },
    } as unknown as React.RefObject<HTMLCanvasElement>;

    if (typeof componentInstance.componentDidMount === 'function') {
        componentInstance.componentDidMount();
    }

    return {
        page,
        componentInstance,
    };
}

/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const playerEventMap: Record<string, () => void> = {};
jest.mock('../utils/ymaps.utils', () => ({
    PanoramaMap: jest.fn(),
    definePanoramaClasses: jest.fn(),
}));

import React from 'react';
import type { DebouncedFunc } from 'lodash';
import { shallow } from 'enzyme';
import type ymaps from 'yandex-maps';

import contextMock from 'autoru-frontend/mocks/contextMock';

import panoramaInteriorMock from 'auto-core/models/panoramaInterior/mocks';

import YandexMapsUtils from '../utils/ymaps.utils';

import type { Props } from './PanoramaInteriorPainter';
import PanoramaInteriorPainter from './PanoramaInteriorPainter';

const mockYaPlayer = {
    destroy: jest.fn(),
    getSpan: jest.fn(() => ([ 80, 80 ])),
    setSpan: jest.fn(),
    getDirection: jest.fn(() => ([ 30, 10 ])),
    events: {
        add: jest.fn((event: string, cb: () => void) => {
            playerEventMap[event] = cb;
        }),
    },
};
const eventMap: Record<string, EventListenerOrEventListenerObject> = {};

let props: Props;

beforeEach(() => {
    props = {
        data: panoramaInteriorMock.value(),
        isCurrentSlide: true,
        isTouch: false,
        lib: {
            panorama: {
                isSupported: jest.fn(() => true),
                Player: jest.fn(() => mockYaPlayer),
            },
        } as unknown as typeof ymaps,
        options: {
            defaultDirection: [ Math.PI, 0, 0 ],
        },
        place: 'card_page',
        onDragStateChange: jest.fn(),
        onTap: jest.fn(),
    };

    jest.spyOn(global, 'addEventListener').mockImplementation((eventType, callback) => {
        eventMap[eventType] = callback;
    });
});

afterEach(() => {
    jest.restoreAllMocks();
});

describe('инициализация', () => {
    it('проставит уникальный айди плееру', () => {
        const { page } = shallowRenderComponent({ props });
        expect(page.find('.PanoramaInteriorPainter').prop('id')).toBe('PanoramaInteriorPainter_4036500412-1595961776760-KD1uu_card_page');
    });

    it('создаст плеер и передаст в него корректные параметры', () => {
        shallowRenderComponent({ props });

        expect(YandexMapsUtils.PanoramaMap).toHaveBeenCalledTimes(1);
        expect(YandexMapsUtils.PanoramaMap).toHaveBeenCalledWith(props.lib, props.data, props.options);

        expect(props.lib.panorama.Player).toHaveBeenCalledTimes(1);
        expect((props.lib.panorama.Player as jest.Mock).mock.calls[0]).toMatchSnapshot();
    });

    it('если это текущий слайд галереи, отправит метрику показа', () => {
        props.isCurrentSlide = true;
        shallowRenderComponent({ props });

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendPageEvent.mock.calls[0]).toMatchSnapshot();
    });
});

it('при смене id панорамы удалит удалит старый плеер и проинициализирует новый', async() => {
    const { page } = shallowRenderComponent({ props });

    const newPanorama = { ...props.data, id: 'foo' };
    page.setProps({ data: newPanorama });

    expect(mockYaPlayer.destroy).toHaveBeenCalledTimes(1);
    expect(props.lib.panorama.Player).toHaveBeenCalledTimes(2);
    expect((props.lib.panorama.Player as jest.Mock).mock.calls).toMatchSnapshot();
});

describe('проп onDragStateChange', () => {
    it('вызовется при mouse down', () => {
        const { page } = shallowRenderComponent({ props });
        const painter = page.find('.PanoramaInteriorPainter');
        painter.simulate('mousedown');

        expect(props.onDragStateChange).toHaveBeenCalledTimes(1);
        expect(props.onDragStateChange).toHaveBeenCalledWith(true);
    });

    it('вызовется при mouse up', () => {
        const { page } = shallowRenderComponent({ props });
        const painter = page.find('.PanoramaInteriorPainter');
        painter.simulate('mouseup');

        expect(props.onDragStateChange).toHaveBeenCalledTimes(1);
        expect(props.onDragStateChange).toHaveBeenCalledWith(false);
    });
});

describe('данные для кнопок зума', () => {
    it('правильно рассчитывает следующий зум', () => {
        const { instance } = shallowRenderComponent({ props });

        expect(instance.state).toMatchSnapshot();

        instance.handleZoomButtonClick(true);

        expect(mockYaPlayer.setSpan).toHaveBeenCalledTimes(1);
        expect(mockYaPlayer.setSpan).toHaveBeenLastCalledWith([ 64, 64 ]);
        expect(instance.state).toMatchSnapshot();
    });

    it('не будет зумить дальше максимального зума', () => {
        const { instance } = shallowRenderComponent({ props });

        instance.handleZoomButtonClick(true);
        instance.handleZoomButtonClick(true);
        instance.handleZoomButtonClick(true);
        instance.handleZoomButtonClick(true);
        instance.handleZoomButtonClick(true);

        expect(mockYaPlayer.setSpan).toHaveBeenLastCalledWith([ 32, 32 ]);
        expect(instance.state).toMatchSnapshot();
    });

    it('не будет зумить меньше минимального зума', () => {
        const { instance } = shallowRenderComponent({ props });

        instance.handleZoomButtonClick(false);

        expect(mockYaPlayer.setSpan).toHaveBeenLastCalledWith([ 80, 80 ]);
        expect(instance.state).toMatchSnapshot();
    });
});

describe('метрика при вращении панорамы', () => {
    // лимиты тут такие
    //      по горизонтали 0.15 * 360 = 55 градусов
    //      по вертикали 0.15 * 180 = 27 градусов
    it('в десктопе отправляется при движении вправо вверх', () => {
        const { page } = shallowRenderComponent({ props });
        contextMock.metrika.sendPageEvent.mockClear();
        const painter = page.find('.PanoramaInteriorPainter');

        // не значительные смещения, не должна отправляться
        mockYaPlayer.getDirection.mockReturnValueOnce([ 0, 0 ]);
        painter.simulate('mouseDown');
        painter.simulate('mouseMove');
        mockYaPlayer.getDirection.mockReturnValueOnce([ 30, 20 ]);
        playerEventMap.directionchange();
        painter.simulate('mouseUp');
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(0);

        // значительные смещения, должна отправляться только для первого
        mockYaPlayer.getDirection.mockReturnValueOnce([ 0, 0 ]);
        painter.simulate('mouseDown');
        painter.simulate('mouseMove');
        mockYaPlayer.getDirection.mockReturnValueOnce([ -10, -10 ]);
        playerEventMap.directionchange();
        mockYaPlayer.getDirection.mockReturnValueOnce([ 55, 30 ]);
        playerEventMap.directionchange();
        mockYaPlayer.getDirection.mockReturnValueOnce([ -55, -30 ]);
        playerEventMap.directionchange();
        painter.simulate('mouseUp');

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(2);
        expect(contextMock.metrika.sendPageEvent.mock.calls).toMatchSnapshot();
    });

    it('в десктопе отправляется при движении влево вниз', () => {
        const { page } = shallowRenderComponent({ props });
        contextMock.metrika.sendPageEvent.mockClear();
        const painter = page.find('.PanoramaInteriorPainter');

        mockYaPlayer.getDirection.mockReturnValueOnce([ 0, 0 ]);
        painter.simulate('mouseDown');
        painter.simulate('mouseMove');
        mockYaPlayer.getDirection.mockReturnValueOnce([ -55, -30 ]);
        playerEventMap.directionchange();
        painter.simulate('mouseUp');

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(2);
        expect(contextMock.metrika.sendPageEvent.mock.calls).toMatchSnapshot();
    });

    it('в таче отправляется при движении вправо вверх', () => {
        props.isTouch = true;
        const { page } = shallowRenderComponent({ props });
        contextMock.metrika.sendPageEvent.mockClear();
        const painter = page.find('.PanoramaInteriorPainter');

        // не значительные смещения, не должна отправляться
        mockYaPlayer.getDirection.mockReturnValueOnce([ 0, 0 ]);
        painter.simulate('touchStart');
        painter.simulate('touchMove');
        mockYaPlayer.getDirection.mockReturnValueOnce([ 30, 20 ]);
        playerEventMap.directionchange();
        painter.simulate('touchEnd');
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(0);

        // значительные смещения, должна отправляться только для первого
        mockYaPlayer.getDirection.mockReturnValueOnce([ 0, 0 ]);
        painter.simulate('touchStart');
        painter.simulate('touchMove');
        mockYaPlayer.getDirection.mockReturnValueOnce([ -10, -10 ]);
        playerEventMap.directionchange();
        mockYaPlayer.getDirection.mockReturnValueOnce([ 55, 30 ]);
        playerEventMap.directionchange();
        mockYaPlayer.getDirection.mockReturnValueOnce([ -55, -30 ]);
        playerEventMap.directionchange();
        painter.simulate('touchEnd');

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(2);
        expect(contextMock.metrika.sendPageEvent.mock.calls).toMatchSnapshot();
    });

    it('в таче отправляется при движении влево вниз', () => {
        props.isTouch = true;
        const { page } = shallowRenderComponent({ props });
        contextMock.metrika.sendPageEvent.mockClear();
        const painter = page.find('.PanoramaInteriorPainter');

        mockYaPlayer.getDirection.mockReturnValueOnce([ 0, 0 ]);
        painter.simulate('touchStart');
        painter.simulate('touchMove');
        mockYaPlayer.getDirection.mockReturnValueOnce([ -55, -30 ]);
        playerEventMap.directionchange();
        painter.simulate('touchEnd');

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(2);
        expect(contextMock.metrika.sendPageEvent.mock.calls).toMatchSnapshot();
    });
});

function shallowRenderComponent({ props }: { props: Props }) {
    class NonThrottledComponent extends PanoramaInteriorPainter {
        throttledHandleMouseMove = this.handleMouseMove as DebouncedFunc<() => void>;
    }

    const page = shallow(
        <NonThrottledComponent { ...props }/>,
        { context: contextMock, lifecycleExperimental: true },
    );
    const instance = page.instance() as PanoramaInteriorPainter;

    return { page, instance };
}

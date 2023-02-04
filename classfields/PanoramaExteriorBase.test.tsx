/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('../utils/loader', () => {
    return {
        createOrGetLoader: jest.fn(),
        removeLoader: jest.fn(),
    };
});
jest.mock('auto-core/react/lib/onDomContentLoaded', () => {
    return (callback: () => void) => callback();
});

import React from 'react';
import { shallow } from 'enzyme';
import _ from 'lodash';
import { InView } from 'react-intersection-observer';

import contextMock from 'autoru-frontend/mocks/contextMock';

import panoramaExteriorMock from 'auto-core/models/panoramaExterior/mocks';

import type { PanoramaFrame } from '../types';
import loader from '../utils/loader';
import type { InjectedProps } from '../PanoramaExteriorPainter/PanoramaExteriorPainter';

import type { BaseProps, BaseState } from './PanoramaExteriorBase';
import PanoramaExteriorBase from './PanoramaExteriorBase';

type Props = BaseProps & {
    isAddHotSpotMode: boolean;
    isReadyToPlay: boolean;
}

// мокаем то, что возвращает лоадер
const images = _.range(100).map((index) => {
    const image = new Image() as PanoramaFrame;
    image.isLoaded = true;
    image.src = 'http://autoru-panorama.s3.yandex.net/artifacts/257/039/2570394992-1571260340412-jwU9D/image_jpeg/v1/1200_0000.jpeg'
        .replace(/_(0000)\./, `_${ _.padStart(String(index), 4, '0') }.`);

    return image;
});
const loadPromise = Promise.resolve(images);
type Loader = {
    load: () => Promise<Array<PanoramaFrame>>;
}

const createLoaderMock = loader.createOrGetLoader as unknown as jest.MockedFunction<() => Loader>;
const removeLoaderMock = loader.removeLoader as unknown as jest.MockedFunction<() => void>;
const loadMock = jest.fn(() => loadPromise);
createLoaderMock.mockReturnValue({
    load: loadMock,
});

// делаем компонент который реализует данный класс
class ComponentMock extends PanoramaExteriorBase<Props, BaseState> {
    constructor(props: Props) {
        super(props);

        this.state = {
            ...this.state,
            isReadyToPlay: props.isReadyToPlay,
        };
    }

    isAddHotSpotMode() {
        return this.props.isAddHotSpotMode;
    }

    isFrozen() {
        return false;
    }

    isInGallery = false;

    isZoomOnScrollEnabled = false;

    shouldSpinOnScroll() {
        return false;
    }

    renderHotSpots() {
        return <div className="PanoramaExteriorHotSpots"/>;
    }

    renderZoomControls() {
        return null;
    }

    renderPullToSlideAnimation() {
        return null;
    }

    getComponentClassNames() {
        return '';
    }

    getPainterClassName() {
        return '';
    }

    getPreviewSourceData() {
        return {
            count: 100,
            high_res_first_frame: 'http://autoru-panorama.s3.yandex.net/artifacts/257/039/2570394992-1571260340412-jwU9D/image_jpeg/v1/1920_0000.jpeg',
            full_first_frame: 'http://autoru-panorama.s3.yandex.net/artifacts/257/039/2570394992-1571260340412-jwU9D/image_jpeg/v1/1200_0000.jpeg',
            preview_first_frame: 'http://autoru-panorama.s3.yandex.net/artifacts/257/039/2570394992-1571260340412-jwU9D/image_jpeg/v1/320_0000.jpeg',
        };
    }

    hidePressAndSpin = jest.fn()
}

const injectPainterPropsMock = {
    isMaxZoomed: false,
    isMinZoomed: true,
    onZoomButtonClick: _.noop,
    playPanoramaTo: _.noop,
    animateZoomOut: _.noop,
    registerOnPaintCallback: _.noop,
    unregisterOnPaintCallback: _.noop,
    getScene: _.noop,
    getCanvasRect: _.noop,
    onMouseDown: _.noop,
    onTouchStart: _.noop,
    onTouchMove: _.noop,
} as InjectedProps;

declare var global: {
    Ya: Record<string, any>;
    performance?: Performance;
};

let props: Props;
let originalWindowYa: any;
let originalWindowPerformance: Performance | undefined;

beforeEach(() => {
    props = {
        data: panoramaExteriorMock.value(),
        hasWebpSupport: true,
        isAddHotSpotMode: false,
        isCurrentSlide: true,
        isPressAndSpinHidden: true,
        isTouch: false,
        widescreen: 'disallow',
        isWindows: false,
        loadStrategy: 'mount',
        isReadyToPlay: true,
        onDragStateChange: jest.fn(),
    };

    originalWindowYa = global.Ya;
    global.Ya = {
        Rum: {
            time: jest.fn(),
            timeEnd: jest.fn(),
            _deltaMarks: {},
        },
    };

    originalWindowPerformance = global.performance;
    delete global.performance;
    global.performance = {
        clearResourceTimings: jest.fn(),
        getEntriesByName: jest.fn(() => ([])),
    } as unknown as Performance;
});

afterEach(() => {
    jest.restoreAllMocks();

    global.Ya = originalWindowYa;
    global.performance = originalWindowPerformance;
});

it('во время загрузки покажет превью правильного фрейма', () => {
    const { page } = shallowRenderComponent({ props });
    const preview = page.find('.PanoramaExteriorBase__preview');

    expect(getImageIndexInPath(preview.prop('src') as string)).toBe('012');
});

it('во время загрузки hd-панорамы покажет превью правильного фрейма', () => {
    props.quality = 'hd';
    const { page } = shallowRenderComponent({ props });
    const preview = page.find('.PanoramaExteriorBase__preview');

    expect(preview.prop('src')).toMatch(/1920_0012\.jpeg$/);
});

it('если панорама грузится по скроллу, то начнет загрузку только после попадания блока во вьюпорт', () => {
    props.loadStrategy = 'intersection';
    const { page } = shallowRenderComponent({ props });

    expect(loadMock).toHaveBeenCalledTimes(0);

    const observer = page.find(InView);
    observer.simulate('change', true);

    expect(loadMock).toHaveBeenCalledTimes(1);
});

it('при смене id панорамы удалит предыдущую и загрузить новую', async() => {
    const { page } = shallowRenderComponent({ props });

    await loadPromise;

    createLoaderMock.mockClear();

    const newPanorama = { ...props.data, id: 'foo' };
    page.setProps({ data: newPanorama });

    expect(removeLoaderMock).toHaveBeenCalledTimes(1);
    expect(removeLoaderMock).toHaveBeenCalledWith(props.data, { hasWebpSupport: true, isWidescreen: false, quality: undefined, resolveOnDivisor: 2 });

    expect(createLoaderMock).toHaveBeenCalledTimes(1);
    expect(createLoaderMock).toHaveBeenCalledWith(newPanorama, { hasWebpSupport: true, isWidescreen: false, quality: undefined, resolveOnDivisor: 2 });
});

describe('лоадер', () => {
    it('покажет при загрузке', () => {
        const { page } = shallowRenderComponent({ props });

        const loader = page.find('.PanoramaExteriorBase__loader');
        expect(loader.isEmptyRender()).toBe(false);
    });

    it('скроет после загрузки, если панорама готова к проигрыванию', async() => {
        const { page } = shallowRenderComponent({ props });

        await loadPromise;

        const loader = page.find('.PanoramaExteriorBase__loader');

        expect(loader.isEmptyRender()).toBe(true);
    });

    it('будет показываться после загрузки, пока компонент не готов к проигрыванию', async() => {
        props.data = panoramaExteriorMock.withPoiCount(1).value();
        props.isReadyToPlay = false;
        const { componentInstance, page } = shallowRenderComponent({ props });

        await loadPromise;
        const loader = page.find('.PanoramaExteriorBase__loader');
        expect(loader.isEmptyRender()).toBe(false);

        componentInstance.handleReadyStateChange();

        const loaderUpdated = page.find('.PanoramaExteriorBase__loader');
        expect(loaderUpdated.isEmptyRender()).toBe(true);
    });
});

describe('компонент с точками', () => {
    it('не нарисует, если у панорамы нет точек', async() => {
        const { page } = shallowRenderComponent({ props });

        await loadPromise;

        const hotSpots = page.find('.PanoramaExteriorHotSpots');

        expect(hotSpots.isEmptyRender()).toBe(true);
    });

    it('нарисует, если у панорамы есть точки', async() => {
        props.data = panoramaExteriorMock.withPoiCount(1).value();
        const { page } = shallowRenderComponent({ props });

        await loadPromise;

        const hotSpots = page.find('PanoramaExteriorPainter').dive().find('.PanoramaExteriorHotSpots');

        expect(hotSpots.isEmptyRender()).toBe(false);
    });

    it('нарисует всегда для владельца', async() => {
        props.isOwner = true;
        const { page } = shallowRenderComponent({ props });

        await loadPromise;

        const hotSpots = page.find('PanoramaExteriorPainter').dive().find('.PanoramaExteriorHotSpots');

        expect(hotSpots.isEmptyRender()).toBe(false);
    });
});

describe('промо "нажми и вращай"', () => {
    beforeEach(() => {
        props.isPressAndSpinHidden = false;
    });

    it('показывается если все условия соблюдены', () => {
        const { componentInstance } = shallowRenderComponent({ props });

        return loadPromise
            .then(() => {
                const promo = componentInstance.renderPressAndSpinAnimation(injectPainterPropsMock);

                expect(promo).not.toBeNull();
            });
    });

    describe('не показывается', () => {
        it('если скрыли до этого', () => {
            props.isPressAndSpinHidden = true;
            const { componentInstance } = shallowRenderComponent({ props });

            return loadPromise
                .then(() => {
                    const promo = componentInstance.renderPressAndSpinAnimation(injectPainterPropsMock);

                    expect(promo).toBeNull();
                });
        });

        it('если панорама еще не загрузилась', () => {
            const { componentInstance } = shallowRenderComponent({ props });

            const promo = componentInstance.renderPressAndSpinAnimation(injectPainterPropsMock);

            expect(promo).toBeNull();
        });

        it('если панорама еще не готова проигрываться', () => {
            const { componentInstance } = shallowRenderComponent({ props });
            componentInstance.setState({ isLoaded: true, isReadyToPlay: false });

            const promo = componentInstance.renderPressAndSpinAnimation(injectPainterPropsMock);

            expect(promo).toBeNull();
        });
    });

    describe('при опускании мыши на панораму', () => {
        it('вызовет метод если клик был по отрисовщику', () => {
            const { componentInstance, page } = shallowRenderComponent({ props });

            return loadPromise
                .then(() => {
                    page.simulate('mouseDown', {
                        stopPropagation: _.noop,
                        target: { closest: () => true },
                    });

                    expect(componentInstance.hidePressAndSpin).toHaveBeenCalledTimes(1);
                });
        });

        it('не вызовет метод если клик был не по отрисовщику', () => {
            const { componentInstance, page } = shallowRenderComponent({ props });

            return loadPromise
                .then(() => {
                    page.simulate('mouseDown', {
                        stopPropagation: _.noop,
                        target: { closest: () => false },
                    });

                    expect(componentInstance.hidePressAndSpin).toHaveBeenCalledTimes(0);
                });
        });
    });

    describe('при опускании пальца на панораму', () => {
        it('вызовет метод если тап был по отрисовщику', () => {
            const { componentInstance, page } = shallowRenderComponent({ props });

            return loadPromise
                .then(() => {
                    page.simulate('touchStart', {
                        stopPropagation: _.noop,
                        target: { closest: () => true },
                    });

                    expect(componentInstance.hidePressAndSpin).toHaveBeenCalledTimes(1);
                });
        });

        it('не вызовет метод если тап был не по отрисовщику', () => {
            const { componentInstance, page } = shallowRenderComponent({ props });

            return loadPromise
                .then(() => {
                    page.simulate('touchStart', {
                        stopPropagation: _.noop,
                        target: { closest: () => false },
                    });

                    expect(componentInstance.hidePressAndSpin).toHaveBeenCalledTimes(0);
                });
        });
    });
});

function getImageIndexInPath(str: string) {
    return str.slice(-8, -5);
}

function shallowRenderComponent({ props }: { props: Props }) {
    const page = shallow(
        <ComponentMock { ...props }/>
        ,
        { context: contextMock, lifecycleExperimental: true },
    );

    const componentInstance = page.instance() as ComponentMock;

    componentInstance.container = {
        current: {
            style: {},
        },
    } as React.RefObject<HTMLDivElement>;

    return {
        page,
        componentInstance,
    };
}

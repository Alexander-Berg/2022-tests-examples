/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/dataDomain/panoramaHotSpots/actions/fetchAllHotSpots');

import React from 'react';
import type { ShallowWrapper } from 'enzyme';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';
import { getBunkerMock } from 'autoru-frontend/mockData/state/bunker.mock';

import fetchAllHotSpots from 'auto-core/react/dataDomain/panoramaHotSpots/actions/fetchAllHotSpots';
import type { PanoramaScene } from 'auto-core/react/components/common/Panorama/PanoramaExterior/types';
import type { PanoramaHotSpotPresent } from 'auto-core/react/dataDomain/panoramaHotSpots/types';
import { panoramaHotSpotsStateMock, panoramaHotSpotMock } from 'auto-core/react/dataDomain/panoramaHotSpots/mocks';

import type { OwnProps, AppState, AbstractState, AbstractProps } from './PanoramaHotSpots';
import PanoramaHotSpots from './PanoramaHotSpots';

const fetchHotSpotsPromise = Promise.resolve();
const fetchAllHotSpotsMock = fetchAllHotSpots as jest.MockedFunction<typeof fetchAllHotSpots>;
fetchAllHotSpotsMock.mockReturnValue((() => fetchHotSpotsPromise));

const sceneMock = { x: 0, y: 0, frame: 42, scale: 1 };

type Props = AbstractProps & {
    store: any;
}
const spinPanoramaToBestFramePromise = Promise.resolve();
const eventMock = { stopPropagation: jest.fn() };
const spotMock = panoramaHotSpotMock.value();

class ComponentMock extends PanoramaHotSpots<Props, AbstractState> {
    renderPromo = () => null
    repositionSpot = jest.fn()
    getCurrentFrameSpot = jest.fn()
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    spinPanoramaToBestFrame = jest.fn((spotId: string) => spinPanoramaToBestFramePromise)
    spinPanoramaToNearestFrameWithHotSpot = jest.fn(() => Promise.resolve())
}

let paintCallbacks: Array<(scene: PanoramaScene) => void>;
let props: OwnProps;
let initialState: AppState;

beforeEach(() => {
    paintCallbacks = [];

    props = {
        hasPromo: false,
        place: 'card_gallery',
        isOwner: true,
        isReadyToPlay: true,
        panoramaId: 'my-panorama',
        onReady: jest.fn(),
        getScene: jest.fn(() => sceneMock),
        registerOnPaintCallback: jest.fn((callback: (scene: PanoramaScene) => void) => {
            paintCallbacks.push(callback);
        }),
        showControls: true,
    };

    initialState = {
        bunker: getBunkerMock([ 'common/panorama_poi' ]),
        cookies: {},
        panoramaHotSpots: panoramaHotSpotsStateMock.withSpots([ spotMock ], props.panoramaId).value(),
    };

    jest.useFakeTimers();
});

describe('переключатель режима видимости', () => {
    it('не покажется при маунте если точки не видны', () => {
        const { componentInstance, page } = shallowRenderComponent({ props, initialState });
        componentInstance.getCurrentFrameSpot.mockImplementationOnce(() => false);
        paintCallbacks.forEach((callback) => callback({ frame: 0, x: 0, y: 0, scale: 1 }));
        const switcher = page.find('.PanoramaHotSpotsMobile__switcher');

        expect(switcher.isEmptyRender()).toBe(true);
    });

    it('покажется когда панорама довращается до фрейм с точкой', () => {
        const { componentInstance, page } = shallowRenderComponent({ props, initialState });
        componentInstance.getCurrentFrameSpot.mockImplementationOnce(() => true);
        paintCallbacks.forEach((callback) => callback({ frame: 47, x: 0, y: 0, scale: 1 }));
        const switcher = page.find('.PanoramaHotSpotsMobile__switcher');

        expect(switcher.isEmptyRender()).toBe(false);
    });

    it('не покажется если передан флаг', () => {
        props.showControls = false;
        const { componentInstance, page } = shallowRenderComponent({ props, initialState });
        componentInstance.getCurrentFrameSpot.mockImplementationOnce(() => true);
        paintCallbacks.forEach((callback) => callback({ frame: 47, x: 0, y: 0, scale: 1 }));
        const switcher = page.find('.PanoramaHotSpotsMobile__switcher');

        expect(switcher.isEmptyRender()).toBe(true);
    });

    describe('при клике на контрол', () => {
        let wrapper: {
            page: ShallowWrapper;
            componentInstance: ComponentMock;
        };

        beforeEach(() => {
            wrapper = shallowRenderComponent({ props, initialState });
            wrapper.componentInstance.getCurrentFrameSpot.mockImplementationOnce(() => true);
            paintCallbacks.forEach((callback) => callback({ frame: 47, x: 0, y: 0, scale: 1 }));

            const switcher = wrapper.page.find('.PanoramaHotSpotsMobile__switcher');
            switcher.simulate('click', eventMock);
        });

        it('изменит состояние переключателя', () => {
            const switcher = wrapper.page.find('.PanoramaHotSpotsMobile__switcher');
            expect(switcher.prop('type')).toBe('visibility_off');
        });

        it('отправит метрику', () => {
            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'panoramas_poi', 'buyer', 'switch_off' ]);
        });

        it('если точки стали видимыми, перпозиционирует их', () => {
            wrapper.componentInstance.repositionSpot.mockClear();
            const switcher = wrapper.page.find('.PanoramaHotSpotsMobile__switcher');
            switcher.simulate('click', eventMock);

            expect(wrapper.componentInstance.repositionSpot).toHaveBeenCalledTimes(1);
            expect(wrapper.componentInstance.repositionSpot).toHaveBeenCalledWith(spotMock, sceneMock);
        });
    });
});

describe('при маунте', () => {
    describe('если нет точек', () => {
        beforeEach(() => {
            initialState.panoramaHotSpots = panoramaHotSpotsStateMock.value();
        });

        it('загрузит их', () => {
            shallowRenderComponent({ props, initialState });

            expect(fetchAllHotSpotsMock).toHaveBeenCalledTimes(1);
            expect(fetchAllHotSpotsMock).toHaveBeenCalledWith(props.panoramaId);
        });

        it('после загрузки вызовет проп onReady', () => {
            shallowRenderComponent({ props, initialState });

            return fetchHotSpotsPromise
                .then(() => {
                    expect(props.onReady).toHaveBeenCalledTimes(1);
                });
        });
    });

    describe('если есть точки', () => {
        it('вызовет проп onReady', () => {
            const { componentInstance } = shallowRenderComponent({ props, initialState });
            expect(props.onReady).toHaveBeenCalledTimes(1);
            expect(componentInstance.spinPanoramaToBestFrame).toHaveBeenCalledTimes(0);
            expect(componentInstance.spinPanoramaToNearestFrameWithHotSpot).toHaveBeenCalledTimes(0);
        });

        it('в промо подкрутит панораму к ближайшему фрейму с точкой', () => {
            props.place = 'promo';
            const { componentInstance } = shallowRenderComponent({ props, initialState });
            expect(componentInstance.spinPanoramaToBestFrame).toHaveBeenCalledTimes(0);
            expect(componentInstance.spinPanoramaToNearestFrameWithHotSpot).toHaveBeenCalledTimes(1);
        });

        it('в журнале если передан айди точки для портала, подкрутит к точке и откроет портал', () => {
            props.place = 'mag_gallery';
            props.portalSpotId = '1601895867643-qK9cT';
            const { componentInstance, page } = shallowRenderComponent({ props, initialState });

            expect(componentInstance.spinPanoramaToBestFrame).toHaveBeenCalledTimes(1);

            return spinPanoramaToBestFramePromise
                .then(() => {
                    const portal = page.find('PanoramaHotSpotPortal');
                    expect(portal.prop('isOpened')).toBe(true);
                    expect((portal.prop('spot') as PanoramaHotSpotPresent).point.id).toBe(props.portalSpotId);
                });
        });
    });
});

describe('клик по точке', () => {
    const spotId = '1601895867643-qK9cT';
    const eventMock = {
        stopPropagation: jest.fn(),
        currentTarget: {
            getAttribute: () => spotId,
        },
    };

    it('вызовет stopPropagation', () => {
        const { page } = shallowRenderComponent({ props, initialState });
        const spot = page.find('.PanoramaHotSpotsMobile__spot');
        spot.simulate('click', eventMock);

        expect(eventMock.stopPropagation).toHaveBeenCalledTimes(1);
    });

    it('подкрутит панораму до лучшего фрейма и откроет портал', () => {
        const { componentInstance, page } = shallowRenderComponent({ props, initialState });
        const spot = page.find('.PanoramaHotSpotsMobile__spot');
        spot.simulate('click', eventMock);

        expect(componentInstance.spinPanoramaToBestFrame).toHaveBeenCalledTimes(1);
        expect(componentInstance.spinPanoramaToBestFrame).toHaveBeenCalledWith(spotId);

        return spinPanoramaToBestFramePromise
            .then(() => {
                const portal = page.find('PanoramaHotSpotPortal');
                expect(portal.prop('isOpened')).toBe(true);
                expect((portal.prop('spot') as PanoramaHotSpotPresent).point.id).toBe(spotId);
            });
    });

    it('отправит метрику', () => {
        const { page } = shallowRenderComponent({ props, initialState });
        const spot = page.find('.PanoramaHotSpotsMobile__spot');
        spot.simulate('click', eventMock);

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'panoramas_poi', 'buyer', 'tap' ]);
    });

    it('в промке не будет ничего крутить и открывать, не будет отправлять метрику', () => {
        props.place = 'promo';
        const { componentInstance, page } = shallowRenderComponent({ props, initialState });
        const spot = page.find('.PanoramaHotSpotsMobile__spot');
        spot.simulate('click', eventMock);

        expect(eventMock.stopPropagation).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(0);
        expect(componentInstance.spinPanoramaToBestFrame).toHaveBeenCalledTimes(0);
    });

    it('вне галереи только отправит метрику', () => {
        props.place = 'mag_page';
        const { componentInstance, page } = shallowRenderComponent({ props, initialState });
        const spot = page.find('.PanoramaHotSpotsMobile__spot');
        spot.simulate('click', eventMock);

        expect(eventMock.stopPropagation).toHaveBeenCalledTimes(0);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(componentInstance.spinPanoramaToBestFrame).toHaveBeenCalledTimes(0);
    });
});

function shallowRenderComponent({ initialState, props }: { props: OwnProps; initialState: AppState }) {
    const store = mockStore(initialState);

    const ConnectedComponentMock = ComponentMock.connector(ComponentMock);

    const page = shallow(
        <ConnectedComponentMock { ...props } store={ store }/>
        ,
        { disableLifecycleMethods: true, context: contextMock },
    ).dive();

    const componentInstance = page.instance() as ComponentMock;

    (componentInstance as ComponentMock).spotRefs = {
        '1601895867643-qK9cT': {
            style: {},
        } as HTMLDivElement,
    };

    if (typeof componentInstance.componentDidMount === 'function') {
        componentInstance.componentDidMount();
    }

    return {
        page,
        componentInstance,
    };
}

/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';
import { getBunkerMock } from 'autoru-frontend/mockData/state/bunker.mock';

import type { PanoramaScene } from 'auto-core/react/components/common/Panorama/PanoramaExterior/types';
import type { OwnProps as OwnPropsAbstract, AppState, AbstractState, AbstractProps }
    from 'auto-core/react/components/mobile/PanoramaHotSpots/PanoramaHotSpots';
import type PanoramaHotSpots from 'auto-core/react/components/mobile/PanoramaHotSpots/PanoramaHotSpots';
import { panoramaHotSpotsStateMock, panoramaHotSpotMock } from 'auto-core/react/dataDomain/panoramaHotSpots/mocks';

import panoramaExteriorMock from 'auto-core/models/panoramaExterior/mocks';

import type { OwnProps } from './PanoramaExteriorHotSpots';
import PanoramaExteriorHotSpots from './PanoramaExteriorHotSpots';

const spotMock = panoramaHotSpotMock.value();
const sceneMock = { x: -50, y: -100, frame: 47, scale: 1.5 };
const spotClickerMock = { style: {} };
const animateZoomOutPromise = Promise.resolve();
const playPanoramaToPromise = Promise.resolve();

let paintCallbacks: Array<(scene: Readonly<PanoramaScene>) => void>;

let props: OwnProps & OwnPropsAbstract;
let initialState: AppState;

beforeEach(() => {
    paintCallbacks = [];

    props = {
        hasPromo: false,
        place: 'card_gallery',
        isOwner: true,
        isReadyToPlay: true,
        format: 'r4x3',
        panoramaId: 'my-panorama',
        onReady: jest.fn(),
        getScene: jest.fn(() => (sceneMock)),
        registerOnPaintCallback: jest.fn((callback: (scene: Readonly<PanoramaScene>) => void) => {
            paintCallbacks.push(callback);
        }),
        getCanvasRect: () => ({
            width: 800,
            height: 600,
            top: 150,
            left: 100,
        } as ClientRect),
        animateZoomOut: jest.fn(() => animateZoomOutPromise),
        playPanoramaTo: jest.fn(() => playPanoramaToPromise),
        panorama: panoramaExteriorMock.value(),
    };

    initialState = {
        bunker: getBunkerMock([ 'common/panorama_poi' ]),
        cookies: {},
        panoramaHotSpots: panoramaHotSpotsStateMock
            .withSpots([ spotMock ], props.panoramaId)
            .value(),
    };

    jest.useFakeTimers();
});

it('правильно позиционирует точку и кликер', () => {
    const { componentInstance } = shallowRenderComponent({ props, initialState });

    paintCallbacks.forEach((callback) => callback(sceneMock));

    expect((componentInstance as any).spotRefs['1601895867643-qK9cT'].style).toMatchSnapshot();
    expect(spotClickerMock.style).toMatchSnapshot();
});

it('клик на точку отзумит панораму и провращает ее до лучшего фрейма', () => {
    const eventMock = {
        stopPropagation: jest.fn(),
        currentTarget: {
            getAttribute: () => spotMock.point.id,
        },
    };
    const { page } = shallowRenderComponent({ props, initialState });
    const spot = page.find('.PanoramaHotSpotsMobile__spot');
    spot.simulate('click', eventMock);

    expect(props.animateZoomOut).toHaveBeenCalledTimes(1);

    return animateZoomOutPromise
        .then(() => {
            expect(props.playPanoramaTo).toHaveBeenCalledTimes(1);
            expect(props.playPanoramaTo).toHaveBeenCalledWith(48);
        });
});

it('при открытии в промо подкрутит панораму к ближайшей точке', () => {
    props.place = 'promo';
    initialState.panoramaHotSpots = panoramaHotSpotsStateMock
        .withSpots([
            panoramaHotSpotMock.withId('1').withFrames({
                r4x3: [
                    { frame: 12, coordinates: { x: 0.3, y: 0.5 } },
                    { frame: 13, coordinates: { x: 0.4, y: 0.5 } },
                ],
            }).value(),
            panoramaHotSpotMock.withId('2').withFrames({
                r4x3: [
                    { frame: 52, coordinates: { x: 0.9, y: 0.5 } },
                    { frame: 54, coordinates: { x: 0.8, y: 0.5 } },
                    { frame: 55, coordinates: { x: 0.7, y: 0.5 } },
                ],
            }).value(),
        ], props.panoramaId)
        .value(),
    shallowRenderComponent({ props, initialState });

    expect(props.playPanoramaTo).toHaveBeenCalledTimes(1);
    expect(props.playPanoramaTo).toHaveBeenCalledWith(55);
});

function shallowRenderComponent({ initialState, props }: { props: OwnProps & OwnPropsAbstract; initialState: AppState }) {
    const store = mockStore(initialState);

    const page = shallow(
        <Provider store={ store }>
            <PanoramaExteriorHotSpots { ...props }/>
        </Provider>
        ,
        { context: contextMock },
    ).dive().dive();

    const componentInstance = page.instance();

    (componentInstance as PanoramaHotSpots<AbstractProps, AbstractState>).spotRefs = {
        '1601895867643-qK9cT': {
            style: {},
            querySelector: () => spotClickerMock,
        } as unknown as HTMLDivElement,
    };

    return {
        page,
        componentInstance,
    };
}

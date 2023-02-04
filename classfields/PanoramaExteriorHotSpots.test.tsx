/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */
jest.mock('auto-core/react/dataDomain/panoramaHotSpots/actions/createNewHotSpot');
jest.mock('auto-core/react/dataDomain/panoramaHotSpots/actions/editHotSpot');

import React from 'react';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';
import { getBunkerMock } from 'autoru-frontend/mockData/state/bunker.mock';

import type { PanoramaScene } from 'auto-core/react/components/common/Panorama/PanoramaExterior/types';
import type { ActionNewHotSpotCreate } from 'auto-core/react/dataDomain/panoramaHotSpots/types';
import createNewHotSpot from 'auto-core/react/dataDomain/panoramaHotSpots/actions/createNewHotSpot';
import editHotSpot from 'auto-core/react/dataDomain/panoramaHotSpots/actions/editHotSpot';
import type { OwnProps as OwnPropsAbstract, AppState, AbstractState, AbstractProps }
    from 'auto-core/react/components/desktop/PanoramaHotSpots/PanoramaHotSpots';
import type PanoramaHotSpots from 'auto-core/react/components/desktop/PanoramaHotSpots/PanoramaHotSpots';
import { panoramaHotSpotsStateMock, panoramaHotSpotMock } from 'auto-core/react/dataDomain/panoramaHotSpots/mocks';

import type { OwnProps } from './PanoramaExteriorHotSpots';
import PanoramaExteriorHotSpots from './PanoramaExteriorHotSpots';

const createNewHotSpotMock = createNewHotSpot as jest.MockedFunction<typeof createNewHotSpot>;
createNewHotSpotMock.mockReturnValue((() => () => { }) as unknown as ActionNewHotSpotCreate);

const editHotSpotPromise = Promise.resolve();
const editHotSpotMock = editHotSpot as jest.MockedFunction<typeof editHotSpot>;
editHotSpotMock.mockReturnValue((() => editHotSpotPromise));

const spot = panoramaHotSpotMock.value();
const eventMock = {
    stopPropagation: jest.fn(),
    clientX: 800,
    clientY: 300,
} as unknown as React.MouseEvent;

let paintCallbacks: Array<(scene: Readonly<PanoramaScene>) => void>;
let clickCallbacks: Array<(event: React.MouseEvent) => void>;

let props: OwnProps & OwnPropsAbstract;
let initialState: AppState;

beforeEach(() => {
    clickCallbacks = [];
    paintCallbacks = [];

    props = {
        hasPromo: false,
        isAddMode: false,
        isCurrentSlide: true,
        isFrozen: false,
        isOwner: true,
        isTouch: false,
        panoramaId: 'my-panorama',
        format: 'r4x3',
        onAddModeToggle: jest.fn(),
        onFrozenStateChange: jest.fn(),
        onReady: jest.fn(),
        getScene: jest.fn(() => ({ x: -50, y: -100, frame: 47, scale: 1.5 })),
        registerOnPaintCallback: jest.fn((callback: (scene: Readonly<PanoramaScene>) => void) => {
            paintCallbacks.push(callback);
        }),
        registerOnClickCallback: jest.fn((callback: (event: React.MouseEvent) => void) => {
            clickCallbacks.push(callback);
        }),
        updateCanvasRect: jest.fn(() => ({
            width: 800,
            height: 600,
            left: 100,
            top: 150,
        } as DOMRect)),
    };

    initialState = {
        bunker: getBunkerMock([ 'common/panorama_poi' ]),
        cookies: {},
        panoramaHotSpots: panoramaHotSpotsStateMock
            .withSpots([ spot ], props.panoramaId)
            .value(),
    };

    jest.useFakeTimers();
});

it('правильно рисует сохраненную точку', () => {
    const { componentInstance } = shallowRenderComponent({ props, initialState });

    paintCallbacks.forEach((callback) => callback({ x: -50, y: -100, frame: 47, scale: 1.5 }));

    expect((componentInstance as any).spotRefs['1601895867643-qK9cT'].style).toMatchSnapshot();
});

it('правильно рисует сохраненную точку в формате 16х9', () => {
    props.format = 'r16x9';
    const { componentInstance } = shallowRenderComponent({ props, initialState });

    paintCallbacks.forEach((callback) => callback({ x: -50, y: -100, frame: 47, scale: 1.5 }));

    expect((componentInstance as any).spotRefs['1601895867643-qK9cT'].style).toMatchSnapshot();
});

it('правильно рисует новую точку', () => {
    initialState.panoramaHotSpots = panoramaHotSpotsStateMock
        .withSpots([
            panoramaHotSpotMock.withNewPoint().value(),
        ], props.panoramaId)
        .value();
    const { componentInstance } = shallowRenderComponent({ props, initialState });

    paintCallbacks.forEach((callback) => callback({ x: -50, y: -100, frame: 47, scale: 1.5 }));

    expect((componentInstance as any).spotRefs.new_spot.style).toMatchSnapshot();
});

it('правильно перерисовывает точку при дрэге', () => {
    const { componentInstance, page } = shallowRenderComponent({ props, initialState });

    const hotSpot = page.find('PanoramaHotSpot');
    hotSpot.simulate('dragStart');
    hotSpot.simulate('dragMove', spot, [ 100, 50 ]);

    expect((componentInstance as any).spotRefs['1601895867643-qK9cT'].style).toMatchSnapshot();
});

describe('простановка новой точки', () => {
    it('правильно ставит точку', () => {
        props.isAddMode = true;
        shallowRenderComponent({ props, initialState });

        clickCallbacks.forEach((callback) => callback(eventMock));

        expect(props.updateCanvasRect).toHaveBeenCalledTimes(1);
        expect(createNewHotSpotMock).toHaveBeenCalledTimes(1);
        expect(createNewHotSpotMock).toHaveBeenCalledWith({ aspectType: 'R4x3', frame: 47, x: 0.875, y: 0.25, panoramaId: 'my-panorama' });
    });
});

describe('перетаскивание текущей точки', () => {
    it('правильно ставит точку после перетаскивания', () => {
        initialState.panoramaHotSpots = panoramaHotSpotsStateMock
            .withSpots([ spot ], props.panoramaId)
            .value();
        const { page } = shallowRenderComponent({ props, initialState });
        const hotSpot = page.find('PanoramaHotSpot');
        hotSpot.simulate('dragStart');
        hotSpot.simulate('dragMove', spot, [ 100, 50 ]);
        hotSpot.simulate('dragEnd', spot, eventMock);

        expect(props.updateCanvasRect).toHaveBeenCalledTimes(1);
        expect(editHotSpotMock).toHaveBeenCalledTimes(1);
        expect(editHotSpotMock).toHaveBeenCalledWith(
            'my-panorama',
            {
                frame_info: { aspectType: 'R4x3', frame: 47 },
                point: {
                    coordinates: { x: 0.875, y: 0.25 },
                    id: '1601895867643-qK9cT',
                },
                properties: { text: { text: 'this is hot!' },
                },
            },
        );
    });
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
        } as HTMLDivElement,
        new_spot: {
            style: {},
        } as HTMLDivElement,
    };

    return {
        page,
        componentInstance,
    };
}

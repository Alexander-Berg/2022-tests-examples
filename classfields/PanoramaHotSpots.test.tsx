/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/dataDomain/panoramaHotSpots/actions/createNewHotSpot');
jest.mock('auto-core/react/dataDomain/panoramaHotSpots/actions/removeNewHotSpot');
jest.mock('auto-core/react/dataDomain/panoramaHotSpots/actions/postNewHotSpot');
jest.mock('auto-core/react/dataDomain/panoramaHotSpots/actions/editHotSpot');
jest.mock('auto-core/react/dataDomain/panoramaHotSpots/actions/deleteHotSpot');
jest.mock('auto-core/react/dataDomain/panoramaHotSpots/actions/updateHotSpot');

import React from 'react';
import type { ShallowWrapper } from 'enzyme';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';
import { getBunkerMock } from 'autoru-frontend/mockData/state/bunker.mock';

import type {
    ActionNewHotSpotCreate,
    ActionHotSpotsUpdateSpot,
    ActionNewHotSpotRemove,
    PanoramaAspectType,
    PanoramaHotSpot as TPanoramaHotSpot,
} from 'auto-core/react/dataDomain/panoramaHotSpots/types';
import createNewHotSpot from 'auto-core/react/dataDomain/panoramaHotSpots/actions/createNewHotSpot';
import postNewHotSpot from 'auto-core/react/dataDomain/panoramaHotSpots/actions/postNewHotSpot';
import editHotSpot from 'auto-core/react/dataDomain/panoramaHotSpots/actions/editHotSpot';
import deleteHotSpot from 'auto-core/react/dataDomain/panoramaHotSpots/actions/deleteHotSpot';
import updateHotSpot from 'auto-core/react/dataDomain/panoramaHotSpots/actions/updateHotSpot';
import removeNewHotSpot from 'auto-core/react/dataDomain/panoramaHotSpots/actions/removeNewHotSpot';
import { panoramaHotSpotsStateMock, panoramaHotSpotMock } from 'auto-core/react/dataDomain/panoramaHotSpots/mocks';
import cardMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import getImageUrlsRaw from 'auto-core/react/lib/offer/getImageUrlsRaw';

import type { OwnProps, AppState, AbstractState, AbstractProps } from './PanoramaHotSpots';
import PanoramaHotSpots from './PanoramaHotSpots';
import { PortalType } from './PanoramaHotSpotPortal/PanoramaHotSpotPortal';

const postNewHotSpotPromise = Promise.resolve();
const editHotSpotPromise = Promise.resolve();
const deleteHotSpotPromise = Promise.resolve();

const createNewHotSpotMock = createNewHotSpot as jest.MockedFunction<typeof createNewHotSpot>;
createNewHotSpotMock.mockReturnValue((() => () => { }) as unknown as ActionNewHotSpotCreate);

const postNewHotSpotMock = postNewHotSpot as jest.MockedFunction<typeof postNewHotSpot>;
postNewHotSpotMock.mockReturnValue((() => postNewHotSpotPromise));

const editHotSpotMock = editHotSpot as jest.MockedFunction<typeof editHotSpot>;
editHotSpotMock.mockReturnValue((() => editHotSpotPromise));

const deleteHotSpotMock = deleteHotSpot as jest.MockedFunction<typeof deleteHotSpot>;
deleteHotSpotMock.mockReturnValue((() => deleteHotSpotPromise));

const updateHotSpotMock = updateHotSpot as jest.MockedFunction<typeof updateHotSpot>;
updateHotSpotMock.mockReturnValue((() => () => { }) as unknown as ActionHotSpotsUpdateSpot);

const removeNewHotSpotMock = removeNewHotSpot as jest.MockedFunction<typeof removeNewHotSpot>;
removeNewHotSpotMock.mockReturnValue((() => () => { }) as unknown as ActionNewHotSpotRemove);

const sceneMock = { x: 0, y: 0, frame: 42, scale: 1 };
const newSpotParamsMock = { aspectType: 'R4x3' as PanoramaAspectType, frame: 42, x: 0.25, y: 0.5, panoramaId: 'my-panorama' };

const repositionSpotMock = jest.fn();
const getNewSpotParamsMock = jest.fn(() => newSpotParamsMock);
let clickCallbacks: Array<(event: React.MouseEvent) => void>;
let clickEventMock: React.MouseEvent;

type Props = AbstractProps & {
    store: any;
}

class ComponentMock extends PanoramaHotSpots<Props, AbstractState> {
    repositionSpot = repositionSpotMock

    getNewSpotParams = getNewSpotParamsMock
}

let props: OwnProps;
let initialState: AppState;

beforeEach(() => {
    clickCallbacks = [];

    props = {
        hasPromo: false,
        isAddMode: false,
        isCurrentSlide: true,
        isFrozen: false,
        isOwner: true,
        isTouch: false,
        panoramaId: 'my-panorama',
        onAddModeToggle: jest.fn(),
        onFrozenStateChange: jest.fn(),
        onReady: jest.fn(),
        getScene: jest.fn(() => sceneMock),
        registerOnPaintCallback: jest.fn(),
        registerOnClickCallback: jest.fn((callback: (event: React.MouseEvent) => void) => {
            clickCallbacks.push(callback);
        }),
    };

    initialState = {
        bunker: getBunkerMock([ 'common/panorama_poi' ]),
        cookies: {},
        panoramaHotSpots: panoramaHotSpotsStateMock.value(),
    };

    clickEventMock = {
        stopPropagation: jest.fn(),
        clientX: 0,
        clientY: 0,
    } as unknown as React.MouseEvent;

    jest.useFakeTimers();
});

describe('создание точки', () => {
    describe('при клике в режиме добавления точек', () => {
        beforeEach(() => {
            props.isAddMode = true;
            shallowRenderComponent({ props, initialState });
            clickCallbacks.forEach((callback) => callback(clickEventMock));
        });

        it('заморозит панораму и галерею', () => {
            expect(props.onFrozenStateChange).toHaveBeenCalledTimes(1);
            expect(props.onFrozenStateChange).toHaveBeenCalledWith(true);
        });

        it('создаст новую точку', () => {
            expect(createNewHotSpotMock).toHaveBeenCalledTimes(1);
            expect(createNewHotSpotMock).toHaveBeenCalledWith({ aspectType: 'R4x3', frame: 42, x: 0.25, y: 0.5, panoramaId: 'my-panorama' });
        });
    });

    it('при сохранении вызовет проп и передаст в него все параметры', () => {
        const newSpot = panoramaHotSpotMock.withNewPoint().value();
        initialState.panoramaHotSpots = panoramaHotSpotsStateMock
            .withSpots([ newSpot ], props.panoramaId)
            .value();
        const { componentInstance, page } = shallowRenderComponent({ props, initialState });

        simulateSpotSave({ page, componentInstance, text: 'comment-mock', photoUrl: 'photo-url-mock', spot: newSpot });

        expect(postNewHotSpotMock).toHaveBeenCalledTimes(1);
        expect(postNewHotSpotMock).toHaveBeenLastCalledWith({
            panoramaId: props.panoramaId,
            text: 'comment-mock',
            photoUrl: 'photo-url-mock',
        });
    });

    describe('при удачном сохранении', () => {
        const postNewHotSpotPromise = Promise.resolve();
        const newSpot = panoramaHotSpotMock.withNewPoint().value();

        beforeEach(() => {
            initialState.panoramaHotSpots = panoramaHotSpotsStateMock
                .withSpots([ newSpot ], props.panoramaId)
                .value();
            const { componentInstance, page } = shallowRenderComponent({ props, initialState });

            contextMock.metrika.sendPageEvent.mockClear();

            simulateSpotSave({ page, componentInstance, text: 'comment-mock', photoUrl: 'photo-url-mock', spot: newSpot });
        });

        it('разморозит панораму', () => {
            return postNewHotSpotPromise
                .then(() => {
                    expect(props.onFrozenStateChange).toHaveBeenCalledTimes(1);
                    expect(props.onFrozenStateChange).toHaveBeenLastCalledWith(false);
                });
        });

        it('выйдет из режима добавления точек', () => {
            return postNewHotSpotPromise
                .then(() => {
                    expect(props.onFrozenStateChange).toHaveBeenCalledTimes(1);
                });
        });

        it('отправит метрику', () => {
            return postNewHotSpotPromise
                .then(() => {
                    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
                    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'panoramas_poi', 'add', 'comment_photo' ]);
                });
        });
    });

    it('если фото привязано из оффера отправит правильную метрику', () => {
        const postNewHotSpotPromise = Promise.resolve();
        const newSpot = panoramaHotSpotMock.withNewPoint().value();
        initialState.panoramaHotSpots = panoramaHotSpotsStateMock
            .withSpots([ newSpot ], props.panoramaId)
            .value();
        props.relatedImages = getImageUrlsRaw(cardMock);

        const { componentInstance, page } = shallowRenderComponent({ props, initialState });

        contextMock.metrika.sendPageEvent.mockClear();

        simulateSpotSave({ page, componentInstance, text: '', photoUrl: 'https:picture-of-cat', spot: newSpot });

        return postNewHotSpotPromise
            .then(() => {
                expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
                expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'panoramas_poi', 'add', 'offer_photo' ]);
            });
    });

    describe('при неудачном сохранении', () => {
        let wrapper: ShallowWrapper;
        const postNewHotSpotPromise = Promise.reject();
        const newSpot = panoramaHotSpotMock.withNewPoint().value();

        beforeEach(() => {
            initialState.panoramaHotSpots = panoramaHotSpotsStateMock
                .withSpots([ newSpot ], props.panoramaId)
                .value();
            postNewHotSpotMock.mockReturnValueOnce(() => postNewHotSpotPromise);
            const { componentInstance, page } = shallowRenderComponent({ props, initialState });
            wrapper = page;

            contextMock.metrika.sendPageEvent.mockClear();

            simulateSpotSave({ page, componentInstance, text: 'comment-mock', photoUrl: 'photo-url-mock', spot: newSpot });
        });

        it('разморозит панораму', () => {
            return postNewHotSpotPromise
                .catch(() => { })
                .then(() => {
                    expect(props.onFrozenStateChange).toHaveBeenCalledTimes(1);
                    expect(props.onFrozenStateChange).toHaveBeenLastCalledWith(false);
                });
        });

        it('покажет портал с ошибкой', () => {
            const portal = wrapper.find('PanoramaHotSpotPortal');
            expect(portal.prop('type')).toBe(PortalType.ERROR_POPUP);
            expect((portal.prop('spot') as TPanoramaHotSpot).point.id).toBe('new_spot');
            expect(portal.prop('isOpened')).toBe(true);
        });

        it('отправит метрику', () => {
            return postNewHotSpotPromise
                .catch(() => { })
                .then(() => {
                    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
                    expect(contextMock.metrika.sendPageEvent).toHaveBeenLastCalledWith([ 'panoramas_poi', 'add', 'error' ]);
                });
        });
    });
});

describe('редактирование точки', () => {
    it('обновит данные точки при вводе', () => {
        const spot = panoramaHotSpotMock.value();
        initialState.panoramaHotSpots = panoramaHotSpotsStateMock
            .withSpots([ spot ], props.panoramaId)
            .value();
        const { componentInstance, page } = shallowRenderComponent({ props, initialState });

        componentInstance.setState({ isPortalOpened: true, portalType: PortalType.COMMENT_POPUP, portalSpotId: spot.point.id });

        const portal = page.find('PanoramaHotSpotPortal');
        portal.simulate('spotUpdate', spot, 'new-text');

        (spot.properties.text || { text: 'foo' }).text = 'new-text';

        expect(updateHotSpotMock).toHaveBeenCalledTimes(1);
        expect(updateHotSpotMock).toHaveBeenCalledWith(spot, props.panoramaId);
    });

    describe('при удачном сохранении', () => {
        let wrapper: ShallowWrapper;
        const spot = panoramaHotSpotMock.value();

        beforeEach(() => {
            initialState.panoramaHotSpots = panoramaHotSpotsStateMock
                .withSpots([ spot ], props.panoramaId)
                .value();

            const { componentInstance, page } = shallowRenderComponent({ props, initialState });
            wrapper = page;

            contextMock.metrika.sendPageEvent.mockClear();

            simulateSpotSave({ page, componentInstance, text: 'new-text', photoUrl: 'new-photo-url', spot });
        });

        it('вызовет проп с правильными параметрами', () => {
            expect(editHotSpotMock).toHaveBeenCalledTimes(1);
            expect(editHotSpotMock.mock.calls[0]).toMatchSnapshot();
        });

        it('разморозит панораму', () => {
            return editHotSpotPromise
                .then(() => {
                    expect(props.onFrozenStateChange).toHaveBeenCalledTimes(1);
                    expect(props.onFrozenStateChange).toHaveBeenLastCalledWith(false);
                });
        });

        it('скроет модал', () => {
            return editHotSpotPromise
                .then(() => {
                    const portal = wrapper.find('PanoramaHotSpotPortal');
                    expect(portal.prop('isOpened')).toBe(false);
                });
        });

        it('отправит метрику', () => {
            return editHotSpotPromise
                .then(() => {
                    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
                    expect(contextMock.metrika.sendPageEvent).toHaveBeenLastCalledWith([ 'panoramas_poi', 'edit', 'comment_photo' ]);
                });
        });
    });

    it('если фото привязано из оффера отправит правильную метрику', () => {
        const spot = panoramaHotSpotMock.value();
        initialState.panoramaHotSpots = panoramaHotSpotsStateMock
            .withSpots([ spot ], props.panoramaId)
            .value();
        props.relatedImages = getImageUrlsRaw(cardMock);

        const { componentInstance, page } = shallowRenderComponent({ props, initialState });

        contextMock.metrika.sendPageEvent.mockClear();

        simulateSpotSave({ page, componentInstance, text: 'new-text', photoUrl: 'https:picture-of-cat', spot });

        return editHotSpotPromise
            .then(() => {
                expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
                expect(contextMock.metrika.sendPageEvent).toHaveBeenLastCalledWith([ 'panoramas_poi', 'edit', 'comment_offer_photo' ]);
            });
    });

    describe('при неудачном сохранении', () => {
        let wrapper: ShallowWrapper;
        const spot = panoramaHotSpotMock.value();
        const editHotSpotPromise = Promise.reject();

        beforeEach(() => {
            initialState.panoramaHotSpots = panoramaHotSpotsStateMock
                .withSpots([ spot ], props.panoramaId)
                .value();
            editHotSpotMock.mockReturnValueOnce(() => editHotSpotPromise);

            const { componentInstance, page } = shallowRenderComponent({ props, initialState });
            wrapper = page;

            contextMock.metrika.sendPageEvent.mockClear();

            simulateSpotSave({ page, componentInstance, text: 'new-text', photoUrl: 'new-photo-url', spot });
        });

        it('откроет модал', () => {
            return editHotSpotPromise
                .catch(() => { })
                .then(() => {
                    const portal = wrapper.find('PanoramaHotSpotPortal');
                    expect(portal.prop('isOpened')).toBe(true);
                });
        });

        it('отправит метрику', () => {
            return editHotSpotPromise
                .catch(() => { })
                .then(() => {
                    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
                    expect(contextMock.metrika.sendPageEvent).toHaveBeenLastCalledWith([ 'panoramas_poi', 'edit', 'error' ]);
                });
        });
    });
});

describe('перемещение существующей точки', () => {
    let wrapper: ShallowWrapper;
    const spot = panoramaHotSpotMock.value();

    beforeEach(() => {
        initialState.panoramaHotSpots = panoramaHotSpotsStateMock
            .withSpots([ spot ], props.panoramaId)
            .value();
        const { componentInstance, page } = shallowRenderComponent({ props, initialState });

        wrapper = page;

        contextMock.metrika.sendPageEvent.mockClear();

        componentInstance.setState({ isPortalOpened: true, portalType: PortalType.PREVIEW_POPUP, portalSpotId: spot.point.id });
    });

    describe('в начале перемещения', () => {
        beforeEach(() => {
            const hotSpot = wrapper.find('PanoramaHotSpot');
            hotSpot.simulate('dragStart');
        });

        it('заморозит панораму', () => {
            expect(props.onFrozenStateChange).toHaveBeenCalledTimes(1);
            expect(props.onFrozenStateChange).toHaveBeenLastCalledWith(true);
        });

        it('скроет модал', () => {
            const portal = wrapper.find('PanoramaHotSpotPortal');
            expect(portal.prop('isOpened')).toBe(false);
        });
    });

    describe('при дропе точки', () => {
        beforeEach(() => {
            const hotSpot = wrapper.find('PanoramaHotSpot');
            hotSpot.simulate('dragStart');
            hotSpot.simulate('dragMove', spot, [ 100, 50 ]);
            hotSpot.simulate('dragEnd', spot, { clientX: 0, clientY: 0 });
        });

        it('дернет проп', () => {
            expect(editHotSpotMock).toHaveBeenCalledTimes(1);
            expect(editHotSpotMock.mock.calls[0]).toMatchSnapshot();
        });

        it('после сохранения отправит метрику', () => {
            return editHotSpotPromise
                .then(() => {
                    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
                    expect(contextMock.metrika.sendPageEvent).toHaveBeenLastCalledWith([ 'panoramas_poi', 'edit', 'point' ]);
                });
        });

        it('после сохранения разморозит панораму', () => {
            return editHotSpotPromise
                .then(() => {
                    expect(props.onFrozenStateChange).toHaveBeenCalledTimes(2);
                    expect(props.onFrozenStateChange).toHaveBeenLastCalledWith(false);
                });
        });
    });
});

describe('перемещение новой точки', () => {
    let wrapper: ShallowWrapper;
    const spot = panoramaHotSpotMock.withNewPoint().value();

    beforeEach(() => {
        initialState.panoramaHotSpots = panoramaHotSpotsStateMock
            .withSpots([ spot ], props.panoramaId)
            .withNewSpotPostError('out_of_the_vehicle', props.panoramaId)
            .value();
        const { componentInstance, page } = shallowRenderComponent({ props, initialState });

        wrapper = page;

        contextMock.metrika.sendPageEvent.mockClear();

        componentInstance.setState({ isPortalOpened: true, portalType: PortalType.PREVIEW_POPUP, portalSpotId: spot.point.id });
    });

    it('при дропе вызовет соотвествующие экшены', () => {
        const hotSpot = wrapper.find('PanoramaHotSpot');
        hotSpot.simulate('dragStart');
        hotSpot.simulate('dragMove', spot, [ 100, 50 ]);
        hotSpot.simulate('dragEnd', spot, { clientX: 0, clientY: 0 });

        expect(removeNewHotSpotMock).toHaveBeenCalledTimes(1);

        expect(createNewHotSpotMock).toHaveBeenCalledTimes(1);
        expect(createNewHotSpotMock).toHaveBeenCalledWith({ aspectType: 'R4x3', frame: 42, x: 0.25, y: 0.5, panoramaId: 'my-panorama' });

        expect(postNewHotSpotMock).toHaveBeenCalledTimes(1);
        expect(postNewHotSpotMock).toHaveBeenCalledWith({
            panoramaId: props.panoramaId,
            photoUrl: undefined,
            text: 'this is hot!',
        });
    });
});

describe('ховер на существующую точку', () => {
    let wrapper: ShallowWrapper;
    const spot = panoramaHotSpotMock.value();

    beforeEach(() => {
        initialState.panoramaHotSpots = panoramaHotSpotsStateMock
            .withSpots([ spot ], props.panoramaId)
            .value();
        props.isOwner = false;
        const { page } = shallowRenderComponent({ props, initialState });

        wrapper = page;

        contextMock.metrika.sendPageEvent.mockClear();
    });

    it('mouse enter откроет превью-модал через таймаут', () => {
        const hotSpot = wrapper.find('PanoramaHotSpot');
        hotSpot.simulate('mouseEnter', spot);

        const portal = wrapper.find('PanoramaHotSpotPortal');
        expect(portal.isEmptyRender()).toBe(true);

        jest.advanceTimersByTime(200);

        const updatedPortal = wrapper.find('PanoramaHotSpotPortal');
        expect(updatedPortal.prop('isOpened')).toBe(true);
        expect(updatedPortal.prop('spot')).toEqual(spot);
        expect(updatedPortal.prop('type')).toBe(PortalType.PREVIEW_POPUP);
    });

    it('mouse leave скроет превью-модал через таймаут', () => {
        const hotSpot = wrapper.find('PanoramaHotSpot');
        hotSpot.simulate('mouseEnter', spot);
        jest.advanceTimersByTime(500);
        hotSpot.simulate('mouseLeave');

        const portal = wrapper.find('PanoramaHotSpotPortal');
        expect(portal.prop('isOpened')).toBe(true);

        jest.advanceTimersByTime(200);

        const updatedPortal = wrapper.find('PanoramaHotSpotPortal');
        expect(updatedPortal.prop('isOpened')).toBe(false);
    });
});

describe('удаление точки', () => {
    it('удалит новую точку', () => {
        const spot = panoramaHotSpotMock.withNewPoint().value();
        initialState.panoramaHotSpots = panoramaHotSpotsStateMock
            .withSpots([ spot ], props.panoramaId)
            .withNewSpotPostError('out_of_the_vehicle', props.panoramaId)
            .value();
        const { page } = shallowRenderComponent({ props, initialState });

        const hotSpot = page.find('PanoramaHotSpot');
        hotSpot.simulate('remove', spot);

        expect(removeNewHotSpotMock).toHaveBeenCalledTimes(1);
        expect(props.onFrozenStateChange).toHaveBeenCalledTimes(1);
        expect(props.onFrozenStateChange).toHaveBeenLastCalledWith(false);
    });

    it('удалит существующую точку', () => {
        const spot = panoramaHotSpotMock.value();
        initialState.panoramaHotSpots = panoramaHotSpotsStateMock
            .withSpots([ spot ], props.panoramaId)
            .value();
        const { page } = shallowRenderComponent({ props, initialState });

        const hotSpot = page.find('PanoramaHotSpot');
        hotSpot.simulate('click', spot);

        const portal = page.find('PanoramaHotSpotPortal');
        portal.simulate('spotRemove', spot);

        expect(deleteHotSpotMock).toHaveBeenCalledTimes(1);
        expect(deleteHotSpotMock).toHaveBeenCalledWith(props.panoramaId, spot.point.id);
    });
});

describe('клики на контейнере', () => {
    const spot = panoramaHotSpotMock.value();

    beforeEach(() => {
        initialState.panoramaHotSpots = panoramaHotSpotsStateMock
            .withSpots([ spot ], props.panoramaId)
            .value();
    });

    it('не ловятся в обычной ситуации', () => {
        shallowRenderComponent({ props, initialState });
        clickCallbacks.forEach((callback) => callback(clickEventMock));

        expect(clickEventMock.stopPropagation).toHaveBeenCalledTimes(0);
    });

    it('не ловятся если передан спец флаг', () => {
        props.isAddMode = true;
        props.allowClickEventBubble = true;
        shallowRenderComponent({ props, initialState });
        clickCallbacks.forEach((callback) => callback(clickEventMock));

        expect(clickEventMock.stopPropagation).toHaveBeenCalledTimes(0);
    });

    it('ловятся если открыт превью попап', () => {
        const { componentInstance, page } = shallowRenderComponent({ props, initialState });
        componentInstance.setState({ isPortalOpened: true, portalType: PortalType.PREVIEW_POPUP, portalSpotId: spot.point.id });
        clickCallbacks.forEach((callback) => callback(clickEventMock));

        expect(clickEventMock.stopPropagation).toHaveBeenCalledTimes(1);

        const portal = page.find('PanoramaHotSpotPortal');
        expect(portal.prop('isOpened')).toBe(false);
    });

    it('ловятся если открыт попап ошибки', () => {
        const { componentInstance, page } = shallowRenderComponent({ props, initialState });
        componentInstance.setState({ isPortalOpened: true, portalType: PortalType.ERROR_POPUP, portalSpotId: spot.point.id });
        clickCallbacks.forEach((callback) => callback(clickEventMock));

        expect(clickEventMock.stopPropagation).toHaveBeenCalledTimes(1);

        const portal = page.find('PanoramaHotSpotPortal');
        expect(portal.prop('isOpened')).toBe(false);
    });

    it('ловятся во время добавления точки', () => {
        props.isAddMode = true;
        shallowRenderComponent({ props, initialState });
        clickCallbacks.forEach((callback) => callback(clickEventMock));

        expect(clickEventMock.stopPropagation).toHaveBeenCalledTimes(1);
    });

    it('ловятся если открыто промо', () => {
        props.hasPromo = true;
        shallowRenderComponent({ props, initialState });
        clickCallbacks.forEach((callback) => callback(clickEventMock));

        expect(clickEventMock.stopPropagation).toHaveBeenCalledTimes(1);
    });
});

function simulateSpotSave({ componentInstance, page, photoUrl, spot, text }: {
    page: ShallowWrapper;
    componentInstance: ComponentMock;
    text: string;
    photoUrl: string;
    spot: TPanoramaHotSpot;
}) {
    componentInstance.setState({ isPortalOpened: true, portalType: PortalType.PHOTO_MODAL, portalSpotId: spot.point.id });

    const portal = page.find('PanoramaHotSpotPortal');
    portal.simulate('spotSave', spot, text, photoUrl);
}

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
        new_spot: {
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

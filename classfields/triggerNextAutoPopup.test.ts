jest.mock('auto-core/react/lib/localstorage', () => ({
    getItem: jest.fn(),
    setItem: jest.fn(),
}));

import MockDate from 'mockdate';

import mockStore from 'autoru-frontend/mocks/mockStore';

import { getItem, setItem } from 'auto-core/react/lib/localstorage';
import { LS_KEY } from 'auto-core/react/lib/userSession';
import type { AppStateCore } from 'auto-core/react/AppState';
import configMock from 'auto-core/react/dataDomain/config/mock';
import userMock from 'auto-core/react/dataDomain/user/mocks';
import geoMock from 'auto-core/react/dataDomain/geo/mocks/geo.mock';
import routerMock from 'auto-core/react/dataDomain/router/mock';

import { AutoPopupNames, BUNKER_KEY } from '../types';
import actionTypes from '../actionTypes';
import items from '../mocks/items.mocks';

import triggerNextAutoPopup from './triggerNextAutoPopup';

const getItemMock = getItem as jest.MockedFunction<typeof getItem>;

const priority = {
    index: [
        items[1].id,
        items[2].id,
        items[0].id,
    ],
};

const defaultSession = {
    autoPopupShown: false,
    openedAt: 1587340800000,
    updatedAt: 1587340800000,
    visits: 2,
};
const currentDate = '2020-04-20';

let state: AppStateCore;
beforeEach(() => {
    state = {
        autoPopup: { id: undefined },
        bunker: {
            [BUNKER_KEY]: {
                whiteList: {
                    [AutoPopupNames.FOO]: true,
                    [AutoPopupNames.BAR]: false,
                    [AutoPopupNames.BAZ]: true,
                },
            },
        },
        config: configMock.withPageType('index').value(),
        cookies: { from: 'direct' },
        geo: geoMock,
        router: routerMock.value(),
        user: userMock.value(),
        searchID: {
            searchID: 'searchID',
            parentSearchId: 'parentSearchID',
        },
    };

    getItemMock.mockReturnValue(JSON.stringify(defaultSession));

    MockDate.set(currentDate);
});

afterEach(() => {
    MockDate.reset();
});

it('диспатчит экшен с первым не пустым попапом', async() => {
    const store = mockStore(state);
    const expectedActions = [
        {
            type: actionTypes.AUTO_POPUP_RESOLVED,
            payload: {
                id: AutoPopupNames.BAZ,
                data: { text: 'baz' },
            },
        },
    ];

    await store.dispatch(triggerNextAutoPopup(items, priority));
    expect(store.getActions()).toEqual(expectedActions);

    const now = Date.now();
    const expectedArg = JSON.stringify({ autoPopupShown: true, openedAt: defaultSession.openedAt, updatedAt: now, visits: 2 });
    expect(setItem).toHaveBeenCalledTimes(1);
    expect(setItem).toHaveBeenCalledWith(LS_KEY, expectedArg);
});

it('если в гет-параметрах есть флаг force_auto_popup, задиспатчит экшен с этим попапом', async() => {
    const store = mockStore({
        ...state,
        router: routerMock.withCurrentRoute({ params: { force_auto_popup: AutoPopupNames.BAR } }).value(),
        config: configMock.withPageType('index').withPageParams({ force_auto_popup: AutoPopupNames.BAR }).value(),
        cookies: { from: 'morda' },
    });
    const expectedActions = [
        {
            type: actionTypes.AUTO_POPUP_RESOLVED,
            payload: {
                id: AutoPopupNames.BAR,
            },
        },
    ];

    await store.dispatch(triggerNextAutoPopup(items, priority));
    expect(store.getActions()).toEqual(expectedActions);

    const now = Date.now();
    const expectedArg = JSON.stringify({ autoPopupShown: true, openedAt: defaultSession.openedAt, updatedAt: now, visits: 2 });
    expect(setItem).toHaveBeenCalledTimes(1);
    expect(setItem).toHaveBeenCalledWith(LS_KEY, expectedArg);
});

it('если в гет-параметрах есть флаг force_auto_popup, но данных для него нет, задиспатчит экшен с попапом из очереди', async() => {
    const store = mockStore({
        ...state,
        config: configMock.withPageType('index').withPageParams({ force_auto_popup: AutoPopupNames.FOO }).value(),
    });
    const expectedActions = [
        {
            type: actionTypes.AUTO_POPUP_RESOLVED,
            payload: {
                id: AutoPopupNames.BAZ,
                data: { text: 'baz' },
            },
        },
    ];

    await store.dispatch(triggerNextAutoPopup(items, priority));
    expect(store.getActions()).toEqual(expectedActions);

    const now = Date.now();
    const expectedArg = JSON.stringify({ autoPopupShown: true, openedAt: defaultSession.openedAt, updatedAt: now, visits: 2 });
    expect(setItem).toHaveBeenCalledTimes(1);
    expect(setItem).toHaveBeenCalledWith(LS_KEY, expectedArg);
});

it('ничего не будет делать для непрямого траффика', async() => {
    const store = mockStore({
        ...state,
        cookies: { from: 'morda' },
    });
    await store.dispatch(triggerNextAutoPopup(items, priority));
    expect(store.getActions()).toHaveLength(0);
    expect(setItem).toHaveBeenCalledTimes(0);
});

it('ничего не будет делать, если в сессии нет данных о визитах пользователя', async() => {
    getItemMock.mockReturnValueOnce(JSON.stringify({}));
    const store = mockStore(state);

    await store.dispatch(triggerNextAutoPopup(items, priority));
    expect(store.getActions()).toHaveLength(0);
    expect(setItem).toHaveBeenCalledTimes(0);
});

it('ничего не будет делать, если это первый визит пользователя', async() => {
    getItemMock.mockReturnValueOnce(JSON.stringify({ ...defaultSession, visits: 1 }));
    const store = mockStore(state);

    await store.dispatch(triggerNextAutoPopup(items, priority));
    expect(store.getActions()).toHaveLength(0);
    expect(setItem).toHaveBeenCalledTimes(0);
});

it('ничего не будет делать, если пользователю уже был показан один попап', async() => {
    getItemMock.mockReturnValueOnce(JSON.stringify({ ...defaultSession, autoPopupShown: true }));
    const store = mockStore(state);

    await store.dispatch(triggerNextAutoPopup(items, priority));
    expect(store.getActions()).toHaveLength(0);
    expect(setItem).toHaveBeenCalledTimes(0);
});

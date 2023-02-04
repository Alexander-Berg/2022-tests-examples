import contextMock from 'autoru-frontend/mocks/contextMock';

import configMock from 'auto-core/react/dataDomain/config/mock';
import userMock from 'auto-core/react/dataDomain/user/mocks';
import geoMock from 'auto-core/react/dataDomain/geo/mocks/geo.mock';
import routerMock from 'auto-core/react/dataDomain/router/mock';

import { AutoPopupNames, BUNKER_KEY } from '../types';
import items from '../mocks/items.mocks';

import processQueue from './processQueue';

const dispatchMock = jest.fn();
const state = {
    autoPopup: { id: undefined, isFetching: false },
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
    cookies: {},
    geo: geoMock,
    router: routerMock.value(),
    user: userMock.value(),
    searchID: {
        searchID: 'searchID',
        parentSearchId: 'parentSearchID',
    },
};

beforeEach(() => {
    dispatchMock.mockClear();
});

it('пропускает элемент в очереди, если его не нужно запускать', async() => {
    const queue = [
        {
            ...items[0],
            shouldRun: () => false,
        },
        items[1],
        items[2],
    ];
    await processQueue(queue, state, dispatchMock, contextMock);

    expect(items[0].run).toHaveBeenCalledTimes(0);
    expect(items[1].run).toHaveBeenCalledTimes(1);
    expect(items[2].run).toHaveBeenCalledTimes(0);
});

it('возвращает первый непустой результат', async() => {
    const result = await processQueue(items, state, dispatchMock, contextMock);

    expect(items[0].run).toHaveBeenCalledTimes(1);
    expect(items[1].run).toHaveBeenCalledTimes(1);
    expect(items[2].run).toHaveBeenCalledTimes(0);
    expect(result).toEqual({ id: AutoPopupNames.BAR });
});

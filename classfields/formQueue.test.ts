import configMock from 'auto-core/react/dataDomain/config/mock';
import userMock from 'auto-core/react/dataDomain/user/mocks';
import geoMock from 'auto-core/react/dataDomain/geo/mocks/geo.mock';
import routerMock from 'auto-core/react/dataDomain/router/mock';

import { AutoPopupNames, BUNKER_KEY } from '../types';
import items from '../mocks/items.mocks';

import formQueue from './formQueue';

it('правильно формирует очередь', () => {
    const priority = {
        index: [
            AutoPopupNames.BAZ,
            AutoPopupNames.BAR,
            AutoPopupNames.FOO,
        ],
    };

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

    const queue = formQueue(items, priority, state).map(({ id }) => id);

    expect(queue).toEqual([ AutoPopupNames.BAZ, AutoPopupNames.FOO ]);
});

import { createStore, runStore } from 'view/store';
import { createStoreProxy } from 'view/store/store-proxy';

export function createAppStoreProxy() {
    return createStoreProxy();
}

export function runAppStore(storeProxy, appContext, state = {}) {
    const initialState = {
        user: {
            crc: 'crc',
            uid: '123',
            yandexuid: '456',
            name: 'locky',
            login: 'locky',
            role: 'manager',
            permissions: []
        },
        config: {
            serverTime: new Date('Fri Aug 31 2018 17:39:33 GMT+0300 (MSK)').getTime(),
            lkOrigin: 'https://realty.yandex.ru'
        },
        ...state
    };

    const store = runStore(createStore(initialState), appContext);

    storeProxy.imitate(store);

    return store;
}

export function createStoreMock(initialState) {
    return {
        initialState,
        extend(initialStatePatch) {
            return createStoreMock({ ...initialState, ...initialStatePatch });
        }
    };
}

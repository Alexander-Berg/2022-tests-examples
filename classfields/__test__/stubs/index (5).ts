import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { IUniversalStore } from 'view/modules/types';

export const store: DeepPartial<IUniversalStore> = {
    passport: {
        defaultPhone: '',
        defaultEmail: '',
        isAuth: false,
        phones: [],
        emails: [],
    },
    passportAccounts: {
        accounts: [],
    },
    config: {
        realtyUrl: 'https://realty.test.vertis.yandex.ru/',
    },
    user: {
        calculatedInfo: {},
    },
    geo: {
        id: 2,
        type: 'CITY',
        rgid: 2323,
        name: 'Санкт-Петербург',
        locative: 'в Санкт-Петербурге',
    },
    yandexArenda: {
        isLoading: false,
        isSuccess: false,
        savedPhone: '',
    },
    page: {
        route: 'landing-owner',
    },
    serviceLinks: {
        ARENDA_SEARCH_URL: 'https://realty-yandex.ru',
    },
    landing: {
        network: {
            getMapDataPointsStatus: RequestStatus.INITIAL,
            getMapDataStatus: RequestStatus.INITIAL,
        },
        data: {
            map: {},
        },
        config: {
            shouldLazyLoadImages: false,
        },
    },
    landingDynamic: {
        intersection: {
            blocksInViewport: {},
        },
    },
};

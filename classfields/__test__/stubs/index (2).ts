import { DeepPartial } from 'utility-types';

import { Renovation } from '@vertis/schema-registry/ts-types/realty/offer/common';

import { RequestStatus } from 'realty-core/types/network';

import { IUniversalStore } from 'view/modules/types';

export const getStore = (isMobile?: string): DeepPartial<IUniversalStore> => {
    return {
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
        user: {
            calculatedInfo: {},
        },
        rentPrice: {
            fields: {
                address: 'г. Магов, ул. Друида, 43',
                numberOfRooms: 3,
                area: 23,
                floor: 233,
                renovation: Renovation.RENOVATION_COSMETIC_REQUIRED,
            },
            network: {
                updateRentPriceAction: RequestStatus.LOADED,
                priceByAi: 343434434,
            },
        },
        geo: {
            id: 2,
            type: 'SUBJECT_FEDERATION',
            rgid: 741965,
            name: 'Санкт-Петербург и ЛО',
            locative: 'в Санкт-Петербурге и ЛО',
            isInNovosibirskObl: true,
        },
        config: {
            isMobile,
        },
        serviceLinks: {
            ARENDA_SEARCH_URL: 'https://realty.ru',
        },
    };
};

export const getStoreWithLoadingForm = (isMobile?: string): DeepPartial<IUniversalStore> => {
    return {
        ...getStore(isMobile),
        rentPrice: {
            network: {
                updateRentPriceAction: RequestStatus.PENDING,
            },
        },
    };
};

export const getStoreWithForm = (isMobile?: string): DeepPartial<IUniversalStore> => {
    return {
        ...getStore(isMobile),
        rentPrice: {
            network: {
                updateRentPriceAction: RequestStatus.INITIAL,
            },
        },
    };
};

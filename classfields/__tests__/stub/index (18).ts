import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { tinkoffCard, sberbankCard } from 'view/components/UserPaymentMethodsCardBase/__tests__/stub';
import { IUniversalStore } from 'view/modules/types';

export const storeWithoutCards: DeepPartial<IUniversalStore> = {
    payments: {
        data: {
            cards: [],
        },
        network: {
            initBindCard: RequestStatus.LOADED,
        },
    },
    spa: {
        status: RequestStatus.LOADED,
    },
};

export const storeWithOneCard: DeepPartial<IUniversalStore> = {
    payments: {
        data: {
            cards: [tinkoffCard],
        },
        network: {
            initBindCard: RequestStatus.LOADED,
        },
    },
    spa: {
        status: RequestStatus.LOADED,
    },
};

export const storeWithTwoCard: DeepPartial<IUniversalStore> = {
    payments: {
        data: {
            cards: [tinkoffCard, sberbankCard],
        },
        network: {
            initBindCard: RequestStatus.LOADED,
        },
    },
    spa: {
        status: RequestStatus.LOADED,
    },
};

export const storeWithSkeleton: DeepPartial<IUniversalStore> = {
    payments: {
        data: {
            cards: [],
        },
        network: {
            initBindCard: RequestStatus.LOADED,
        },
    },
    spa: {
        status: RequestStatus.PENDING,
    },
};

import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { IUniversalStore } from 'view/modules/types';
import { PageName } from 'view/libs/link';

export const store: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    breadcrumbs: {
        crumbs: [
            {
                route: 'manager-search-flats',
            },
            {
                route: 'manager-flat-payments',
                params: {
                    flatId: 'e47ee70238524841bcff89acb3356bce',
                },
            },
        ],
        current: {
            route: 'manager-flat-payment',
        },
    },
};

export const mobileStore: DeepPartial<IUniversalStore> = {
    config: { isMobile: 'iOS' },
    spa: {
        status: RequestStatus.LOADED,
    },
    breadcrumbs: {
        crumbs: [
            {
                route: 'manager-search-flats',
            },
            {
                route: 'manager-flat-payments',
                params: {
                    flatId: 'e47ee70238524841bcff89acb3356bce',
                },
            },
        ],
        current: {
            route: 'manager-flat-payment',
        },
    },
};

export const onlyContentStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    cookies: { ['only-content']: 'true' },
    spa: {
        status: RequestStatus.LOADED,
    },
    breadcrumbs: {
        crumbs: [
            {
                route: 'manager-search-flats',
            },
            {
                route: 'manager-flat-payments',
                params: {
                    flatId: 'e47ee70238524841bcff89acb3356bce',
                },
            },
        ],
        current: {
            route: 'manager-flat-payment',
        },
    },
};

export const onlyContentMobileStore: DeepPartial<IUniversalStore> = {
    config: { isMobile: 'iOS' },
    cookies: { ['only-content']: 'true' },
    spa: {
        status: RequestStatus.LOADED,
    },
    breadcrumbs: {
        crumbs: [
            {
                route: 'manager-search-flats',
            },
            {
                route: 'manager-flat-payments',
                params: {
                    flatId: 'e47ee70238524841bcff89acb3356bce',
                },
            },
        ],
        current: {
            route: 'manager-flat-payment',
        },
    },
};

export const noBcStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
};

export const noBcMobileStore: DeepPartial<IUniversalStore> = {
    config: { isMobile: 'iOS' },
    spa: {
        status: RequestStatus.LOADED,
    },
};

export const oneCrumbStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    breadcrumbs: {
        current: {
            route: 'manager-flat-form',
        },
    },
};

export const oneCrumbMobileStore: DeepPartial<IUniversalStore> = {
    config: { isMobile: 'iOS' },
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    breadcrumbs: {
        current: {
            route: 'manager-flat-form',
        },
    },
};

const manyCrumbs: { route: PageName }[] = [
    { route: 'manager-search-users' },
    { route: 'outstaff-retoucher-search-flats' },
    { route: 'owner-payments-history' },
    { route: 'owner-flat-tenant-group' },
    { route: 'owner-flat-tenant-candidate-groups' },
    { route: 'owner-flat-tenant-candidate-group' },
    { route: 'manager-flat-payments' },
    { route: 'manager-flat-payment' },
    { route: 'manager-flat-showings' },
    { route: 'manager-flat-statistics' },
    { route: 'manager-flat-showing' },
    { route: 'manager-flat-contracts' },
    { route: 'manager-flat-contract-form' },
    { route: 'manager-user' },
    { route: 'personal-data' },
    { route: 'owner-payment-data' },
    { route: 'owner-payment-methods' },
    { route: 'feedback' },
    { route: 'owner-flat-draft' },
    { route: 'owner-flat-photos' },
    { route: 'manager-search-flats' },
];

export const manyCrumbsStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    breadcrumbs: {
        crumbs: manyCrumbs,
        current: {
            route: 'manager-flat-form',
        },
    },
};

export const manyCrumbsMobileStore: DeepPartial<IUniversalStore> = {
    config: { isMobile: 'iOS' },
    spa: {
        status: RequestStatus.LOADED,
    },
    breadcrumbs: {
        crumbs: manyCrumbs,
        current: {
            route: 'manager-flat-form',
        },
    },
};

export const sameTitleStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    breadcrumbs: {
        crumbs: [{ route: 'user-flat' }],
        current: {
            route: 'owner-payments-history',
        },
    },
};

export const sameTitleMobileStore: DeepPartial<IUniversalStore> = {
    config: { isMobile: 'iOS' },
    spa: {
        status: RequestStatus.LOADED,
    },
    breadcrumbs: {
        crumbs: [{ route: 'user-flat' }],
        current: {
            route: 'owner-payments-history',
        },
    },
};

export const skeletonStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.PENDING,
    },
};

export const skeletonMobileStore: DeepPartial<IUniversalStore> = {
    config: { isMobile: 'iOS' },
    spa: {
        status: RequestStatus.PENDING,
    },
};

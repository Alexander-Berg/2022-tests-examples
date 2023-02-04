import { DeepPartial } from 'utility-types';

import { Flavor } from 'realty-core/types/utils';

import { RequestStatus } from 'realty-core/types/network';

import { FlatStatus, FlatUserRole } from 'types/flat';

import { IUniversalStore } from 'view/modules/types';

export const storeFlats: DeepPartial<Pick<IUniversalStore, 'managerUser'>> = {
    managerUser: {
        user: {
            phone: '+79999999999',
            person: {
                name: 'Семен',
                surname: 'Иванов',
                patronymic: 'Петрович',
            },
            hasAcceptedTermsOfUse: true,
            email: '123@yandex.ru',
            calculatedInfo: {
                hasOwnerRequests: false,
                isTenant: true,
            },
            userId: '3c128cec0585' as Flavor<string, 'UserId'>,
        },
        assignedFlats: [
            {
                flatId: 'cb4ccf01ab2d4b18857aef94c2dfc56f',
                address: {
                    address: 'г. Москва, ул. Народного ополчения, д.39к1',
                    flatNumber: '10',
                },
                status: FlatStatus.CONFIRMED,
                userRole: FlatUserRole.TENANT,
            },
            {
                flatId: 'cb4ccf01ab2d4b18857aef94c2dfc56e',
                address: {
                    address: 'г. Москва, ул Новая Басманная, д.13',
                    flatNumber: '666',
                },
                status: FlatStatus.CONFIRMED,
                userRole: FlatUserRole.OWNER,
            },
            {
                flatId: 'cb4ccf01ab2d4b18857aef94c2dfc56w',
                address: {
                    address: 'г. Москва, ул. Павловская, д.8',
                    flatNumber: '88',
                },
                status: FlatStatus.CONFIRMED,
                userRole: FlatUserRole.TENANT_CANDIDATE,
            },
        ],
        naturalPersonChecks: {},
    },
};

export const storeNoFlats: DeepPartial<Pick<IUniversalStore, 'managerUser'>> = {
    managerUser: {
        user: {
            phone: '+79999999999',
            person: {
                name: 'Семен',
                surname: 'Иванов',
                patronymic: 'Петрович',
            },
            hasAcceptedTermsOfUse: true,
            email: '123@yandex.ru',
            calculatedInfo: {
                hasOwnerRequests: false,
                isTenant: true,
            },
            userId: '3c128cec0585' as Flavor<string, 'UserId'>,
        },
        assignedFlats: [],
        naturalPersonChecks: {},
    },
};

export const storeSkeleton: DeepPartial<IUniversalStore> = {
    page: {
        isLoading: true,
    },
    spa: {
        status: RequestStatus.PENDING,
    },
};

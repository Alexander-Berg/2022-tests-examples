import { DeepPartial } from 'utility-types';

import { Flavor } from 'realty-core/types/utils';

import { RequestStatus } from 'realty-core/types/network';

import { getManagerFlatFormFields } from 'app/libs/manager-flat-form/getManagerFlatFormFields';

import { FlatStatus, FlatUserRole, IManagerFlat } from 'types/flat';
import { IUniversalStore } from 'view/modules/types';

const owner: IManagerFlat = {
    flat: {
        address: {
            address: 'г Москва, ул Минская, д 6, кв 1',
            flatNumber: '10',
        },
        flatId: 'flat10' as Flavor<string, 'FlatID'>,
        assignedUsers: [
            {
                userId: 'owner' as Flavor<string, 'UserId'>,
                person: {
                    name: 'Антон',
                    surname: 'Антонов',
                    patronymic: 'Антонович',
                },
                email: 'assignedowner@gmail.com',
                phone: '89997773322',
                userRole: FlatUserRole.OWNER,
            },
            {
                userId: 'owner' as Flavor<string, 'UserId'>,
                person: {
                    name: 'Борис',
                    surname: 'Борисов',
                    patronymic: 'Борисович',
                },
                email: 'assignedtenant@gmail.com',
                phone: '89997773322',
                userRole: FlatUserRole.TENANT,
            },
        ],
        userRole: FlatUserRole.OWNER,
        desiredRentAmount: '3000000',
        status: FlatStatus.CONFIRMED,
        code: '12-DDFF',
    },
    actualContract: {},
    extendedCode: 'A0000000012DDFF',
};

export const flatCreatedByOwner: DeepPartial<IUniversalStore> = {
    managerFlat: owner,
    managerFlatForm: {
        fields: getManagerFlatFormFields(owner),
    },
    page: {
        params: {
            flatId: owner.flat.flatId,
        },
    },
};
const manager: IManagerFlat = {
    flat: {
        address: {
            address: 'г Москва, ул Минская, д 6, кв 1',
            flatNumber: '10',
        },
        flatId: 'flat10' as Flavor<string, 'FlatID'>,
        assignedUsers: [
            {
                userId: 'owner' as Flavor<string, 'UserId'>,
                person: {
                    name: 'Антон',
                    surname: 'Антонов',
                    patronymic: 'Антонович',
                },
                email: 'assignedowner@gmail.com',
                phone: '89997773322',
                userRole: FlatUserRole.OWNER,
            },
            {
                userId: 'owner' as Flavor<string, 'UserId'>,
                person: {
                    name: 'Борис',
                    surname: 'Борисов',
                    patronymic: 'Борисович',
                },
                email: 'assignedtenant@gmail.com',
                phone: '89997773322',
                userRole: FlatUserRole.TENANT,
            },
        ],
        userRole: FlatUserRole.OWNER,
        desiredRentAmount: '3000000',
        status: FlatStatus.CONFIRMED,
        code: '12-DDFF',
    },
    actualContract: {},
};

export const flatCreatedByManager: DeepPartial<IUniversalStore> = {
    managerFlat: manager,
    managerFlatForm: {
        fields: getManagerFlatFormFields(manager),
    },
    page: {
        params: {
            flatId: manager.flat.flatId,
        },
    },
};

const application: IManagerFlat = {
    flat: {
        address: {
            address: 'г Москва, ул Минская, д 6, кв 1',
            flatNumber: '10',
        },
        person: {
            name: 'Антон',
            surname: 'Антонов',
            patronymic: 'Антонович',
        },
        phone: '89042331155',
        email: 'applicationowner@gmail.com',
        flatId: 'flat10' as Flavor<string, 'FlatID'>,
        assignedUsers: [
            {
                userId: 'owner' as Flavor<string, 'UserId'>,
                person: {
                    name: 'Антон',
                    surname: 'Антонов',
                    patronymic: 'Антонович',
                },
                email: 'assignedowner@gmail.com',
                phone: '89997773322',
                userRole: FlatUserRole.OWNER,
            },
            {
                userId: 'owner' as Flavor<string, 'UserId'>,
                person: {
                    name: 'Борис',
                    surname: 'Борисов',
                    patronymic: 'Борисович',
                },
                email: 'assignedtenant@gmail.com',
                phone: '89997773322',
                userRole: FlatUserRole.TENANT,
            },
        ],
        userRole: FlatUserRole.OWNER,
        desiredRentAmount: '3000000',
        status: FlatStatus.CONFIRMED,
        code: '12-DDFF',
    },
    actualContract: {},
};

export const flatCreatedByAplication: DeepPartial<IUniversalStore> = {
    managerFlat: application,
    managerFlatForm: {
        fields: getManagerFlatFormFields(application),
    },
    page: {
        params: {
            flatId: application.flat.flatId,
        },
    },
};

export const onSkeleton: DeepPartial<IUniversalStore> = {
    spa: {
        status: RequestStatus.PENDING,
    },
};

const emptyFlat = {
    flat: {
        address: {
            address: '',
            flatNumber: '',
        },
        flatId: '' as Flavor<string, 'FlatID'>,
        assignedUsers: [],
        userRole: FlatUserRole.OWNER,
        desiredRentAmount: '',
        status: FlatStatus.RENTED,
        code: '12-DDFF',
    },
    actualContract: {},
};

export const newFlatCreated: DeepPartial<IUniversalStore> = {
    managerFlat: {},
    managerFlatForm: {
        fields: getManagerFlatFormFields(emptyFlat),
    },
};

export const mobileStore: DeepPartial<IUniversalStore> = {
    config: { isMobile: 'iOS' },
    managerFlat: {},
    managerFlatForm: {
        fields: getManagerFlatFormFields(emptyFlat),
    },
    spa: {
        status: RequestStatus.LOADED,
    },
};

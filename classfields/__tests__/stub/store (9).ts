import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';
import { Flavor } from 'realty-core/types/utils';

import { StaffDepartment, StaffUID } from 'types/staff';
import { FlatStatus, FlatUserRole, ShowingId } from 'types/flat';
import { FlatShowingStatus, FlatShowingType } from 'types/showing';

import { IUniversalStore } from 'view/modules/types';
import { initialState } from 'view/modules/managerFlatShowingResolutionForm/reducers/fields';
import { Fields } from 'view/modules/managerFlatShowingResolutionForm/types';

const flatShowing = {
    showingType: FlatShowingType.OFFLINE,
    tenantStructureInfo: {
        numberOfAdults: 1,
    },
    amoLeadLink: 'https://yandexarenda.amocrm.ru/leads/detail/21458605',
    showingId: '2246c06419b4' as ShowingId,
    groupUsers: {
        mainUser: {
            person: {
                name: 'Петр',
                surname: 'Иванов',
                patronymic: 'Иванович',
            },
            phone: '+79043333335',
        },
        users: [
            {
                person: {
                    name: 'Олег',
                    surname: 'Иванов',
                    patronymic: 'Иванович',
                },
                phone: '+79043333335',
            },
            {
                person: {
                    name: 'Степан',
                    surname: 'Иванов',
                    patronymic: 'Иванович',
                },
                phone: '+79043333335',
            },
            {
                person: {
                    name: 'Павел',
                    surname: 'Иванов',
                    patronymic: 'Иванович',
                },
                phone: '+79043333335',
            },
        ],
    },
};

export const storeStatusShowingAppointed: DeepPartial<IUniversalStore> = {
    spa: {
        status: RequestStatus.LOADED,
    },
    page: {
        params: {
            flatId: '0599460e0d6f425e861ad270584dfddd',
            showingId: '2246c06419b4',
        },
    },
    managerFlatShowings: {
        showings: [{ ...flatShowing, status: FlatShowingStatus.SHOWING_APPOINTED }],
    },
    staff: {
        user: {
            uid: '4450000000482659' as StaffUID,
            login: 'ppp',
            name: 'Петров Александр',
        },
        groups: {
            ['yandex_personal_vertserv_comm_0395_dep52296' as StaffDepartment]: [
                {
                    uid: '4450000000039573' as StaffUID,
                    login: '111',
                    name: 'Иванова Мария',
                },
                {
                    uid: '4450000000039574' as StaffUID,
                    login: '222',
                    name: 'Иванов Алексей',
                },
            ],
        },
    },
};

export const skeletonStore: DeepPartial<IUniversalStore> = {
    ...storeStatusShowingAppointed,
    spa: {
        status: RequestStatus.PENDING,
    },
};

export const storeStatusConfirmedByTenant: DeepPartial<IUniversalStore> = {
    ...storeStatusShowingAppointed,
    managerFlat: {
        flat: {
            address: {
                address: 'г Москва, ул Минская, д 6, кв 1',
                flatNumber: '10',
            },
            flatId: 'flat10' as Flavor<string, 'FlatID'>,
            assignedUsers: [],
            userRole: FlatUserRole.OWNER,
            desiredRentAmount: '3000000',
            status: FlatStatus.CONFIRMED,
        },
    },
    managerFlatShowings: {
        showings: [{ ...flatShowing, status: FlatShowingStatus.CONFIRMED_BY_TENANT }],
    },
};

export const storeWithTwoContacts: DeepPartial<IUniversalStore> = {
    ...storeStatusShowingAppointed,
    managerFlat: {
        flat: {
            address: {
                address: 'г Москва, ул Минская, д 6, кв 1',
                flatNumber: '10',
            },
        },
    },
    managerFlatShowings: {
        showings: [{ ...flatShowing, status: FlatShowingStatus.SHOWING_APPOINTED }],
    },
};

export const getSpecificStatusStore = (status: FlatShowingStatus) => {
    return {
        ...storeStatusShowingAppointed,
        managerFlatShowings: {
            showings: [{ ...flatShowing, status }],
        },
    };
};

export const mobileStore: DeepPartial<IUniversalStore> = {
    ...storeStatusShowingAppointed,
    config: {
        isMobile: 'iPhone',
    },
};

export const mobileStorePositiveStatus: DeepPartial<IUniversalStore> = {
    ...mobileStore,
    managerFlatShowingResolutionForm: {
        fields: {
            ...initialState,
            [Fields.STATUS]: {
                id: Fields.STATUS,
                value: FlatShowingStatus.CONFIRMED_BY_TENANT,
            },
        },
    },
};

export const mobileStoreNegativeStatus: DeepPartial<IUniversalStore> = {
    ...mobileStore,
    managerFlatShowingResolutionForm: {
        fields: {
            ...initialState,
            [Fields.STATUS]: {
                id: Fields.STATUS,
                value: FlatShowingStatus.CLOSE_WITHOUT_RELEASE,
            },
        },
    },
};

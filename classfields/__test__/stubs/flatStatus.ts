import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { IUniversalStore } from 'view/modules/types';
import { FlatId, FlatStatus, FlatUserRole, IFlat } from 'types/flat';
import { initialState as managerFlatStatusFormFields } from 'view/modules/managerFlatStatusForm/reducers/fields';
import { initialState as managerFlatStatusFormNetwork } from 'view/modules/managerFlatStatusForm/reducers/network';
import { StaffDepartment, StaffUID } from 'types/staff';
import { Fields } from 'view/modules/managerFlatStatusForm/types';

const flat: DeepPartial<IFlat> = {
    address: {
        address: 'г Москва, ул Минская, д 6, кв 1',
        flatNumber: '10',
    },
    flatId: '11111111111111' as FlatId,
    person: {
        name: 'Антон',
        surname: 'Антонов',
        patronymic: 'Антонович',
    },
    email: 'assignedowner@gmail.com',
    phone: '89997773322',
    userRole: FlatUserRole.OWNER,
    assignedUsers: [],
    status: FlatStatus.UNKNOWN,
    code: '12-DDFF',
};

export const storeWithSkeleton: DeepPartial<IUniversalStore> = {
    spa: {
        status: RequestStatus.PENDING,
    },
};

export const getStore = (status: FlatStatus): DeepPartial<IUniversalStore> => {
    return {
        managerFlat: {
            flat: {
                ...flat,
                status,
            },
        },
        managerFlatStatusForm: {
            fields: {
                ...managerFlatStatusFormFields,
                [Fields.CURRENT_STATUS]: {
                    id: Fields.CURRENT_STATUS,
                    value: status,
                },
            },
            network: managerFlatStatusFormNetwork,
        },
        staff: {
            user: {
                uid: '1120000000148552' as StaffUID,
                login: 'login',
                name: 'Имя',
            },
            groups: {
                ['yandex_personal_vertserv_comm_0395_dep52296' as StaffDepartment]: [
                    {
                        uid: '1120000000069186' as StaffUID,
                        login: 'matusiktv',
                        name: 'Матусик Татьяна',
                    },
                    {
                        uid: '1120000000277929' as StaffUID,
                        login: 'tkachevalexey',
                        name: 'Ткачев Алексей',
                    },
                    {
                        uid: '1120000000297272' as StaffUID,
                        login: 'nmatvienko',
                        name: 'Матвиенко Анастасия',
                    },
                ],
            },
        },
        spa: {
            status: RequestStatus.LOADED,
        },
    };
};

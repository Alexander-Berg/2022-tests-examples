import { DeepPartial } from 'utility-types';

// eslint-disable-next-line max-len
import { CalculationStrategyNamespace_CalculationStrategy } from '@vertis/schema-registry/ts-types/realty/rent/api/rent_contract';
import { FlatUserRoleNamespace_FlatUserRole } from '@vertis/schema-registry/ts-types/realty/rent/api/common';

import { RequestStatus } from 'realty-core/types/network';

import { FlatId, FlatStatus, FlatUserRole, IFlat } from 'types/flat';
import { IRentContract, ContractId, RentContractStatus } from 'types/contract';

import { StaffDepartment, StaffUID } from 'types/staff';

import { UserId } from 'types/user';

import { IUniversalStore } from 'view/modules/types';
import { initialState as fieldsStore } from 'view/modules/managerFlatContractStatusForm/reducers/fields';
import { initialState as networkStore } from 'view/modules/managerFlatContractStatusForm/reducers/network';

import { Fields } from 'view/modules/managerFlatContractStatusForm/types';

export const flat: DeepPartial<IFlat> = {
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

export const flatContract: DeepPartial<IRentContract> = {
    contractId: '8facd8a09b6a4325b32dfd329d59cf7a' as ContractId,
    rentStartDate: '2021-11-08T21:00:00Z',
    ownerInfo: {
        person: {
            name: 'Антон',
            surname: 'Антонов',
            patronymic: 'Антонович',
        },
        phone: '70001226344',
        inn: '943091770821',
        email: 'supplementary_pet@blah.ru',
        bankInfo: {},
        assignedUser: {
            person: {},
            userRole: FlatUserRoleNamespace_FlatUserRole.OWNER,
            userId: 'd3f60e24baed' as UserId,
        },
    },
    tenantInfo: {
        person: {
            name: 'Гомер',
            surname: 'Симпсон',
            patronymic: 'Абрахам',
        },
        phone: '70004936702',
        email: 'meaningful_chest@blah.ru',
        assignedUser: {
            person: {},
            userRole: FlatUserRoleNamespace_FlatUserRole.TENANT,
            userId: 'b16ed6ece0af' as UserId,
        },
        tenantUserId: 'b16ed6ece0af',
    },
    rentAmount: '8894056103555322880',
    paymentDayOfMonth: 10,
    insurance: {
        insuranceAmount: '200000',
    },
    calculatedAmounts: {
        tenantPaymentAmount: '100000',
        ownerPaymentAmount: '100000',
    },
    contractNumber: '',
    status: RentContractStatus.ACTIVE,
    calculationStrategy: CalculationStrategyNamespace_CalculationStrategy.STRATEGY_1,
    terminationInfo: {
        checkOutWithoutAdditionalPayments: false,
        ownerContinuesWorkWithUs: false,
        tenantRefusedPayFor30Days: false,
    },
};

export const storeWithSkeleton: DeepPartial<IUniversalStore> = {
    spa: {
        status: RequestStatus.PENDING,
    },
};

export const getStore = (status: RentContractStatus): DeepPartial<IUniversalStore> => {
    return {
        managerFlat: {
            flat,
            actualContract: flatContract,
        },
        managerFlatContract: { ...flatContract, status },
        managerFlatContractStatusForm: {
            fields: {
                ...fieldsStore,
                [Fields.CURRENT_STATUS]: {
                    id: Fields.CURRENT_STATUS,
                    value: status,
                },
            },
            network: networkStore,
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
        page: {
            params: {
                contractId: flatContract.contractId,
                flatId: flat.flatId,
            },
        },
        config: {
            isMobile: '',
        },
    };
};

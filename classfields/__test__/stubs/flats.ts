import { Flavor } from 'realty-core/types/utils';

import { FlatStatus, FlatUserRole, IManagerFlat } from 'types/flat';

export const applicationOwner: IManagerFlat = {
    flat: {
        flatId: '223' as Flavor<string, 'FlatID'>,
        address: {
            address: ' г. Москва, улица Московская, д 5, кв. 343',
            flatNumber: '12345',
        },
        person: {
            name: 'И/Заявки',
            surname: 'Ф/Заявки',
            patronymic: 'О/Заявки',
        },
        phone: '89996665544',
        email: 'applicationOwner@mail.ru',
        userRole: FlatUserRole.OWNER,
        assignedUsers: [],
        status: FlatStatus.DRAFT,
        code: '12-DDFF',
    },
    actualContract: {},
};

export const assignedTenant: IManagerFlat = {
    flat: {
        flatId: '223' as Flavor<string, 'FlatID'>,
        address: {
            address: ' г. Москва, улица Московская, д 5, кв. 343',
            flatNumber: '12345',
        },
        userRole: FlatUserRole.OWNER,
        assignedUsers: [
            {
                userId: 'userid' as Flavor<string, 'UserId'>,
                person: {
                    name: 'И/Привязанных',
                    surname: 'Ф/Привязанных',
                    patronymic: 'О/Привязанных',
                },
                email: 'assignedTenant@mail.ru',
                phone: '8934332222',
                userRole: FlatUserRole.TENANT,
            },
        ],
        status: FlatStatus.DRAFT,
        code: '12-DDFF',
    },
    actualContract: {},
};

export const assignedUsers: IManagerFlat = {
    flat: {
        flatId: '223' as Flavor<string, 'FlatID'>,
        address: {
            address: ' г. Москва, улица Московская, д 5, кв. 343',
            flatNumber: '12345',
        },
        userRole: FlatUserRole.OWNER,
        assignedUsers: [
            {
                userId: 'userid' as Flavor<string, 'UserId'>,
                person: {
                    name: 'И/Привязки',
                    surname: 'Ф/Привязки',
                    patronymic: 'О/Привязки',
                },
                email: 'assignedOwner@mail.ru',
                phone: '8934332222',
                userRole: FlatUserRole.OWNER,
            },
            {
                userId: 'owner' as Flavor<string, 'UserId'>,
                person: {
                    name: 'И/Привязки',
                    surname: 'Ф/Привязки',
                    patronymic: 'О/Привязки',
                },
                email: 'assignedTenant@mail.ru',
                phone: '8932221144',
                userRole: FlatUserRole.TENANT,
            },
        ],
        status: FlatStatus.DRAFT,
        code: '12-DDFF',
    },
    actualContract: {},
};

export const assignedUsersWithPreviousTenant: IManagerFlat = {
    flat: {
        flatId: '223' as Flavor<string, 'FlatID'>,
        address: {
            address: ' г. Москва, улица Московская, д 5, кв. 343',
            flatNumber: '12345',
        },
        userRole: FlatUserRole.OWNER,
        assignedUsers: [
            {
                userId: 'userid' as Flavor<string, 'UserId'>,
                person: {
                    name: 'И/Привязки',
                    surname: 'Ф/Привязки',
                    patronymic: 'О/Привязки',
                },
                email: 'assignedOwner@mail.ru',
                phone: '8934332222',
                userRole: FlatUserRole.OWNER,
            },
            {
                userId: 'owner' as Flavor<string, 'UserId'>,
                person: {
                    name: 'И/Привязки',
                    surname: 'Ф/Привязки',
                    patronymic: 'О/Привязки',
                },
                email: 'assignedTenant@mail.ru',
                phone: '8932221144',
                userRole: FlatUserRole.PREVIOUS_TENANT,
            },
        ],
        status: FlatStatus.AFTER_RENT,
        code: '12-DDFF',
    },
    actualContract: {},
};

export const applicationOwnerMatchAssignedOwner: IManagerFlat = {
    flat: {
        flatId: '223' as Flavor<string, 'FlatID'>,
        address: {
            address: ' г. Москва, улица Московская, д 5, кв. 343',
            flatNumber: '12345',
        },
        person: {
            name: 'И/Заявки',
            surname: 'Ф/Заявки',
            patronymic: 'О/Завяки',
        },
        email: 'applicationOwner@mail.ru',
        phone: '8932221144',
        userRole: FlatUserRole.OWNER,
        assignedUsers: [
            {
                userId: 'owner' as Flavor<string, 'UserId'>,
                person: {
                    name: 'И/Заявки',
                    surname: 'Ф/Заявки',
                    patronymic: 'О/Заявки',
                },
                email: 'applicationOwner@mail.ru',
                phone: '8932221144',
                userRole: FlatUserRole.OWNER,
            },
        ],
        status: FlatStatus.DRAFT,
        code: '12-DDFF',
    },
    actualContract: {},
};

export const applicationOwnerNotMatchAssignedOwner: IManagerFlat = {
    flat: {
        flatId: '223' as Flavor<string, 'FlatID'>,
        address: {
            address: ' г. Москва, улица Московская, д 5, кв. 343',
            flatNumber: '12345',
        },
        person: {
            name: 'И/Заявки',
            surname: 'Ф/Заявки',
            patronymic: 'О/Заявки',
        },
        phone: '89996665544',
        email: 'applicationOwner@mail.ru',
        userRole: FlatUserRole.OWNER,
        assignedUsers: [
            {
                userId: 'owner' as Flavor<string, 'UserId'>,
                person: {
                    name: 'И/Привязки',
                    surname: 'Ф/Привязки',
                    patronymic: 'О/Привязки',
                },
                email: 'assignedOwner@mail.ru',
                phone: '8932221144',
                userRole: FlatUserRole.OWNER,
            },
        ],
        status: FlatStatus.DRAFT,
        code: '12-DDFF',
    },
    actualContract: {},
};

export const contractData: IManagerFlat = {
    flat: {
        flatId: '4960e34dbf2a4ef7b3f35a8d365c9577' as Flavor<string, 'FlatID'>,
        address: {
            address: 'г. Москва, улица Московская, д 5, кв. 343',
            flatNumber: '77',
        },
        userRole: FlatUserRole.OWNER,
        status: FlatStatus.RENTED,
        assignedUsers: [],
        code: '12-DDFF',
    },
    actualContract: {
        contractId: '6c1f6cb68a184604aa5a8bbea53ae5a5' as Flavor<string, 'ContractID'>,
        rentStartDate: '2021-05-31T21:00:00Z',
        ownerInfo: {
            person: {
                name: 'И/Контракта',
                surname: 'Ф/контракта',
            },
            phone: '+79042166222',
            inn: '132808730606',
            email: 'contractOwner@mail.ru',
            bankInfo: {
                accountNumber: '33333333333333333333',
                bic: '043333333',
            },
        },
        tenantInfo: {
            person: {
                name: 'И/Контракта',
                surname: 'Ф/Контракта',
                patronymic: 'О/Контракта',
            },
            phone: '+79040954444',
            email: 'contractTenant@mail.ru',
        },
    },
};

export const contractAndAssigned: IManagerFlat = {
    flat: {
        flatId: '4960e34dbf2a4ef7b3f35a8d365c9577' as Flavor<string, 'FlatID'>,
        address: {
            address: 'г. Москва, улица Московская, д 5, кв. 343',
            flatNumber: '77',
        },
        userRole: FlatUserRole.OWNER,
        status: FlatStatus.RENTED,
        assignedUsers: [
            {
                phone: '+79042166222',
                person: {
                    name: 'И/Привязки',
                    surname: 'Ф/Привязки',
                },
                userRole: FlatUserRole.OWNER,
                email: 'assignedOwner@yandex.ru',
                userId: 'd3ed5d9e8f47' as Flavor<string, 'UserId'>,
            },
            {
                phone: '+79040954444',
                person: {
                    name: 'И/Привязки',
                    surname: 'Ф/Привязки',
                    patronymic: 'О/Привязки',
                },
                userRole: FlatUserRole.TENANT,
                email: 'assignedTenant@mail.ru',
                userId: '4015472092' as Flavor<string, 'UserId'>,
            },
        ],
        code: '12-DDFF',
    },
    actualContract: {
        contractId: '6c1f6cb68a184604aa5a8bbea53ae5a5' as Flavor<string, 'ContractID'>,
        rentStartDate: '2021-05-31T21:00:00Z',
        ownerInfo: {
            person: {
                name: 'И/Контракта',
                surname: 'Ф/Контракта',
            },
            phone: '+79042166222',
            inn: '132808730606',
            email: 'contractOwner@mail.ru',
            bankInfo: {
                accountNumber: '33333333333333333333',
                bic: '043333333',
            },
        },
        tenantInfo: {
            person: {
                name: 'И/Контракта',
                surname: 'Ф/Контракта',
                patronymic: 'О/Контракта',
            },
            phone: '+79040954444',
            email: 'contractTenantt@mail.ru',
        },
        rentAmount: '6000000',
        paymentDayOfMonth: 4,
    },
};

export const emptyData: IManagerFlat = {
    flat: {
        flatId: '223' as Flavor<string, 'FlatID'>,
        address: {
            address: ' г. Москва, улица Московская, д 5, кв. 343',
            flatNumber: '23',
        },
        userRole: FlatUserRole.TENANT,
        assignedUsers: [],
        status: FlatStatus.DRAFT,
        code: '12-DDFF',
    },
    actualContract: {},
};

export const hasAmoLink: IManagerFlat = {
    flat: {
        flatId: '223' as Flavor<string, 'FlatID'>,
        address: {
            address: ' г. Москва, улица Московская, д 5, кв. 343',
            flatNumber: '12345',
        },
        person: {
            name: 'И/Заявки',
            surname: 'Ф/Заявки',
            patronymic: 'О/Заявки',
        },
        userRole: FlatUserRole.OWNER,
        assignedUsers: [],
        status: FlatStatus.DRAFT,
        code: '12-DDFF',
    },
    amoLeadLink: 'https://yandexarenda.amocrm.ru/leads/detail/17388243',
    actualContract: {},
};

export const hasTerminationDate: IManagerFlat = {
    flat: {
        flatId: '223' as Flavor<string, 'FlatID'>,
        address: {
            address: ' г. Москва, улица Московская, д 5, кв. 343',
            flatNumber: '12345',
        },
        person: {
            name: 'И/Заявки',
            surname: 'Ф/Заявки',
            patronymic: 'О/Заявки',
        },
        userRole: FlatUserRole.OWNER,
        assignedUsers: [],
        status: FlatStatus.DRAFT,
        code: '12-DDFF',
    },
    amoLeadLink: 'https://yandexarenda.amocrm.ru/leads/detail/17388243',
    actualContract: {
        terminationInfo: {
            date: '2022-03-24T21:00:00Z',
            legalTerminationDate: '2022-03-25T21:00:00Z',
        },
    },
};

export const hasPaymentOverdueDays: IManagerFlat = {
    flat: {
        flatId: '223' as Flavor<string, 'FlatID'>,
        address: {
            address: ' г. Москва, улица Московская, д 5, кв. 343',
            flatNumber: '12345',
        },
        person: {
            name: 'И/Заявки',
            surname: 'Ф/Заявки',
            patronymic: 'О/Заявки',
        },
        userRole: FlatUserRole.OWNER,
        assignedUsers: [],
        status: FlatStatus.DRAFT,
        code: '12-DDFF',
    },
    amoLeadLink: 'https://yandexarenda.amocrm.ru/leads/detail/17388243',
    actualContract: {
        terminationInfo: {
            date: '2022-03-24T21:00:00Z',
            legalTerminationDate: '2022-03-25T21:00:00Z',
        },
    },
    paymentOverdueDays: 7,
};

export const hasPaymentInfo: IManagerFlat = {
    flat: {
        flatId: '223' as Flavor<string, 'FlatID'>,
        address: {
            address: ' г. Москва, улица Московская, д 5, кв. 343',
            flatNumber: '12345',
        },
        userRole: FlatUserRole.OWNER,
        status: FlatStatus.CONFIRMED,
        assignedUsers: [],
        code: '12-DDFF',
    },
    actualContract: {
        contractId: '6c1f6cb68a184604aa5a8bbea53ae5a5' as Flavor<string, 'ContractID'>,
        rentStartDate: '2021-02-31T21:00:00Z',
        ownerInfo: {
            person: {
                name: 'И/Контракта',
                surname: 'Ф/Контракта',
            },
            phone: '+79042166222',
            inn: '132808730606',
            email: 'conractOwner@mail.ru',
            bankInfo: {
                accountNumber: '33333333333333333333',
                bic: '043333333',
            },
        },
        tenantInfo: {
            person: {
                name: 'И/Контракта',
                surname: 'Ф/Контракта',
            },
            phone: '+79040954444',
            email: 'conractTenant@mail.ru',
        },
        rentAmount: '6000000',
        paymentDayOfMonth: 1,
    },
};

export const hasLastFebruaryLastMonthDay: IManagerFlat = {
    flat: {
        flatId: '223' as Flavor<string, 'FlatID'>,
        address: {
            address: ' г. Москва, улица Московская, д 5, кв. 343',
            flatNumber: '12345',
        },
        userRole: FlatUserRole.OWNER,
        status: FlatStatus.CONFIRMED,
        assignedUsers: [],
        code: '12-DDFF',
    },
    actualContract: {
        contractId: '6c1f6cb68a184604aa5a8bbea53ae5a5' as Flavor<string, 'ContractID'>,
        rentStartDate: '2021-01-31T21:00:00Z',
        ownerInfo: {
            person: {
                name: 'И/Контракта',
                surname: 'Ф/Контракта',
            },
            phone: '+79042166222',
            inn: '132808730606',
            email: 'contractOwner@mail.ru',
            bankInfo: {
                accountNumber: '33333333333333333333',
                bic: '043333333',
            },
        },
        tenantInfo: {
            person: {
                name: 'И/Контракта',
                surname: 'Ф/Контракта',
            },
            phone: '+79040954444',
            email: 'contractTenant@mail.ru',
        },
        rentAmount: '6000000',
        paymentDayOfMonth: 31,
    },
};

export const hasPaymentNextYear: IManagerFlat = {
    flat: {
        flatId: '223' as Flavor<string, 'FlatID'>,
        address: {
            address: ' г. Москва, улица Московская, д 5, кв. 343',
            flatNumber: '12345',
        },
        userRole: FlatUserRole.OWNER,
        status: FlatStatus.CONFIRMED,
        assignedUsers: [],
        desiredRentAmount: '50000',
        code: '12-DDFF',
    },
    actualContract: {
        contractId: '6c1f6cb68a184604aa5a8bbea53ae5a5' as Flavor<string, 'ContractID'>,
        rentStartDate: '2022-02-31T21:00:00Z',
        ownerInfo: {
            person: {
                name: 'И/Контракта',
                surname: 'Ф/Контракта',
            },
            phone: '+79042166222',
            inn: '132808730606',
            email: 'contractOnwer@mail.ru',
            bankInfo: {
                accountNumber: '33333333333333333333',
                bic: '043333333',
            },
        },
        tenantInfo: {
            person: {
                name: 'И/Контракта',
                surname: 'Ф/Контракта',
            },
            phone: '+79040954444',
            email: 'contractTenant@mail.ru',
        },
        rentAmount: '6000000',
        paymentDayOfMonth: 20,
    },
};

export const createByModeration: IManagerFlat = {
    flat: {
        flatId: '2283' as Flavor<string, 'FlatID'>,
        address: {
            address: ' г. Москва, улица Московская, д 5, кв. 343',
            flatNumber: '12345',
        },
        person: {
            name: 'И/Заявки',
            surname: 'Ф/Заявки',
            patronymic: 'О/Заявки',
        },
        phone: '+79040954444',
        email: 'applicationOwner@mail.ru',
        userRole: FlatUserRole.OWNER,
        assignedUsers: [],
        desiredRentAmount: '3000',
        status: FlatStatus.DRAFT,
        code: '12-DDFF',
    },
    actualContract: {},
};

export const createByOwner: IManagerFlat = {
    flat: {
        flatId: '2283' as Flavor<string, 'FlatID'>,
        address: {
            address: ' г. Москва, улица Московская, д 5, кв. 343',
            flatNumber: '12345',
        },
        person: {
            name: 'И/Заявки',
            surname: 'Ф/Заявки',
            patronymic: 'О/Заявки',
        },
        phone: '+79040954444',
        email: 'applicationOwner@mail.ru',
        userRole: FlatUserRole.OWNER,
        assignedUsers: [],
        desiredRentAmount: '3000',
        status: FlatStatus.DRAFT,
        code: '12-DDFF',
    },
    actualContract: {},
};

export const flatStatus = (): IManagerFlat[] => {
    const flats: IManagerFlat[] = Object.values(FlatStatus)
        .filter((status) => status !== FlatStatus.UNKNOWN)
        .map((status) => ({
            ...contractAndAssigned,
            flat: {
                ...contractAndAssigned.flat,
                status,
            },
        }));

    return flats;
};

import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { PassportVerificationStatus, UserId } from 'types/user';
import { FlatStatus, FlatUserRole } from 'types/flat';

import { IUniversalStore } from 'view/modules/types';

const order = ['d23b4175ba2c', '88f6d8841dfe', '343055d6c731', '34fdsf3434'] as DeepPartial<UserId[]>;

export const baseStore: DeepPartial<IUniversalStore> = {
    managerSearchUsers: {
        order: [] as DeepPartial<UserId[]>,
        map: {},
        paging: {
            page: {
                num: 1,
                size: 10,
            },
            total: 0,
            pageCount: 0,
        },
    },
};

export const onPending: DeepPartial<IUniversalStore> = {
    spa: {
        status: RequestStatus.PENDING,
    },
};

export const toTenUsers: DeepPartial<IUniversalStore> = {
    managerSearchUsers: {
        order,
        map: {
            ['d23b4175ba2c' as UserId]: {
                user: {
                    phone: '+79111597792',
                    person: {
                        name: 'Евпатий',
                        surname: 'Коловрат',
                    },
                    passportVerificationStatus: PassportVerificationStatus.ABSENT,
                    hasAcceptedTermsOfUse: true,
                    email: 'qwerqw@yandex.com',
                    calculatedInfo: {
                        hasOwnerRequests: true,
                        isTenant: false,
                    },
                    tenantQuestionnaire: {},
                    userId: 'd23b4175ba2c' as UserId,
                },
                assignedFlats: [
                    {
                        flatId: '5d199f9f095d4521957f1dd11f6e7ea1',
                        address: {
                            address: 'г Санкт-Петербург, Пискарёвский пр-кт, д 3',
                            flatNumber: '200',
                        },
                        status: FlatStatus.DENIED,
                        userRole: FlatUserRole.OWNER,
                    },
                ],
            },
            ['88f6d8841dfe' as UserId]: {
                user: {
                    phone: '+79992134916',
                    person: {
                        name: 'Иванов',
                        surname: 'Иван',
                        patronymic: 'Иванович',
                    },
                    passportVerificationStatus: PassportVerificationStatus.UPLOADED,
                    hasAcceptedTermsOfUse: true,
                    calculatedInfo: {
                        hasOwnerRequests: false,
                        isTenant: true,
                    },
                    tenantQuestionnaire: {},
                    userId: '88f6d8841dfe' as UserId,
                },
                assignedFlats: [
                    {
                        flatId: 'a7629edc17ed41f6a1c730c6f0ff0262',
                        address: {
                            address: 'г Санкт-Петербург, пр-кт Энергетиков, д 30 к 1',
                            flatNumber: '154',
                        },
                        status: FlatStatus.RENTED,
                        userRole: FlatUserRole.TENANT,
                    },
                ],
            },
            ['343055d6c731' as UserId]: {
                user: {
                    phone: '+79043333333',
                    person: {
                        name: 'Петр',
                        surname: 'Валентинов',
                    },
                    passportVerificationStatus: PassportVerificationStatus.UPLOADED,
                    hasAcceptedTermsOfUse: true,
                    email: 'ztuzextended5@yandex.ru',
                    calculatedInfo: {
                        hasOwnerRequests: true,
                        isTenant: false,
                    },
                    tenantQuestionnaire: {},
                    userId: '343055d6c731' as UserId,
                },
                assignedFlats: [
                    {
                        flatId: 'adf2a50a7613498a98af390fd3227a4f',
                        address: {
                            address: 'г Санкт-Петербург, пр-кт Энергетиков, д 30 к 1',
                            flatNumber: '77',
                        },
                        status: FlatStatus.CONFIRMED,
                        userRole: FlatUserRole.OWNER,
                    },
                ],
            },
            ['34fdsf3434' as UserId]: {
                user: {
                    phone: '+79043333333',
                    person: {
                        name: 'Петр',
                        surname: 'Валентинов',
                    },
                    passportVerificationStatus: PassportVerificationStatus.VERIFIED,
                    hasAcceptedTermsOfUse: true,
                    email: 'ztuzextended5@yandex.ru',
                    calculatedInfo: {
                        hasOwnerRequests: true,
                        isTenant: false,
                    },
                    tenantQuestionnaire: {},
                    userId: '343055d6c731' as UserId,
                },
                assignedFlats: [
                    {
                        flatId: 'adf2a50a7613498a98af390fd3227a4f',
                        address: {
                            address: 'г Санкт-Петербург, пр-кт Энергетиков, д 30 к 1',
                            flatNumber: '77',
                        },
                        status: FlatStatus.CONFIRMED,
                        userRole: FlatUserRole.OWNER,
                    },
                ],
            },
        },
        paging: {
            page: {
                num: 1,
                size: 10,
            },
            total: 4,
            pageCount: 1,
        },
    },
};

export const toLastPage: DeepPartial<IUniversalStore> = {
    managerSearchUsers: {
        order,
        map: {
            ...toTenUsers.managerSearchUsers?.map,
        },
        paging: {
            page: {
                num: 3,
                size: 10,
            },
            total: 4,
            pageCount: 3,
        },
    },
};

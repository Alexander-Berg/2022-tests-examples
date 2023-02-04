import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { TenantQuestionnaireModerationStatus, UserId, UserPersonalActivity } from 'types/user';
import { FlatUserRole, FlatId, ShowingId, AssignedGroupUsersStatus } from 'types/flat';
import { NaturalPersonCheckResolution, NaturalPersonCheckStatus } from 'types/userChecks';

import { IUniversalStore } from 'view/modules/types';

export const filledStore: DeepPartial<IUniversalStore> = {
    spa: {
        status: RequestStatus.LOADED,
    },
    config: {
        realtyUrl: 'https://realty.yandex.ru',
        serverTimeStamp: new Date('03-09-2021').getTime(),
    },
    ownerFlat: {
        flatId: '87646c27479b421e8a9e3fec7c5b086a' as FlatId,
        assignedUsers: [
            {
                phone: '+79112473328',
                person: {
                    name: 'Гомер',
                    surname: 'Симпсон',
                },
                userRole: FlatUserRole.OWNER,
                email: 'zooeyOwner1@yandex.ru',
                userId: '4026826176' as UserId,
            },
        ],
        assignedGroupUsers: [
            {
                showingId: 'da3eb46ff27145c7829b7d79df578629' as ShowingId,
                assignedUser: [
                    {
                        phone: '+79650247945',
                        person: {
                            name: 'Петя',
                            surname: 'Тестовый',
                            patronymic: 'Гомерович',
                        },
                        userRole: FlatUserRole.TENANT_CANDIDATE,
                        email: 'email0000@yandex.ru',
                        userId: 'e77a4c0bac41' as UserId,
                        approvedTenantQuestionnaire: {
                            personalActivity: {
                                activity: UserPersonalActivity.STUDY,
                                educationalInstitution: '123',
                            },
                            additionalTenant: 'Один',
                            reasonForRelocation: 'Лежу на полке',
                            selfDescription: 'Вкусный',
                            hasChildren: false,
                            hasPets: false,
                            petsInfo: '',
                        },
                        naturalPersonCheckStatus: NaturalPersonCheckStatus.READY,
                        naturalPersonCheckResolution: NaturalPersonCheckResolution.VALID,
                        tenantQuestionnaireModerationStatus: TenantQuestionnaireModerationStatus.IN_PROGRESS,
                        proceedingExecutiveStatus: NaturalPersonCheckStatus.READY,
                        proceedingExecutiveResolution: NaturalPersonCheckResolution.VALID,
                    },
                ],
                status: AssignedGroupUsersStatus.CONSIDERATION,
            },
            {
                showingId: 'da3eb46ff27145c7829b7d79df578629' as ShowingId,
                assignedUser: [
                    {
                        phone: '+79650247945',
                        person: {
                            name: 'Петя',
                            surname: 'Тестовый',
                            patronymic: 'Гомерович',
                        },
                        userRole: FlatUserRole.TENANT_CANDIDATE,
                        email: 'email0000@yandex.ru',
                        userId: 'e77a4c0bac41' as UserId,
                        approvedTenantQuestionnaire: {
                            personalActivity: {
                                activity: UserPersonalActivity.STUDY,
                                educationalInstitution: '123',
                            },
                            additionalTenant: 'С семьей',
                            reasonForRelocation: 'Лежу на полке',
                            selfDescription: 'Вкусный',
                            hasChildren: false,
                            hasPets: false,
                            petsInfo: '',
                            numberOfBabies: 2,
                        },
                        naturalPersonCheckStatus: NaturalPersonCheckStatus.READY,
                        naturalPersonCheckResolution: NaturalPersonCheckResolution.VALID,
                        tenantQuestionnaireModerationStatus: TenantQuestionnaireModerationStatus.IN_PROGRESS,
                        proceedingExecutiveStatus: NaturalPersonCheckStatus.READY,
                        proceedingExecutiveResolution: NaturalPersonCheckResolution.VALID,
                    },
                    {
                        phone: '+79665343434',
                        person: {
                            name: 'Маша',
                            surname: 'Тестест',
                            patronymic: 'СнимаюИзменено',
                        },
                        userRole: FlatUserRole.TENANT_CANDIDATE,
                        email: 'test1test2@yandex.ru',
                        userId: 'e77a4c0bac41' as UserId,
                        approvedTenantQuestionnaire: {
                            personalActivity: {
                                activity: UserPersonalActivity.STUDY,
                                educationalInstitution: '123',
                            },
                            additionalTenant: 'Один',
                            reasonForRelocation: 'Причина переезд1',
                            selfDescription: 'Лучший',
                            hasChildren: false,
                            hasPets: false,
                            petsInfo: '',
                        },
                        naturalPersonCheckStatus: NaturalPersonCheckStatus.READY,
                        naturalPersonCheckResolution: NaturalPersonCheckResolution.VALID,
                        tenantQuestionnaireModerationStatus: TenantQuestionnaireModerationStatus.IN_PROGRESS,
                        proceedingExecutiveStatus: NaturalPersonCheckStatus.READY,
                        proceedingExecutiveResolution: NaturalPersonCheckResolution.VALID,
                    },
                ],
                status: AssignedGroupUsersStatus.APPROVED,
            },
            {
                showingId: 'da3eb46ff27145c7829b7d79df578639' as ShowingId,
                assignedUser: [
                    {
                        phone: '+79650247945',
                        person: {
                            name: 'Вася',
                            surname: 'Тестовый',
                            patronymic: 'Гомерович',
                        },
                        userRole: FlatUserRole.TENANT_CANDIDATE,
                        email: 'email0000@yandex.ru',
                        userId: 'e77a4c0bac41' as UserId,
                        approvedTenantQuestionnaire: {
                            personalActivity: {
                                activity: UserPersonalActivity.STUDY,
                                educationalInstitution: '123',
                            },
                            additionalTenant: 'С семьей',
                            reasonForRelocation: 'Лежу на полке',
                            selfDescription: 'Вкусный',
                            hasChildren: false,
                            hasPets: false,
                            petsInfo: '',
                            numberOfBabies: 3,
                        },
                        naturalPersonCheckStatus: NaturalPersonCheckStatus.READY,
                        naturalPersonCheckResolution: NaturalPersonCheckResolution.VALID,
                        tenantQuestionnaireModerationStatus: TenantQuestionnaireModerationStatus.IN_PROGRESS,
                        proceedingExecutiveStatus: NaturalPersonCheckStatus.READY,
                        proceedingExecutiveResolution: NaturalPersonCheckResolution.VALID,
                    },
                    {
                        phone: '+79665343434',
                        person: {
                            name: 'Леша',
                            surname: 'Тестест',
                            patronymic: 'СнимаюИзменено',
                        },
                        userRole: FlatUserRole.TENANT_CANDIDATE,
                        email: 'test1test2@yandex.ru',
                        userId: 'e77a4c0bac41' as UserId,
                        approvedTenantQuestionnaire: {
                            personalActivity: {
                                activity: UserPersonalActivity.STUDY,
                                educationalInstitution: '123',
                            },
                            additionalTenant: 'Один',
                            reasonForRelocation: 'Причина переезд1',
                            selfDescription: 'Лучший',
                            hasChildren: true,
                            hasPets: true,
                            petsInfo: '',
                        },
                        naturalPersonCheckStatus: NaturalPersonCheckStatus.READY,
                        naturalPersonCheckResolution: NaturalPersonCheckResolution.VALID,
                        tenantQuestionnaireModerationStatus: TenantQuestionnaireModerationStatus.IN_PROGRESS,
                        proceedingExecutiveStatus: NaturalPersonCheckStatus.READY,
                        proceedingExecutiveResolution: NaturalPersonCheckResolution.VALID,
                    },
                    {
                        phone: '+79665343434',
                        person: {
                            name: 'Дима',
                            surname: 'Тестест',
                            patronymic: 'СнимаюИзменено',
                        },
                        userRole: FlatUserRole.TENANT_CANDIDATE,
                        email: 'test1test2@yandex.ru',
                        userId: 'e77a4c0bac41' as UserId,
                        approvedTenantQuestionnaire: {
                            personalActivity: {
                                activity: UserPersonalActivity.STUDY,
                                educationalInstitution: '123',
                            },
                            additionalTenant: 'Один',
                            reasonForRelocation: 'Причина переезд1',
                            selfDescription: 'Лучший',
                            hasChildren: true,
                            hasPets: true,
                            petsInfo: '',
                        },
                        naturalPersonCheckStatus: NaturalPersonCheckStatus.READY,
                        naturalPersonCheckResolution: NaturalPersonCheckResolution.VALID,
                        tenantQuestionnaireModerationStatus: TenantQuestionnaireModerationStatus.IN_PROGRESS,
                        proceedingExecutiveStatus: NaturalPersonCheckStatus.READY,
                        proceedingExecutiveResolution: NaturalPersonCheckResolution.VALID,
                    },
                ],
                status: AssignedGroupUsersStatus.DECLINED,
            },
            {
                showingId: 'da3eb46ff27145c7829b7d79df578639' as ShowingId,
                assignedUser: [
                    {
                        phone: '+79650247945',
                        person: {
                            name: 'Максимилиановский-Беловский',
                            surname: 'Тестовый',
                            patronymic: 'Гомерович',
                        },
                        userRole: FlatUserRole.TENANT_CANDIDATE,
                        email: 'email0000@yandex.ru',
                        userId: 'e77a4c0bac41' as UserId,
                        approvedTenantQuestionnaire: {
                            personalActivity: {
                                activity: UserPersonalActivity.STUDY,
                                educationalInstitution: '123',
                            },
                            additionalTenant: 'С семьей',
                            reasonForRelocation: 'Лежу на полке',
                            selfDescription: 'Вкусный',
                            hasChildren: false,
                            hasPets: false,
                            petsInfo: '',
                            numberOfBabies: 3,
                        },
                        naturalPersonCheckStatus: NaturalPersonCheckStatus.READY,
                        naturalPersonCheckResolution: NaturalPersonCheckResolution.VALID,
                        tenantQuestionnaireModerationStatus: TenantQuestionnaireModerationStatus.IN_PROGRESS,
                        proceedingExecutiveStatus: NaturalPersonCheckStatus.READY,
                        proceedingExecutiveResolution: NaturalPersonCheckResolution.VALID,
                    },
                    {
                        phone: '+79665343434',
                        person: {
                            name: 'ТестестТестест',
                            surname: 'Тестест',
                            patronymic: 'СнимаюИзменено',
                        },
                        userRole: FlatUserRole.TENANT_CANDIDATE,
                        email: 'test1test2@yandex.ru',
                        userId: 'e77a4c0bac41' as UserId,
                        approvedTenantQuestionnaire: {
                            personalActivity: {
                                activity: UserPersonalActivity.STUDY,
                                educationalInstitution: '123',
                            },
                            additionalTenant: 'Один',
                            reasonForRelocation: 'Причина переезд1',
                            selfDescription: 'Лучший',
                            hasChildren: true,
                            hasPets: true,
                            petsInfo: '',
                        },
                        naturalPersonCheckStatus: NaturalPersonCheckStatus.READY,
                        naturalPersonCheckResolution: NaturalPersonCheckResolution.VALID,
                        tenantQuestionnaireModerationStatus: TenantQuestionnaireModerationStatus.IN_PROGRESS,
                        proceedingExecutiveStatus: NaturalPersonCheckStatus.READY,
                        proceedingExecutiveResolution: NaturalPersonCheckResolution.VALID,
                    },
                    {
                        phone: '+79665343434',
                        person: {
                            name: 'ГомеровичГомерович',
                            surname: 'Тестест',
                            patronymic: 'СнимаюИзменено',
                        },
                        userRole: FlatUserRole.TENANT_CANDIDATE,
                        email: 'test1test2@yandex.ru',
                        userId: 'e77a4c0bac41' as UserId,
                        approvedTenantQuestionnaire: {
                            personalActivity: {
                                activity: UserPersonalActivity.STUDY,
                                educationalInstitution: '123',
                            },
                            additionalTenant: 'Один',
                            reasonForRelocation: 'Причина переезд1',
                            selfDescription: 'Лучший',
                            hasChildren: true,
                            hasPets: true,
                            petsInfo: '',
                        },
                        naturalPersonCheckStatus: NaturalPersonCheckStatus.READY,
                        naturalPersonCheckResolution: NaturalPersonCheckResolution.VALID,
                        tenantQuestionnaireModerationStatus: TenantQuestionnaireModerationStatus.IN_PROGRESS,
                        proceedingExecutiveStatus: NaturalPersonCheckStatus.READY,
                        proceedingExecutiveResolution: NaturalPersonCheckResolution.VALID,
                    },
                ],
                status: AssignedGroupUsersStatus.CONSIDERATION,
            },
        ],
    },
};

export const emptyStore = {
    ...filledStore,
    ownerFlat: {
        ...filledStore.ownerFlat,
        assignedGroupUsers: [],
    },
};

export const skeletonStore = {
    ...filledStore,
    ownerFlat: {
        ...filledStore.ownerFlat,
    },
    spa: {
        status: RequestStatus.PENDING,
    },
};

export const filledStoreWithOnlyContent = {
    ...filledStore,
    cookies: {
        'only-content': '1',
    },
};

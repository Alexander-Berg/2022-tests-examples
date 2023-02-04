import { RequestStatus } from 'realty-core/types/network';

import { TenantQuestionnaireModerationStatus, UserId, UserPersonalActivity } from 'types/user';
import { FlatUserRole, FlatId, ShowingId, AssignedGroupUsersStatus } from 'types/flat';
import { NaturalPersonCheckResolution, NaturalPersonCheckStatus } from 'types/userChecks';

import { IBreadcrumb } from 'types/breadcrumbs';

import { IBreadcrumbsStore } from 'view/modules/breadcrumbs/reducers';

const breadcrumbs: IBreadcrumbsStore = {
    crumbs: [
        {
            route: 'user-flat',
            params: {
                flatId: 'e65ecdea675e411d90842f44eddcbcfd',
            },
        },
        {
            route: 'owner-flat-tenant-candidate-groups',
            params: {
                flatId: 'e65ecdea675e411d90842f44eddcbcfd',
            },
        },
    ],
    current: {
        route: 'owner-flat-tenant-candidate-group',
    } as IBreadcrumb,
};

const config = {
    realtyUrl: 'https://realty.yandex.ru',
    serverTimeStamp: new Date('03-09-2021').getTime(),
};

export const filledStore = {
    breadcrumbs,
    page: {
        params: {
            showingId: 'da3eb46ff27145c7829b7d79df578639' as ShowingId,
        },
    },
    spa: {
        status: RequestStatus.LOADED,
    },
    config,
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
                userId: '40268261761' as UserId,
            },
        ],
        assignedGroupUsers: [
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
                        userId: 'e77a4c0bac412' as UserId,
                        tenantCandidateAge: 18,
                        approvedTenantQuestionnaire: {
                            personalActivity: {
                                activity: UserPersonalActivity.WORK_AND_STUDY,
                                educationalInstitution: 'СПБГЭТУ "ЛЭТИ"',
                                aboutWorkAndPosition: 'ООО Яндекс Вертикали, MC',
                                aboutBusiness: '',
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
                        userId: 'e77a4c0bac413' as UserId,
                        tenantCandidateAge: 21,
                        approvedTenantQuestionnaire: {
                            personalActivity: {
                                activity: UserPersonalActivity.WORK_AND_STUDY,
                                educationalInstitution: 'СПБГЭТУ "ЛЭТИ"',
                                aboutWorkAndPosition: 'ООО Яндекс Вертикали, MC',
                                aboutBusiness: '',
                            },
                            additionalTenant: 'Один',
                            socialNetworkLink: '',
                            reasonForRelocation:
                                'Вынужден покинуть прекрасное гнёздышко, тк хозяин решил продать свою квартиру',
                            selfDescription:
                                'Интроверт, который иногда не прочь поболтать. ' +
                                'Люблю тишину, поэтому уже много лет живу без телека, а музыка и кино в наушниках. ' +
                                // eslint-disable-next-line max-len
                                'Вредных привычек нет. Часто делаю уборку. Гостей у меня не бывает вообще и я искренне считаю, ' +
                                'что личную жизнь нужно устраивать за пределами квартиры.',
                            hasChildren: true,
                            numberOfTeenagers: 1,
                            numberOfSchoolchildren: 2,
                            numberOfPreschoolers: 3,
                            numberOfBabies: 4,
                            hasPets: true,
                            petsInfo:
                                'Умка и Кузя, кот и пёс. Они взрослые и воспитанные, ' +
                                'равнодушны к мебели и дела делают четко в ' +
                                'отведенные для этого места. Кот кастрирован. За животных ручаюсь!',
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
                        userId: 'e77a4c0bac414' as UserId,
                        tenantCandidateAge: 25,
                        approvedTenantQuestionnaire: {
                            personalActivity: {
                                activity: UserPersonalActivity.WORK_AND_STUDY,
                                educationalInstitution: 'СПБГЭТУ "ЛЭТИ"',
                                aboutWorkAndPosition: 'ООО Яндекс Вертикали, MC',
                                aboutBusiness: '',
                            },
                            additionalTenant: 'Один',
                            socialNetworkLink: '',
                            reasonForRelocation:
                                'Вынужден покинуть прекрасное гнёздышко, тк хозяин решил продать свою квартиру',
                            selfDescription:
                                'Интроверт, который иногда не прочь поболтать. ' +
                                'Люблю тишину, поэтому уже много лет живу без телека, а музыка и кино в наушниках. ' +
                                // eslint-disable-next-line max-len
                                'Вредных привычек нет. Часто делаю уборку. Гостей у меня не бывает вообще и я искренне считаю, ' +
                                'что личную жизнь нужно устраивать за пределами квартиры.',
                            hasChildren: true,
                            numberOfTeenagers: 1,
                            numberOfSchoolchildren: 2,
                            numberOfPreschoolers: 3,
                            numberOfBabies: 4,
                            hasPets: true,
                            petsInfo:
                                'Умка и Кузя, кот и пёс. Они взрослые и воспитанные, ' +
                                'равнодушны к мебели и дела делают четко в ' +
                                'отведенные для этого места. Кот кастрирован. За животных ручаюсь!',
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

export const longNameStore = {
    breadcrumbs,
    page: {
        params: {
            showingId: 'da3eb46ff27145c7829b7d79df578639' as ShowingId,
        },
    },
    spa: {
        status: RequestStatus.LOADED,
    },
    config,
    ownerFlat: {
        flatId: '87646c27479b421e8a9e3fec7c5b086a' as FlatId,
        assignedUsers: [
            {
                phone: '+79112473328',
                person: {
                    name: 'Максимилиановский-Беловский',
                    surname: 'Симпсон',
                },
                userRole: FlatUserRole.OWNER,
                email: 'zooeyOwner1@yandex.ru',
                userId: '40268261761' as UserId,
            },
        ],
        assignedGroupUsers: [
            {
                showingId: 'da3eb46ff27145c7829b7d79df578639' as ShowingId,
                assignedUser: [
                    {
                        phone: '+79650247945',
                        person: {
                            name: 'ГомеровичГомерович',
                            surname: 'Тестовый',
                            patronymic: 'Гомерович',
                        },
                        userRole: FlatUserRole.TENANT_CANDIDATE,
                        email: 'email0000@yandex.ru',
                        userId: 'e77a4c0bac412' as UserId,
                        tenantCandidateAge: 18,
                        approvedTenantQuestionnaire: {
                            personalActivity: {
                                activity: UserPersonalActivity.WORK_AND_STUDY,
                                educationalInstitution: 'СПБГЭТУ "ЛЭТИ"',
                                aboutWorkAndPosition: 'ООО Яндекс Вертикали, MC',
                                aboutBusiness: '',
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
                        userId: 'e77a4c0bac413' as UserId,
                        tenantCandidateAge: 21,
                        approvedTenantQuestionnaire: {
                            personalActivity: {
                                activity: UserPersonalActivity.WORK_AND_STUDY,
                                educationalInstitution: 'СПБГЭТУ "ЛЭТИ"',
                                aboutWorkAndPosition: 'ООО Яндекс Вертикали, MC',
                                aboutBusiness: '',
                            },
                            additionalTenant: 'Один',
                            socialNetworkLink: '',
                            reasonForRelocation:
                                'Вынужден покинуть прекрасное гнёздышко, тк хозяин решил продать свою квартиру',
                            selfDescription:
                                'Интроверт, который иногда не прочь поболтать. ' +
                                'Люблю тишину, поэтому уже много лет живу без телека, а музыка и кино в наушниках. ' +
                                // eslint-disable-next-line max-len
                                'Вредных привычек нет. Часто делаю уборку. Гостей у меня не бывает вообще и я искренне считаю, ' +
                                'что личную жизнь нужно устраивать за пределами квартиры.',
                            hasChildren: true,
                            numberOfTeenagers: 1,
                            numberOfSchoolchildren: 2,
                            numberOfPreschoolers: 3,
                            numberOfBabies: 4,
                            hasPets: true,
                            petsInfo:
                                'Умка и Кузя, кот и пёс. Они взрослые и воспитанные, ' +
                                'равнодушны к мебели и дела делают четко в ' +
                                'отведенные для этого места. Кот кастрирован. За животных ручаюсь!',
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
                        userId: 'e77a4c0bac414' as UserId,
                        tenantCandidateAge: 25,
                        approvedTenantQuestionnaire: {
                            personalActivity: {
                                activity: UserPersonalActivity.WORK_AND_STUDY,
                                educationalInstitution: 'СПБГЭТУ "ЛЭТИ"',
                                aboutWorkAndPosition: 'ООО Яндекс Вертикали, MC',
                                aboutBusiness: '',
                            },
                            additionalTenant: 'Один',
                            socialNetworkLink: '',
                            reasonForRelocation:
                                'Вынужден покинуть прекрасное гнёздышко, тк хозяин решил продать свою квартиру',
                            selfDescription:
                                'Интроверт, который иногда не прочь поболтать. ' +
                                'Люблю тишину, поэтому уже много лет живу без телека, а музыка и кино в наушниках. ' +
                                // eslint-disable-next-line max-len
                                'Вредных привычек нет. Часто делаю уборку. Гостей у меня не бывает вообще и я искренне считаю, ' +
                                'что личную жизнь нужно устраивать за пределами квартиры.',
                            hasChildren: true,
                            numberOfTeenagers: 1,
                            numberOfSchoolchildren: 2,
                            numberOfPreschoolers: 3,
                            numberOfBabies: 4,
                            hasPets: true,
                            petsInfo:
                                'Умка и Кузя, кот и пёс. Они взрослые и воспитанные, ' +
                                'равнодушны к мебели и дела делают четко в ' +
                                'отведенные для этого места. Кот кастрирован. За животных ручаюсь!',
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
    breadcrumbs,
    spa: {
        status: RequestStatus.LOADED,
    },
    config,
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
                userId: '40268261765' as UserId,
            },
        ],
    },
};

export const skeletonStore = {
    breadcrumbs,
    spa: {
        status: RequestStatus.PENDING,
    },
    config,
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
                userId: '40268261766' as UserId,
            },
        ],
    },
};

export const filledStoreWithOnlyContent = {
    ...filledStore,
    cookies: {
        'only-content': '1',
    },
};

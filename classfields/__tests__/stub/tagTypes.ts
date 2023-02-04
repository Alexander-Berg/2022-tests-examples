import { TenantQuestionnaireModerationStatus, UserId, UserPersonalActivity } from 'types/user';
import { FlatUserRole, IFlatTenant, IFlatUser } from 'types/flat';
import { NaturalPersonCheckResolution, NaturalPersonCheckStatus } from 'types/userChecks';

import { ITenantGroupTagType } from 'view/libs/tenantGroup/types';

const commonUser = {
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
            educationalInstitution: 'СПБГЭТУ "ЛЭТИ"',
            aboutWorkAndPosition: '',
            aboutBusiness: '',
        },
        additionalTenant: 'С друзьями',
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
};

export const testCases: Array<{ data: Array<IFlatUser | IFlatTenant>; expect: ITenantGroupTagType[] }> = [
    {
        data: [
            {
                ...commonUser,
                approvedTenantQuestionnaire: {
                    ...commonUser.approvedTenantQuestionnaire,
                    additionalTenant: 'Один',
                },
            },
        ],
        expect: [ITenantGroupTagType.SINGLE, ITenantGroupTagType.WITHOUT_CHILDREN, ITenantGroupTagType.WITHOUT_PETS],
    },
    {
        data: [
            {
                ...commonUser,
                approvedTenantQuestionnaire: {
                    ...commonUser.approvedTenantQuestionnaire,
                    additionalTenant: 'С семьей',
                },
            },
        ],
        expect: [
            ITenantGroupTagType.WITH_FAMILY,
            ITenantGroupTagType.WITHOUT_CHILDREN,
            ITenantGroupTagType.WITHOUT_PETS,
        ],
    },
    {
        data: [
            {
                ...commonUser,
                approvedTenantQuestionnaire: {
                    ...commonUser.approvedTenantQuestionnaire,
                    additionalTenant: 'С супругом/супругой',
                },
            },
        ],
        expect: [ITenantGroupTagType.WITH_WIFE, ITenantGroupTagType.WITHOUT_CHILDREN, ITenantGroupTagType.WITHOUT_PETS],
    },
    {
        data: [
            {
                ...commonUser,
                approvedTenantQuestionnaire: {
                    ...commonUser.approvedTenantQuestionnaire,
                    additionalTenant: 'С родственниками',
                },
            },
        ],
        expect: [
            ITenantGroupTagType.WITH_RELATIVES,
            ITenantGroupTagType.WITHOUT_CHILDREN,
            ITenantGroupTagType.WITHOUT_PETS,
        ],
    },
    {
        data: [
            {
                ...commonUser,
                approvedTenantQuestionnaire: {
                    ...commonUser.approvedTenantQuestionnaire,
                    additionalTenant: 'С парнем/девушкой',
                },
            },
        ],
        expect: [
            ITenantGroupTagType.WITH_GIRLFRIEND,
            ITenantGroupTagType.WITHOUT_CHILDREN,
            ITenantGroupTagType.WITHOUT_PETS,
        ],
    },
    {
        data: [
            {
                ...commonUser,
                approvedTenantQuestionnaire: {
                    ...commonUser.approvedTenantQuestionnaire,
                    additionalTenant: 'С другом/подругой',
                },
            },
        ],
        expect: [
            ITenantGroupTagType.WITH_FRIEND,
            ITenantGroupTagType.WITHOUT_CHILDREN,
            ITenantGroupTagType.WITHOUT_PETS,
        ],
    },
    {
        data: [
            {
                ...commonUser,
                approvedTenantQuestionnaire: {
                    ...commonUser.approvedTenantQuestionnaire,
                    additionalTenant: 'С друзьями',
                },
            },
        ],
        expect: [
            ITenantGroupTagType.WITH_FRIENDS,
            ITenantGroupTagType.WITHOUT_CHILDREN,
            ITenantGroupTagType.WITHOUT_PETS,
        ],
    },
    {
        data: [
            {
                ...commonUser,
                approvedTenantQuestionnaire: {
                    ...commonUser.approvedTenantQuestionnaire,
                    additionalTenant: 'С друзьями',
                    hasPets: true,
                },
            },
        ],
        expect: [ITenantGroupTagType.WITH_FRIENDS, ITenantGroupTagType.WITHOUT_CHILDREN, ITenantGroupTagType.WITH_PETS],
    },
    {
        data: [
            {
                ...commonUser,
                approvedTenantQuestionnaire: {
                    ...commonUser.approvedTenantQuestionnaire,
                    additionalTenant: 'С друзьями',
                    hasPets: true,
                    hasChildren: true,
                    numberOfBabies: 2,
                },
            },
        ],
        expect: [ITenantGroupTagType.WITH_FRIENDS, ITenantGroupTagType.WITH_CHILDREN, ITenantGroupTagType.WITH_PETS],
    },
    {
        data: [
            {
                ...commonUser,
                approvedTenantQuestionnaire: {
                    ...commonUser.approvedTenantQuestionnaire,
                    additionalTenant: 'С друзьями',
                    hasChildren: true,
                    numberOfBabies: 2,
                },
            },
        ],
        expect: [ITenantGroupTagType.WITH_FRIENDS, ITenantGroupTagType.WITH_CHILDREN, ITenantGroupTagType.WITHOUT_PETS],
    },
];

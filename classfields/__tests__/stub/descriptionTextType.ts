import { TenantQuestionnaireModerationStatus, UserId, UserPersonalActivity } from 'types/user';
import { FlatUserRole, IFlatTenant, IFlatUser } from 'types/flat';
import { NaturalPersonCheckResolution, NaturalPersonCheckStatus } from 'types/userChecks';

import { ITenantGroupDescriptionTextType } from 'view/libs/tenantGroup/types';

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

export const testCases: Array<{ data: Array<IFlatUser | IFlatTenant>; expect: ITenantGroupDescriptionTextType }> = [
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
        expect: ITenantGroupDescriptionTextType.SINGLE,
    },
    {
        data: [
            {
                ...commonUser,
                approvedTenantQuestionnaire: {
                    ...commonUser.approvedTenantQuestionnaire,
                    additionalTenant: 'Один',
                    hasChildren: true,
                    numberOfBabies: 2,
                },
            },
        ],
        expect: ITenantGroupDescriptionTextType.SINGLE_AND_CHILDREN,
    },
    {
        data: [
            {
                ...commonUser,
                approvedTenantQuestionnaire: {
                    ...commonUser.approvedTenantQuestionnaire,
                    additionalTenant: 'Один',
                    hasChildren: true,
                },
            },
            {
                ...commonUser,
                userId: 'e77a4c0bac42' as UserId,
                approvedTenantQuestionnaire: {
                    ...commonUser.approvedTenantQuestionnaire,
                    additionalTenant: 'Один',
                    hasChildren: true,
                    numberOfBabies: 1,
                },
            },
        ],
        expect: ITenantGroupDescriptionTextType.SINGLE_AND_CHILDREN,
    },
    {
        data: [
            {
                ...commonUser,
                approvedTenantQuestionnaire: {
                    ...commonUser.approvedTenantQuestionnaire,
                    additionalTenant: 'Один',
                    hasPets: true,
                },
            },
        ],
        expect: ITenantGroupDescriptionTextType.SINGLE_AND_PETS,
    },
    {
        data: [
            {
                ...commonUser,
                approvedTenantQuestionnaire: {
                    ...commonUser.approvedTenantQuestionnaire,
                    additionalTenant: 'Один',
                    hasPets: true,
                    hasChildren: true,
                    numberOfBabies: 2,
                },
            },
        ],
        expect: ITenantGroupDescriptionTextType.SINGLE_AND_CHILDREN_AND_PETS,
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
        expect: ITenantGroupDescriptionTextType.WITH_FAMILY,
    },
    {
        data: [
            {
                ...commonUser,
                approvedTenantQuestionnaire: {
                    ...commonUser.approvedTenantQuestionnaire,
                    additionalTenant: 'С семьей',
                    hasChildren: true,
                    numberOfBabies: 2,
                },
            },
        ],
        expect: ITenantGroupDescriptionTextType.WITH_FAMILY_AND_CHILDREN,
    },
    {
        data: [
            {
                ...commonUser,
                approvedTenantQuestionnaire: {
                    ...commonUser.approvedTenantQuestionnaire,
                    additionalTenant: 'С семьей',
                    hasPets: true,
                },
            },
        ],
        expect: ITenantGroupDescriptionTextType.WITH_FAMILY_AND_PETS,
    },
    {
        data: [
            {
                ...commonUser,
                approvedTenantQuestionnaire: {
                    ...commonUser.approvedTenantQuestionnaire,
                    additionalTenant: 'С семьей',
                    hasPets: true,
                    hasChildren: true,
                    numberOfBabies: 2,
                },
            },
        ],
        expect: ITenantGroupDescriptionTextType.WITH_FAMILY_AND_CHILDREN_AND_PETS,
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
        expect: ITenantGroupDescriptionTextType.WITH_WIFE,
    },
    {
        data: [
            {
                ...commonUser,
                approvedTenantQuestionnaire: {
                    ...commonUser.approvedTenantQuestionnaire,
                    additionalTenant: 'С супругом/супругой',
                    hasChildren: true,
                    numberOfBabies: 2,
                },
            },
        ],
        expect: ITenantGroupDescriptionTextType.WITH_WIFE_AND_CHILDREN,
    },
    {
        data: [
            {
                ...commonUser,
                approvedTenantQuestionnaire: {
                    ...commonUser.approvedTenantQuestionnaire,
                    additionalTenant: 'С супругом/супругой',
                    hasPets: true,
                },
            },
        ],
        expect: ITenantGroupDescriptionTextType.WITH_WIFE_AND_PETS,
    },
    {
        data: [
            {
                ...commonUser,
                approvedTenantQuestionnaire: {
                    ...commonUser.approvedTenantQuestionnaire,
                    additionalTenant: 'С супругом/супругой',
                    hasPets: true,
                    hasChildren: true,
                    numberOfBabies: 2,
                },
            },
        ],
        expect: ITenantGroupDescriptionTextType.WITH_WIFE_AND_CHILDREN_AND_PETS,
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
        expect: ITenantGroupDescriptionTextType.WITH_RELATIVES,
    },
    {
        data: [
            {
                ...commonUser,
                approvedTenantQuestionnaire: {
                    ...commonUser.approvedTenantQuestionnaire,
                    additionalTenant: 'С родственниками',
                    hasChildren: true,
                    numberOfBabies: 2,
                },
            },
        ],
        expect: ITenantGroupDescriptionTextType.WITH_RELATIVES_AND_CHILDREN,
    },
    {
        data: [
            {
                ...commonUser,
                approvedTenantQuestionnaire: {
                    ...commonUser.approvedTenantQuestionnaire,
                    additionalTenant: 'С родственниками',
                    hasPets: true,
                },
            },
        ],
        expect: ITenantGroupDescriptionTextType.WITH_RELATIVES_AND_PETS,
    },
    {
        data: [
            {
                ...commonUser,
                approvedTenantQuestionnaire: {
                    ...commonUser.approvedTenantQuestionnaire,
                    additionalTenant: 'С родственниками',
                    hasPets: true,
                    hasChildren: true,
                    numberOfBabies: 2,
                },
            },
        ],
        expect: ITenantGroupDescriptionTextType.WITH_RELATIVES_AND_CHILDREN_AND_PETS,
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
        expect: ITenantGroupDescriptionTextType.WITH_GIRLFRIEND,
    },
    {
        data: [
            {
                ...commonUser,
                approvedTenantQuestionnaire: {
                    ...commonUser.approvedTenantQuestionnaire,
                    additionalTenant: 'С парнем/девушкой',
                    hasChildren: true,
                    numberOfBabies: 2,
                },
            },
        ],
        expect: ITenantGroupDescriptionTextType.WITH_GIRLFRIEND_AND_CHILDREN,
    },
    {
        data: [
            {
                ...commonUser,
                approvedTenantQuestionnaire: {
                    ...commonUser.approvedTenantQuestionnaire,
                    additionalTenant: 'С парнем/девушкой',
                    hasPets: true,
                },
            },
        ],
        expect: ITenantGroupDescriptionTextType.WITH_GIRLFRIEND_AND_PETS,
    },
    {
        data: [
            {
                ...commonUser,
                approvedTenantQuestionnaire: {
                    ...commonUser.approvedTenantQuestionnaire,
                    additionalTenant: 'С парнем/девушкой',
                    hasPets: true,
                    hasChildren: true,
                    numberOfBabies: 2,
                },
            },
        ],
        expect: ITenantGroupDescriptionTextType.WITH_GIRLFRIEND_AND_CHILDREN_AND_PETS,
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
        expect: ITenantGroupDescriptionTextType.WITH_FRIEND,
    },
    {
        data: [
            {
                ...commonUser,
                approvedTenantQuestionnaire: {
                    ...commonUser.approvedTenantQuestionnaire,
                    additionalTenant: 'С другом/подругой',
                    hasChildren: true,
                    numberOfBabies: 2,
                },
            },
        ],
        expect: ITenantGroupDescriptionTextType.WITH_FRIEND_AND_CHILDREN,
    },
    {
        data: [
            {
                ...commonUser,
                approvedTenantQuestionnaire: {
                    ...commonUser.approvedTenantQuestionnaire,
                    additionalTenant: 'С другом/подругой',
                    hasPets: true,
                },
            },
        ],
        expect: ITenantGroupDescriptionTextType.WITH_FRIEND_AND_PETS,
    },
    {
        data: [
            {
                ...commonUser,
                approvedTenantQuestionnaire: {
                    ...commonUser.approvedTenantQuestionnaire,
                    additionalTenant: 'С другом/подругой',
                    hasPets: true,
                    hasChildren: true,
                    numberOfBabies: 2,
                },
            },
        ],
        expect: ITenantGroupDescriptionTextType.WITH_FRIEND_AND_CHILDREN_AND_PETS,
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
        expect: ITenantGroupDescriptionTextType.WITH_FRIENDS,
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
        expect: ITenantGroupDescriptionTextType.WITH_FRIENDS_AND_CHILDREN,
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
        expect: ITenantGroupDescriptionTextType.WITH_FRIENDS_AND_PETS,
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
        expect: ITenantGroupDescriptionTextType.WITH_FRIENDS_AND_CHILDREN_AND_PETS,
    },
];

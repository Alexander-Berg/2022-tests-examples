import { TenantQuestionnaireModerationStatus, UserId, UserPersonalActivity } from 'types/user';
import { FlatUserRole, IFlatTenant, IFlatUser } from 'types/flat';
import { NaturalPersonCheckResolution, NaturalPersonCheckStatus } from 'types/userChecks';

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

export const testCases: Array<{ data: IFlatUser | IFlatTenant; expect: string }> = [
    {
        data: {
            ...commonUser,
            tenantCandidateAge: 18,
        },
        expect: 'Вася, 18\u00a0лет',
    },
    {
        data: {
            ...commonUser,
            tenantCandidateAge: 21,
        },
        expect: 'Вася, 21\u00a0год',
    },
    {
        data: {
            ...commonUser,
            tenantCandidateAge: 22,
        },
        expect: 'Вася, 22\u00a0года',
    },
    {
        data: {
            ...commonUser,
            tenantCandidateAge: 25,
        },
        expect: 'Вася, 25\u00a0лет',
    },
];

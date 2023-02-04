import { TenantQuestionnaireModerationStatus, UserId } from 'types/user';
import { IFlatUser, FlatUserRole } from 'types/flat';
import { NaturalPersonCheckResolution, NaturalPersonCheckStatus } from 'types/userChecks';

const assignedUserBase: IFlatUser = {
    phone: '+79000000000',
    person: {
        name: 'Иван',
        surname: 'Иванов',
        patronymic: 'Иванович',
    },
    userRole: FlatUserRole.TENANT_CANDIDATE,
    email: 'testuser@yandex.ru',
    userId: '1' as UserId,
};

export const assignedUsersChecksUndefined: IFlatUser[] = [assignedUserBase];

export const assignedUsersChecksInProgress: IFlatUser[] = [
    {
        ...assignedUserBase,
        naturalPersonCheckStatus: NaturalPersonCheckStatus.IN_PROGRESS,
    },
];

export const assignedUsersChecksInProgressWithResolution: IFlatUser[] = [
    {
        ...assignedUserBase,
        naturalPersonCheckStatus: NaturalPersonCheckStatus.IN_PROGRESS,
        naturalPersonCheckResolution: NaturalPersonCheckResolution.VALID,
    },
];

export const assignedUsersChecksError: IFlatUser[] = [
    {
        ...assignedUserBase,
        naturalPersonCheckStatus: NaturalPersonCheckStatus.ERROR,
    },
];

export const assignedUsersChecksReadyResolutionInvalid: IFlatUser[] = [
    {
        ...assignedUserBase,
        naturalPersonCheckStatus: NaturalPersonCheckStatus.READY,
        naturalPersonCheckResolution: NaturalPersonCheckResolution.INVALID,
    },
];

export const assignedUsersChecksReadyResolutionValid: IFlatUser[] = [
    {
        ...assignedUserBase,
        naturalPersonCheckStatus: NaturalPersonCheckStatus.READY,
        naturalPersonCheckResolution: NaturalPersonCheckResolution.VALID,
    },
];

export const assignedUsersChecksReadyWithDebts: IFlatUser[] = [
    {
        ...assignedUserBase,
        naturalPersonCheckStatus: NaturalPersonCheckStatus.READY,
        naturalPersonCheckResolution: NaturalPersonCheckResolution.VALID,
        proceedingExecutiveResolution: NaturalPersonCheckResolution.INVALID,
    },
];

export const assignedUsersChecksInvalidWithDebts: IFlatUser[] = [
    {
        ...assignedUserBase,
        naturalPersonCheckStatus: NaturalPersonCheckStatus.READY,
        naturalPersonCheckResolution: NaturalPersonCheckResolution.INVALID,
        proceedingExecutiveResolution: NaturalPersonCheckResolution.INVALID,
    },
];

export const assignedUsersQuestionnaireInProgress: IFlatUser[] = [
    {
        ...assignedUserBase,
        tenantQuestionnaireModerationStatus: TenantQuestionnaireModerationStatus.IN_PROGRESS,
    },
];

export const assignedUsersQuestionnaireValid: IFlatUser[] = [
    {
        ...assignedUserBase,
        tenantQuestionnaireModerationStatus: TenantQuestionnaireModerationStatus.VALID,
    },
];

export const assignedUsersQuestionnaireInvalid: IFlatUser[] = [
    {
        ...assignedUserBase,
        tenantQuestionnaireModerationStatus: TenantQuestionnaireModerationStatus.INVALID,
    },
];

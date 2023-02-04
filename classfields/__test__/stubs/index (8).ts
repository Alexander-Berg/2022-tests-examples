import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { CandidateId, IRoommateCandidate, RoommateLinkId, RoommateLinkStatus } from 'types/roommates';
import { TenantQuestionnaireModerationStatus, UserId } from 'types/user';

import { IUniversalStore } from 'view/modules/types';

export const getRoommate = (params: {
    id: string;
    tenantQuestionnaireModerationStatus?: TenantQuestionnaireModerationStatus;
}): IRoommateCandidate => {
    const { id, tenantQuestionnaireModerationStatus } = params;
    return {
        candidateId: id as CandidateId,
        userId: '232322323323' as UserId,
        phone: '89992042323',
        person: tenantQuestionnaireModerationStatus
            ? {
                  name: 'Имя',
                  surname: 'Фамилия',
                  patronymic: 'Отчество',
              }
            : undefined,
        tenantQuestionnaireModerationStatus:
            tenantQuestionnaireModerationStatus ?? TenantQuestionnaireModerationStatus.UNKNOWN,
        status: RoommateLinkStatus.CREATED,
    };
};

export const baseStore: DeepPartial<IUniversalStore> = {
    page: {
        params: {},
    },
    config: {
        origin: 'https://realty-frontend.arenda.local.dev.vertis.yandex.ru/lk/assign-roommates/232423432423',
    },
    roommates: {
        fields: {
            linkId: '14233rf434fr3424' as RoommateLinkId,
            users: [],
        },
        network: {
            updateRoommatesStatus: RequestStatus.LOADED,
        },
    },
};

export const skeletonStore: DeepPartial<IUniversalStore> = {
    spa: {
        status: RequestStatus.PENDING,
    },
};

export const withBackButtonStore: DeepPartial<IUniversalStore> = {
    ...baseStore,
    page: {
        params: {
            previousPage: 'tenant-questionnaire-form',
        },
    },
};

export const withOneRommateStore: DeepPartial<IUniversalStore> = {
    ...baseStore,
    roommates: {
        fields: {
            linkId: '14233rf434fr3424' as RoommateLinkId,
            users: [getRoommate({ id: '134234' })],
        },
        network: {
            updateRoommatesStatus: RequestStatus.LOADED,
        },
    },
};

export const withRoommatesStore: DeepPartial<IUniversalStore> = {
    ...baseStore,
    roommates: {
        fields: {
            linkId: '14233rf434fr3424' as RoommateLinkId,
            users: [
                getRoommate({ id: 'd0000' }),
                getRoommate({
                    id: 'd0001',
                    tenantQuestionnaireModerationStatus: TenantQuestionnaireModerationStatus.IN_PROGRESS,
                }),
                getRoommate({
                    id: 'd0001',
                    tenantQuestionnaireModerationStatus: TenantQuestionnaireModerationStatus.INVALID,
                }),
                getRoommate({
                    id: 'd0001',
                    tenantQuestionnaireModerationStatus: TenantQuestionnaireModerationStatus.VALID,
                }),
            ],
        },
        network: {
            updateRoommatesStatus: RequestStatus.LOADED,
        },
    },
};

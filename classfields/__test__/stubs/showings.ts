import { DeepPartial } from 'utility-types';

import { Flavor } from 'realty-core/types/utils';

import { FlatUserRole, FlatStatus, ShowingId } from 'types/flat';

import { CloseShowingCause, Showing, FlatShowingStatus, FlatShowingType } from 'types/showing';
import { RoommateGroupStatus } from 'types/roommates';

import { TenantQuestionnaireModerationStatus, UserId } from 'types/user';

import { IUniversalStore } from 'view/modules/types';

const showing: Showing = {
    showingId: String('1') as ShowingId,
    status: FlatShowingStatus.SHOWING_APPOINTED,
    closeShowingCause: CloseShowingCause.UNKNOWN,
    rentAmount: '300',
    roommates: [],
    retouchedPhotos: [],
    tenantCheckInDate: '',
    realtyOfferId: '1',
    flatId: '1',
    showingType: FlatShowingType.OFFLINE,
    amoLeadLink: 'https://yandexarenda.amocrm.ru/leads/detail/21458605',
    tenantStructureInfo: {
        numberOfAdults: 1,
        numberOfChildren: 0,
        withPets: true,
        descriptionOfPets: 'Животные',
    },
    groupUsers: {
        showingId: '1' as ShowingId,
        status: RoommateGroupStatus.UNKNOWN,
        mainUser: {
            userId: '1' as UserId,
            tenantQuestionnaireModerationStatus: TenantQuestionnaireModerationStatus.UNKNOWN,
            person: {
                name: 'Петр',
                surname: 'Иванов',
                patronymic: 'Иванович',
            },
            phone: '+79043333335',
        },
        users: [
            {
                userId: '1' as UserId,
                tenantQuestionnaireModerationStatus: TenantQuestionnaireModerationStatus.UNKNOWN,

                person: {
                    name: 'Олег',
                    surname: 'Иванов',
                    patronymic: 'Иванович',
                },
                phone: '+79043333335',
            },
            {
                userId: '1' as UserId,
                tenantQuestionnaireModerationStatus: TenantQuestionnaireModerationStatus.UNKNOWN,

                person: {
                    name: 'Степан',
                    surname: 'Иванов',
                    patronymic: 'Иванович',
                },
                phone: '+79043333335',
            },
            {
                userId: '1' as UserId,
                tenantQuestionnaireModerationStatus: TenantQuestionnaireModerationStatus.UNKNOWN,

                person: {
                    name: 'Павел',
                    surname: 'Иванов',
                    patronymic: 'Иванович',
                },
                phone: '+79043333335',
            },
        ],
    },
};

export const storeNoShowings: DeepPartial<IUniversalStore> = {
    managerFlat: {
        flat: {
            address: {
                address: 'г Москва, ул Минская, д 6, кв 1',
                flatNumber: '10',
            },
            flatId: 'flat10' as Flavor<string, 'FlatID'>,
            assignedUsers: [],
            userRole: FlatUserRole.OWNER,
            desiredRentAmount: '3000000',
            status: FlatStatus.CONFIRMED,
            code: '12-DDFF',
        },
        actualContract: {},
    },
};

export const allShowingTypes = Object.keys(FlatShowingStatus).map(
    (status, idx): Showing => {
        return { ...showing, showingId: String(idx) as ShowingId, status: status as FlatShowingStatus };
    }
);

export const multicontactShowing = [{ ...showing }];

export const longDescriptionShowing = [
    {
        ...showing,
        tenantStructureInfo: {
            numberOfAdults: 1,
            numberOfChildren: 1,
            withPets: true,
            descriptionOfPets:
                'Много букв Много букв Много букв Много букв Много букв Много букв Много букв Много букв Много букв ' +
                'Много букв Много букв Много букв Много букв Много букв ' +
                'Много букв Много букв Много букв Много букв Много букв Много букв Много букв Много букв Много букв ' +
                'Много букв Много букв Много букв Много букв Много букв Много букв Много букв ' +
                'Много букв Много букв Много букв Много букв Много букв Много букв Много букв Много букв Много букв',
        },
    },
];

export const onlineShowing = [{ ...showing, showingType: FlatShowingType.ONLINE }];

export const getStore = (flatShowings: Showing[]) => ({
    managerFlat: {
        flat: {
            address: {
                address: 'г Москва, ул Минская, д 6, кв 1',
                flatNumber: '10',
            },
            flatId: 'flat10' as Flavor<string, 'FlatID'>,
            assignedUsers: [],
            isCreatedByModeration: false,
            userRole: FlatUserRole.OWNER,
            desiredRentAmount: '3000000',
            status: FlatStatus.CONFIRMED,
            code: '12-DDFF',
        },
        actualContract: {},
    },
    managerFlatShowings: {
        showings: flatShowings,
    },
    page: {
        params: {
            flatId: 'flat10' as Flavor<string, 'FlatID'>,
        },
    },
});

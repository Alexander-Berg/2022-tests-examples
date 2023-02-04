import { FlatId, FlatStatus, FlatUserRole, IFlat, IFlatAddress } from 'types/flat';
import { UserId } from 'types/user';

export const flat: IFlat = {
    flatId: '1' as FlatId,
    address: ('' as unknown) as IFlatAddress,
    status: FlatStatus.RENTED,
    actualPayment: {
        ownerSpecificPaymentInfo: {
            amount: '2595200',
            ownerPayoutDetails: {
                cardPayment: {
                    panMask: '*** 2597',
                },
            },
        },
    },
    assignedUsers: [
        {
            phone: '+79112473328',
            person: {
                name: 'Гомер',
                surname: 'Симпсон',
            },
            userRole: FlatUserRole.TENANT_CANDIDATE,
            email: 'zooeyOwner1@yandex.ru',
            userId: '402682616' as UserId,
            approvedTenantQuestionnaire: {
                additionalTenant: 'Один',
                reasonForRelocation: 'Выгнали из квартиры',
                selfDescription: ' древнегреческий поэт-сказитель, создатель эпических поэм «Илиада» и «Одиссея»',
                hasChildren: true,
                hasPets: true,
                petsInfo: '',
            },
        },
        {
            phone: '+79112473328',
            person: {
                name: 'Гомер',
                surname: 'Симпсон',
            },
            userRole: FlatUserRole.TENANT_CANDIDATE,
            email: 'zooeyOwner1@yandex.ru',
            userId: '402826176' as UserId,
            approvedTenantQuestionnaire: {
                additionalTenant: 'Один',
                reasonForRelocation: 'Выгнали из квартиры',
                selfDescription: ' древнегреческий поэт-сказитель, создатель эпических поэм «Илиада» и «Одиссея»',
                hasChildren: true,
                hasPets: true,
                petsInfo: '',
            },
        },
    ],
    userRole: FlatUserRole.TENANT,
    code: '1',
};

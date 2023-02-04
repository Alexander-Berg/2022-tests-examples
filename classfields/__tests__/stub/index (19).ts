import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { IUniversalStore } from 'view/modules/types';
import { tinkoffCard } from 'view/components/UserPaymentMethodsCardBase/__tests__/stub';

export const store: DeepPartial<IUniversalStore> = {
    payments: {
        data: {
            cards: [tinkoffCard],
        },
        network: {
            deletePaymentCard: RequestStatus.LOADED,
        },
    },
};

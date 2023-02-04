import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { IUniversalStore } from 'view/modules/types';

export const store: DeepPartial<IUniversalStore> = {
    payments: {
        network: {
            initBindCard: RequestStatus.LOADED,
        },
    },
};

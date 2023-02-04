import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { FlatId, FlatStatus } from 'types/flat';
import { IUniversalStore } from 'view/modules/types';

export const commonStore: DeepPartial<IUniversalStore> = {
    outstaffFlat: {
        flat: {
            flatId: '000000' as FlatId,
            address: {
                address: 'г Санкт‑Петербург, Старо‑Петергофский пр‑кт, д 19',
                flatNumber: '7',
            },
            status: FlatStatus.LOOKING_FOR_TENANT,
            code: '19-COVID',
        },
    },
    spa: {
        status: RequestStatus.LOADED,
    },
    imageUploader: {
        '000000': {
            images: [],
        },
    },
};

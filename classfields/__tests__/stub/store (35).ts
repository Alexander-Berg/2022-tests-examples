import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { IUniversalStore } from 'view/modules/types';
import { FlatUserRole } from 'types/flat';
import { AssignStatus } from 'types/assignment';

export const getStore = (): DeepPartial<IUniversalStore> => {
    return {
        assignmentFlat: {
            flat: {
                address: {
                    address: 'г Санкт-Петербург, Кушелевская дорога, д 8',
                    flatNumber: '288',
                },
            },
            status: AssignStatus.SUCCESS,
        },
        page: {
            params: {
                role: FlatUserRole.OWNER,
            },
        },
        spa: {
            status: RequestStatus.LOADED,
        },
    };
};

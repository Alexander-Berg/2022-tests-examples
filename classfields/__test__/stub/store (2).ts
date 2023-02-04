import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { FlatId } from 'types/flat';
import { CianVasType } from 'types/publishing';

import { IUniversalStore } from 'view/modules/types';
import { CianFields } from 'view/modules/managerFlatVasForm/types';
import { initialState as fieldsInitialState } from 'view/modules/managerFlatVasForm/reducers/cianFields';
import { initialState as networkInitialState } from 'view/modules/managerFlatVasForm/reducers/network';

export const baseStore: DeepPartial<IUniversalStore> = {
    page: {
        params: {
            flatId: '000000' as FlatId,
        },
    },
    spa: {
        status: RequestStatus.LOADED,
    },
    managerFlatVasForm: {
        network: networkInitialState,
        cianFields: fieldsInitialState,
    },
    config: {
        isMobile: '',
    },
};

export const filledStore: DeepPartial<IUniversalStore> = {
    ...baseStore,
    managerFlatVasForm: {
        network: networkInitialState,
        cianFields: {
            [CianFields.VAS_TYPE]: {
                id: CianFields.VAS_TYPE,
                value: CianVasType.PAID,
            },
            [CianFields.VAS_HIGHLIGHT]: {
                id: CianFields.VAS_HIGHLIGHT,
                value: true,
            },
            [CianFields.AUCTION_BET]: {
                id: CianFields.AUCTION_BET,
                value: 26.5,
            },
        },
    },
};

export const mobileStore: DeepPartial<IUniversalStore> = {
    ...baseStore,
    config: { isMobile: 'iOS' },
};

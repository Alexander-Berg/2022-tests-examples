import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { IUniversalStore } from 'view/modules/types';
import { initialState as fieldsInitialState } from 'view/modules/feedbackForm/reducers/fields';
import { initialState as networkInitialState } from 'view/modules/feedbackForm/reducers/network';

export const store: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    feedbackForm: {
        fields: fieldsInitialState,
        network: networkInitialState,
    },
    config: { isMobile: '' },
};

export const skeletonStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.PENDING,
    },
    feedbackForm: {
        fields: fieldsInitialState,
        network: networkInitialState,
    },
    config: { isMobile: '' },
};

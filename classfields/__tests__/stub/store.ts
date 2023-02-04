import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { IUniversalStore } from 'view/modules/types';
import { initialState as fieldsInitialState } from 'view/modules/houseServicesSettingsForm/reducers/fields';
import { initialState as networkInitialState } from 'view/modules/houseServicesSettingsForm/reducers/network';

export const store: DeepPartial<IUniversalStore> = {
    page: {
        params: {
            flatId: '12345',
        },
    },
    spa: {
        status: RequestStatus.LOADED,
    },
    houseServicesSettingsForm: {
        fields: fieldsInitialState,
        network: networkInitialState,
    },
    config: { isMobile: '' },
};

export const mobileStore: DeepPartial<IUniversalStore> = {
    config: { isMobile: 'iOS' },
    spa: {
        status: RequestStatus.LOADED,
    },
    houseServicesSettingsForm: {
        fields: fieldsInitialState,
        network: networkInitialState,
    },
};

export const skeletonStore: DeepPartial<IUniversalStore> = {
    page: {
        params: {
            flatId: '12345',
        },
    },
    spa: {
        status: RequestStatus.PENDING,
    },
    houseServicesSettingsForm: {
        fields: fieldsInitialState,
        network: networkInitialState,
    },
};

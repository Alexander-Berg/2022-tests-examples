import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { IUniversalStore } from 'view/modules/types';
import { initialState as fieldsInitialState } from 'view/modules/questionnaireForm/reducers/fields';
import { initialState as networkInitialState } from 'view/modules/questionnaireForm/reducers/network';

export const store: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    questionnaireForm: {
        fields: fieldsInitialState,
        network: networkInitialState,
    },
    config: { isMobile: '' },
};

export const storeWrongAdditionalTenant: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    questionnaireForm: {
        fields: fieldsInitialState,
        network: networkInitialState,
    },
    user: {
        tenantQuestionnaire: {
            additionalTenant: 'Этого нет в селекте',
        },
    },
    config: { isMobile: '' },
};

export const mobileStore: DeepPartial<IUniversalStore> = {
    config: { isMobile: 'iOS' },
    spa: {
        status: RequestStatus.LOADED,
    },
    questionnaireForm: {
        fields: fieldsInitialState,
        network: networkInitialState,
    },
};

export const skeletonStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.PENDING,
    },
    questionnaireForm: {
        fields: fieldsInitialState,
        network: networkInitialState,
    },
    config: { isMobile: '' },
};

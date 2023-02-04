import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { Flavor } from 'realty-core/types/utils';

import { Fields } from 'view/modules/ownerFlatQuestionnaireForm/types';

import { IUniversalStore } from 'view/modules/types';
// eslint-disable-next-line max-len
import { initialState as flatQuestionnaireFieldsInitialState } from 'view/modules/ownerFlatQuestionnaireForm/reducers/fields';
// eslint-disable-next-line max-len
import { initialState as flatQuestionnaireNetworkInitialState } from 'view/modules/ownerFlatQuestionnaireForm/reducers/network';

export const store: DeepPartial<IUniversalStore> = {
    page: {
        params: {
            flatId: 'ffc21a950bf247818405d91537154696' as Flavor<string, 'FlatID'>,
        },
    },
    spa: {
        status: RequestStatus.LOADED,
    },
    managerFlatQuestionnaire: {},
    ownerFlatQuestionnaireForm: {
        fields: flatQuestionnaireFieldsInitialState,
        network: flatQuestionnaireNetworkInitialState,
    },
    config: { isMobile: '' },
};

export const mobileStore: DeepPartial<IUniversalStore> = {
    config: { isMobile: 'iOS' },
    spa: {
        status: RequestStatus.LOADED,
    },
    ownerFlatQuestionnaireForm: {
        fields: flatQuestionnaireFieldsInitialState,
        network: flatQuestionnaireNetworkInitialState,
    },
};

export const skeletonStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.PENDING,
    },
    ownerFlatQuestionnaireForm: {
        fields: flatQuestionnaireFieldsInitialState,
        network: flatQuestionnaireNetworkInitialState,
    },
};

export const filledStore: DeepPartial<IUniversalStore> = {
    page: {
        params: {
            flatId: 'ffc21a950bf247818405d91537154696' as Flavor<string, 'FlatID'>,
        },
    },
    config: { isMobile: '' },
    spa: {
        status: RequestStatus.LOADED,
    },
    ownerFlatQuestionnaire: {
        questionnaire: {},
    },
    ownerFlatQuestionnaireForm: {
        fields: {
            [Fields.FLAT_ENTRANCE]: {
                id: Fields.FLAT_ENTRANCE,
                value: 4,
            },
            [Fields.FLAT_FLOOR]: {
                id: Fields.FLAT_FLOOR,
                value: 18,
            },
            [Fields.FLAT_INTERCOM_CODE]: {
                id: Fields.FLAT_INTERCOM_CODE,
                value: '300',
            },
            [Fields.FLAT_TYPE]: {
                id: Fields.FLAT_TYPE,
                value: 'FLAT',
            },
            [Fields.FLAT_ROOMS]: {
                id: Fields.FLAT_ROOMS,
                value: 'SIX',
            },
            [Fields.FLAT_AREA]: {
                id: Fields.FLAT_AREA,
                value: 184,
            },
            [Fields.DESIRED_RENT_PRICE]: {
                id: Fields.DESIRED_RENT_PRICE,
                value: 50000000,
            },
        },
        network: {
            updateOwnerFlatQuestionnaireStatus: RequestStatus.LOADED,
        },
    },
};

export const onlyContentFilledStore: DeepPartial<IUniversalStore> = {
    page: {
        params: {
            flatId: 'ffc21a950bf247818405d91537154696' as Flavor<string, 'FlatID'>,
        },
    },
    config: { isMobile: '' },
    spa: {
        status: RequestStatus.LOADED,
    },
    ownerFlatQuestionnaire: {
        questionnaire: {},
    },
    ownerFlatQuestionnaireForm: {
        fields: {
            [Fields.FLAT_ENTRANCE]: {
                id: Fields.FLAT_ENTRANCE,
                value: 4,
            },
            [Fields.FLAT_FLOOR]: {
                id: Fields.FLAT_FLOOR,
                value: 18,
            },
            [Fields.FLAT_INTERCOM_CODE]: {
                id: Fields.FLAT_INTERCOM_CODE,
                value: '300',
            },
            [Fields.FLAT_TYPE]: {
                id: Fields.FLAT_TYPE,
                value: 'FLAT',
            },
            [Fields.FLAT_ROOMS]: {
                id: Fields.FLAT_ROOMS,
                value: 'SIX',
            },
            [Fields.FLAT_AREA]: {
                id: Fields.FLAT_AREA,
                value: 184,
            },
            [Fields.DESIRED_RENT_PRICE]: {
                id: Fields.DESIRED_RENT_PRICE,
                value: 50000000,
            },
        },
        network: {
            updateOwnerFlatQuestionnaireStatus: RequestStatus.LOADED,
        },
    },
    cookies: {
        ['only-content']: '1',
    },
};

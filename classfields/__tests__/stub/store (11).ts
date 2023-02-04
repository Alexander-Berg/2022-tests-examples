import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { IUniversalStore } from 'view/modules/types';
import { initialState as fieldsInitialState } from 'view/modules/ownerFlatDraft/reducers/fields';
import { initialState as networkInitialState } from 'view/modules/ownerFlatDraft/reducers/network';
import { OwnerFlatDraftFieldType } from 'view/modules/ownerFlatDraft/types';

export const store: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    ownerFlat: {},
    ownerFlatDraft: {
        fields: fieldsInitialState,
        network: networkInitialState,
    },
    spa: {
        status: RequestStatus.LOADED,
    },
    config: { isMobile: '' },
};

export const storeWithAddress: DeepPartial<IUniversalStore> = {
    ...store,
    ownerFlatDraft: {
        fields: {
            ...fieldsInitialState,
            ADDRESS: {
                id: OwnerFlatDraftFieldType.ADDRESS,
                unified: 'Россия, Санкт-Петербург, Кушелевская дорога, 8',
                value: {
                    address: 'Россия, Санкт-Петербург, Кушелевская дорога, 8',
                    house: '8',
                },
            },
        },
        network: networkInitialState,
    },
};

export const mobileStore: DeepPartial<IUniversalStore> = {
    config: { isMobile: 'iOS' },
    ownerFlat: {},
    ownerFlatDraft: {
        fields: fieldsInitialState,
        network: networkInitialState,
    },
    spa: {
        status: RequestStatus.LOADED,
    },
};

export const confirmationStore: DeepPartial<IUniversalStore> = {
    config: { isMobile: '' },
    page: { params: { flatId: '123' } },
    ownerFlat: {},
    ownerFlatDraft: {
        fields: {
            ...fieldsInitialState,
            PHONE: { ...fieldsInitialState['PHONE'], value: '+79500000000' },
            CONFIRMATION_CODE: {
                ...fieldsInitialState['CONFIRMATION_CODE'],
                codeLength: 5,
                timestamp: new Date('2020-06-01T03:00:00.111Z').getTime(),
                requestId: '123',
            },
        },
        network: networkInitialState,
    },
    spa: {
        status: RequestStatus.LOADED,
    },
};

export const confirmationStoreWithoutSms: DeepPartial<IUniversalStore> = {
    config: { isMobile: '' },
    page: { params: { flatId: '123' } },
    ownerFlat: {},
    ownerFlatDraft: {
        fields: {
            ...fieldsInitialState,
            PHONE: { ...fieldsInitialState['PHONE'], value: '+79500000000' },
        },
        network: networkInitialState,
    },
    spa: {
        status: RequestStatus.LOADED,
    },
};

export const skeletonStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    ownerFlat: {},
    ownerFlatDraft: {
        fields: fieldsInitialState,
        network: networkInitialState,
    },
    spa: {
        status: RequestStatus.PENDING,
    },
    config: { isMobile: '' },
};

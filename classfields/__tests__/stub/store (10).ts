import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { IUniversalStore } from 'view/modules/types';

import { initialState as phoneFieldsInitialState } from 'view/modules/userPersonalDataPhone/reducers/fields';
import { initialState as phoneNetworkInitialState } from 'view/modules/userPersonalDataPhone/reducers/network';

export const store: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    userPersonalDataPhone: {
        fields: phoneFieldsInitialState,
        network: phoneNetworkInitialState,
    },
};

export const confirmationStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    userPersonalDataPhone: {
        fields: {
            ...phoneFieldsInitialState,
            PHONE: { ...phoneFieldsInitialState['PHONE'], value: '+79500000000' },
            CONFIRMATION_CODE: {
                ...phoneFieldsInitialState['CONFIRMATION_CODE'],
                codeLength: 5,
                timestamp: new Date('2020-06-01T03:00:00.111Z').getTime(),
                requestId: '123',
            },
        },
        network: phoneNetworkInitialState,
    },
};

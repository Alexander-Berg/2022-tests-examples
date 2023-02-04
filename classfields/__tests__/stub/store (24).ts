import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { getFields } from 'app/libs/payment-data-form';

import { IUniversalStore } from 'view/modules/types';
import { initialState as paymentDataFieldsInitialState } from 'view/modules/paymentDataForm/reducers/fields';
import { initialState as paymentDataNetworkInitialState } from 'view/modules/paymentDataForm/reducers/network';

export const store: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    paymentDataForm: {
        fields: paymentDataFieldsInitialState,
        network: paymentDataNetworkInitialState,
    },
    config: { isMobile: '' },
};

export const mobileStore: DeepPartial<IUniversalStore> = {
    config: { isMobile: 'iOS' },
    spa: {
        status: RequestStatus.LOADED,
    },
    paymentDataForm: {
        fields: paymentDataFieldsInitialState,
        network: paymentDataNetworkInitialState,
    },
};

export const skeletonStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.PENDING,
    },
    paymentDataForm: {
        fields: paymentDataFieldsInitialState,
        network: paymentDataNetworkInitialState,
    },
    config: { isMobile: '' },
};

const user = {
    name: 'Иван',
    surname: 'Иванов',
    patronymic: 'Иванович',
};

const paymentData = {
    person: {
        name: 'Иван',
        surname: 'Иванов',
        patronymic: 'Иванович',
    },
    inn: '380114872773',
    bik: '041231112',
    accountNumber: '12334124321312312122',
};

export const filledStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    paymentDataForm: {
        fields: getFields(paymentData, user),
        network: paymentDataNetworkInitialState,
    },
    config: { isMobile: '' },
};

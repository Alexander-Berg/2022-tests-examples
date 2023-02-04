import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { getFields } from 'app/libs/personal-data-form';

import { IUniversalStore } from 'view/modules/types';
import { initialState as personalDataFieldsInitialState } from 'view/modules/personalDataForm/reducers/fields';
import { initialState as personalDataNetworkInitialState } from 'view/modules/personalDataForm/reducers/network';
import { initialState as phoneFieldsInitialState } from 'view/modules/userPersonalDataPhone/reducers/fields';
import { initialState as phoneNetworkInitialState } from 'view/modules/userPersonalDataPhone/reducers/network';
import { UserPersonalDataPhoneFieldType } from 'view/modules/userPersonalDataPhone/types';

export const skeletonStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.PENDING,
    },
    personalDataForm: {
        fields: personalDataFieldsInitialState,
        network: personalDataNetworkInitialState,
    },
    userPersonalDataPhone: {
        fields: phoneFieldsInitialState,
        network: phoneNetworkInitialState,
    },
};

const person = {
    name: 'Иван',
    surname: 'Иванов',
    patronymic: 'Иванович',
};

const passportData = {
    birthday: '2017-04-07T21:00:00Z',
    passportSeries: '2222',
    passportNumber: '333330',
    passportIssueDate: '2008-05-03T21:00:00Z',
    passportIssuedBy: 'МВД по Иркутской области',
    departmentCode: '380840',
    birthPlace: 'Иркутская область, город Ангарск\n',
    registrationAddress: 'Санкт-Петербург, пр-кт Стачек, д 58, кв 332',
};

export const filledStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    userPersonalDataPhone: {
        fields: {
            [UserPersonalDataPhoneFieldType.PHONE]: {
                id: UserPersonalDataPhoneFieldType.PHONE,
                value: '+79876543210',
            },
        },
    },
    legacyUser: {
        email: 'gomer@ya.ru',
        phone: '+79876543210',
        person,
        passportData,
    },
};

export const filledNameAndPhoneStore: DeepPartial<IUniversalStore> = {
    ...filledStore,
    legacyUser: {
        person,
        email: 'gomer@ya.ru',
    },
    personalDataForm: {
        fields: getFields({ person }),
        network: personalDataNetworkInitialState,
    },
};

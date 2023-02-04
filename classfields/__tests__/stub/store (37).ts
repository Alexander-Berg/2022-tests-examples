import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { IUniversalStore } from 'view/modules/types';
import { initialState as personalDataFieldsInitialState } from 'view/modules/personalDataForm/reducers/fields';
import { initialState as personalDataNetworkInitialState } from 'view/modules/personalDataForm/reducers/network';
import { initialState as phoneFieldsInitialState } from 'view/modules/userPersonalDataPhone/reducers/fields';
import { initialState as phoneNetworkInitialState } from 'view/modules/userPersonalDataPhone/reducers/network';

const user = {
    phone: '+79992134916',
    email: '123@list.ru',
    person: {
        name: 'Иван',
        surname: 'Иванов',
        patronymic: 'Иванович',
    },
    passportData: {
        documents: [],
        birthday: '2017-04-07T21:00:00Z',
        passportSeries: '2222',
        passportNumber: '333330',
        passportIssueDate: '2008-05-03T21:00:00Z',
        passportIssuedBy: 'МВД по Иркутской области',
        departmentCode: '380840',
        birthPlace: 'Иркутская область, город Ангарск\n',
        registrationAddress: 'Санкт-Петербург, пр-кт Стачек, д 58, кв 332',
    },
};

const imageUploader = {
    PASSPORT_MAIN_PAGE: {
        images: [],
        getImageUploaderUrlStatus: RequestStatus.LOADED,
    },
    REGISTRATION_PAGE: {
        images: [],
        getImageUploaderUrlStatus: RequestStatus.LOADED,
    },
    SELFIE_WITH_PASSPORT: {
        images: [],
        getImageUploaderUrlStatus: RequestStatus.LOADED,
    },
};

export const store: DeepPartial<IUniversalStore> = {
    spa: {
        status: RequestStatus.LOADED,
    },
    page: { params: { flatId: '123' } },
    breadcrumbs: {
        crumbs: [
            {
                route: 'user-flat',
            },
        ],
        current: {
            route: 'personal-data',
        },
    },
    personalDataForm: {
        fields: personalDataFieldsInitialState,
        network: personalDataNetworkInitialState,
    },
    userPersonalDataPhone: {
        fields: phoneFieldsInitialState,
        network: phoneNetworkInitialState,
    },
    legacyUser: user,
    config: { isMobile: '' },
    imageUploader,
};

export const mobileStore: DeepPartial<IUniversalStore> = {
    ...store,
    config: { isMobile: 'iOS' },
};

export const onlyContentStore: DeepPartial<IUniversalStore> = {
    spa: {
        status: RequestStatus.LOADED,
    },
    page: { params: { flatId: '123' } },
    breadcrumbs: {
        crumbs: [
            {
                route: 'user-flat',
            },
        ],
        current: {
            route: 'personal-data',
        },
    },
    personalDataForm: {
        fields: personalDataFieldsInitialState,
        network: personalDataNetworkInitialState,
    },
    userPersonalDataPhone: {
        fields: phoneFieldsInitialState,
        network: phoneNetworkInitialState,
    },
    legacyUser: user,
    cookies: {
        ['only-content']: '1',
    },
    config: { isMobile: 'iOS' },
    imageUploader,
};

export const skeletonStore: DeepPartial<IUniversalStore> = {
    spa: {
        status: RequestStatus.PENDING,
    },
    page: { params: { flatId: '123' } },
    breadcrumbs: {
        crumbs: [
            {
                route: 'user-flat',
            },
        ],
        current: {
            route: 'personal-data',
        },
    },
    personalDataForm: {
        fields: personalDataFieldsInitialState,
        network: personalDataNetworkInitialState,
    },
    userPersonalDataPhone: {
        fields: phoneFieldsInitialState,
        network: phoneNetworkInitialState,
    },
    legacyUser: user,
    config: { isMobile: '' },
};

import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { DocumentImageEntryType, DocumentImageId } from 'types/documents';
import { ImageUploaderImageStatus, ImageUploaderImageId } from 'types/imageUploader';

import { getFields } from 'app/libs/personal-data-form';

import { Fields } from 'view/modules/personalDataForm/types';

import { IUniversalStore } from 'view/modules/types';
import { initialState as personalDataFieldsInitialState } from 'view/modules/personalDataForm/reducers/fields';
import { initialState as personalDataNetworkInitialState } from 'view/modules/personalDataForm/reducers/network';
import { initialState as phoneFieldsInitialState } from 'view/modules/userPersonalDataPhone/reducers/fields';
import { initialState as phoneNetworkInitialState } from 'view/modules/userPersonalDataPhone/reducers/network';

const documents = [
    {
        id: '3bad76f21da444c586b23eefb8e8e9a4' as DocumentImageId,
        type: DocumentImageEntryType.PASSPORT_MAIN_PAGE,
    },
    {
        id: 'be70b8072c414d03a01b52f0db5dfdd8' as DocumentImageId,
        type: DocumentImageEntryType.REGISTRATION_PAGE,
    },
    {
        id: '9fe725c7b6a14280818ddeaf07c47d5e' as DocumentImageId,
        type: DocumentImageEntryType.SELFIE_WITH_PASSPORT,
    },
];

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

const filledImageUploader: Pick<IUniversalStore, 'imageUploader'> = {
    imageUploader: {
        PASSPORT_MAIN_PAGE: {
            images: [
                {
                    entityId: DocumentImageEntryType.PASSPORT_MAIN_PAGE,
                    imageId: '0c960f01-2fd1-49f1-bfbe-059dfecbda5e' as ImageUploaderImageId,
                    previewUrl: '',
                    uploaderData: {
                        id: '3bad76f21da444c586b23eefb8e8e9a4' as DocumentImageId,
                        type: DocumentImageEntryType.PASSPORT_MAIN_PAGE,
                    },
                    status: ImageUploaderImageStatus.SAVED,
                },
            ],
            getImageUploaderUrlStatus: RequestStatus.LOADED,
        },
        REGISTRATION_PAGE: {
            images: [
                {
                    entityId: DocumentImageEntryType.REGISTRATION_PAGE,
                    imageId: '0c960f01-2fd1-49f1-bfbe-059dfecbda5e' as ImageUploaderImageId,
                    previewUrl: '',
                    uploaderData: {
                        id: '3bad76f21da444c586b23eefb8e8e9a4' as DocumentImageId,
                        type: DocumentImageEntryType.REGISTRATION_PAGE,
                    },
                    status: ImageUploaderImageStatus.SAVED,
                },
            ],
            getImageUploaderUrlStatus: RequestStatus.LOADED,
        },
        SELFIE_WITH_PASSPORT: {
            images: [
                {
                    entityId: DocumentImageEntryType.SELFIE_WITH_PASSPORT,
                    imageId: '0c960f01-2fd1-49f1-bfbe-059dfecbda5e' as ImageUploaderImageId,
                    previewUrl: '',
                    uploaderData: {
                        id: '3bad76f21da444c586b23eefb8e8e9a4' as DocumentImageId,
                        type: DocumentImageEntryType.SELFIE_WITH_PASSPORT,
                    },
                    status: ImageUploaderImageStatus.SAVED,
                },
            ],
            getImageUploaderUrlStatus: RequestStatus.LOADED,
        },
    },
};

export const store: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    personalDataForm: {
        fields: personalDataFieldsInitialState,
        network: personalDataNetworkInitialState,
    },
    userPersonalDataPhone: {
        fields: phoneFieldsInitialState,
        network: phoneNetworkInitialState,
    },
    config: { isMobile: '' },
    imageUploader,
};

export const mobileStore: DeepPartial<IUniversalStore> = {
    config: { isMobile: 'iOS' },
    spa: {
        status: RequestStatus.LOADED,
    },
    personalDataForm: {
        fields: personalDataFieldsInitialState,
        network: personalDataNetworkInitialState,
    },
    userPersonalDataPhone: {
        fields: phoneFieldsInitialState,
        network: phoneNetworkInitialState,
    },
    imageUploader,
};

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

export const filledStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    legacyUser: {
        ...user,
        passportData: {
            ...user.passportData,
            documents,
        },
    },
    personalDataForm: {
        fields: {
            ...getFields(user),
            [Fields.DATA_PROCESSING_AGREEMENT]: {
                id: Fields.DATA_PROCESSING_AGREEMENT,
                value: true,
            },
        },
        network: personalDataNetworkInitialState,
    },
    userPersonalDataPhone: {
        fields: phoneFieldsInitialState,
        network: phoneNetworkInitialState,
    },
    config: { isMobile: '' },
    ...filledImageUploader,
};

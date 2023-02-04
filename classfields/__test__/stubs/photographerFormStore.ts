import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { IFlatQuestionnaireFlatKeyLocationNamespaceKeyLocation } from 'types/flatQuestionnaire';

import { OutstaffRoles } from 'types/outstaff';

import { initialState as photographerFormFields } from 'view/modules/outstaffPhotographerForm/reducers/fields';
import { initialState as photographerFormNetwork } from 'view/modules/outstaffPhotographerForm/reducers/network';
import { Fields } from 'view/modules/outstaffPhotographerForm/types';
import { IUniversalStore } from 'view/modules/types';

import { commonStore } from './common.ts';

export const photoRawUrl = 'https://photo.ru';
export const tour3dUrl = 'https://tour.ru';

export const baseStore: DeepPartial<IUniversalStore> = {
    ...commonStore,
    page: {
        params: {
            role: OutstaffRoles.photographer,
        },
    },
    outstaffPhotographerForm: {
        fields: photographerFormFields,
        network: photographerFormNetwork,
    },
    managerFlatQuestionnaire: {
        questionnaire: {
            flat: {
                entrance: 499,
                floor: 153,
                intercom: {
                    code: '333',
                },
                entranceInstruction: 'Взять ключ у консьержа',
                keyLocation: IFlatQuestionnaireFlatKeyLocationNamespaceKeyLocation.IN_OFFICE,
            },
        },
    },
    config: {
        isMobile: '',
    },
};

export const noFlatEntranceInfoStore: DeepPartial<IUniversalStore> = {
    ...commonStore,
    page: {
        params: {
            role: OutstaffRoles.photographer,
        },
    },
    outstaffPhotographerForm: {
        fields: photographerFormFields,
        network: photographerFormNetwork,
    },
};

export const skeletonStore: DeepPartial<IUniversalStore> = {
    ...baseStore,
    spa: {
        status: RequestStatus.PENDING,
    },
};

export const fullFilledStore: DeepPartial<IUniversalStore> = {
    ...baseStore,
    managerFlatQuestionnaire: {
        questionnaire: {
            media: {
                photoRawUrl,
                tour3dUrl,
            },
            flat: {
                entrance: 499,
                floor: 153,
                intercom: {
                    code: '333',
                },
                entranceInstruction: 'Взять ключ у консьержа',
                keyLocation: IFlatQuestionnaireFlatKeyLocationNamespaceKeyLocation.IN_OFFICE,
            },
        },
    },
    outstaffPhotographerForm: {
        fields: {
            [Fields.OFFER_PHOTO_RAW_URL]: {
                id: Fields.OFFER_PHOTO_RAW_URL,
                value: photoRawUrl,
            },
            [Fields.OFFER_3D_TOUR_URL]: {
                id: Fields.OFFER_3D_TOUR_URL,
                value: tour3dUrl,
            },
        },
        network: photographerFormNetwork,
    },
};

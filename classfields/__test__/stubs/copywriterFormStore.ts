import { DeepPartial } from 'utility-types';

import {
    FlatQuestionnaire_Furniture_Internet_InternetTypeNamespace_InternetType,
    FlatQuestionnaire_Furniture_Oven_OvenTypeNamespace_OvenType,
} from '@vertis/schema-registry/ts-types/realty/rent/api/flat_check_list';

import { RequestStatus } from 'realty-core/types/network';

import { OutstaffRoles } from 'types/outstaff';

import { FlatId, FlatStatus } from 'types/flat';

import {
    FlatQuestionnaireBuildingParkingNamespaceParking,
    FlatQuestionnaireFlatFlatTypeNamespaceFlatType,
    FlatQuestionnaireFlatRenovationTypeNamespaceRenovationType,
    FlatQuestionnaireFlatRentHistoryWhoRentedTypeNamespaceWhoRentedType,
    FlatQuestionnaireFlatRoomsNamespaceRooms,
    FlatQuestionnaireFlatWindowSideType,
    FlatQuestionnaireFlatWorldSideType,
    IFlatQuestionnaire,
    IFlatQuestionnaireFlatKeyLocationNamespaceKeyLocation,
    IFlatQuestionnaireGuaranteedPaymentStatus,
} from 'types/flatQuestionnaire';

import { ArendaFeedNamespaceAliasesBase, ImageNamespaces } from 'types/image';

import { ImageUploaderImageId, ImageUploaderImageStatus } from 'types/imageUploader';

import { IOutstaffFlatStore } from 'view/modules/outstaffFlat/reducers';
import { initialState as copywriterFormFields } from 'view/modules/outstaffCopywriterForm/reducers/fields';
import { initialState as copywriterFormNetwork } from 'view/modules/outstaffCopywriterForm/reducers/network';
import { Fields } from 'view/modules/outstaffCopywriterForm/types';
import { IUniversalStore } from 'view/modules/types';

import { IImageUploaderStore } from 'view/modules/imageUploader/types';

import { commonStore } from './common.ts';

export const offerCopyright =
    'Большая уютная квартира в центре Питера!\n' +
    '- описание\n' +
    '- описание\n' +
    '- описание\n\n' +
    'Все супер, заселяйтесь!';

export const manySymbolsOfferCopyright =
    'Уютная квартира в центре Питера!\n' +
    '- Большое описание, большое описание, большое описание, большое описание, большое описание,\n' +
    'большое описание, большое описание, большое описание, большое описание, большое описание,\n' +
    'большое описание, большое описание, большое описание, большое описание, большое описание,\n' +
    'большое описание, большое описание, большое описание, большое описание, большое описание,\n' +
    'большое описание, большое описание, большое описание, большое описание, большое описание,\n' +
    'большое описание, большое описание, большое описание, большое описание, большое описание,\n' +
    'большое описание, большое описание, большое описание, большое описание, большое описание,\n' +
    'большое описание, большое описание, большое описание, большое описание, большое описание,\n' +
    'большое описание, большое описание, большое описание, большое описание, большое описание,\n' +
    'большое описание, большое описание, большое описание, большое описание, большое описание,\n' +
    'большое описание, большое описание, большое описание, большое описание, большое описание,\n' +
    'большое описание, большое описание, большое описание, большое описание, большое описание,\n' +
    'большое описание, большое описание, большое описание, большое описание, большое описание,\n' +
    'большое описание, большое описание, большое описание, большое описание, большое описание,\n' +
    'большое описание, большое описание, большое описание, большое описание, большое описание,\n' +
    'большое описание, большое описание, большое описание, большое описание, большое описание,\n' +
    'большое описание, большое описание, большое описание, большое описание, большое описание,\n' +
    'большое описание, большое описание, большое описание, большое описание, большое описание,\n' +
    'большое описание, большое описание, большое описание, большое описание, большое описание,\n' +
    'большое описание, большое описание, большое описание, большое описание, большое описание,\n' +
    'большое описание, большое описание, большое описание, большое описание, большое описание,\n' +
    'большое описание, большое описание, большое описание, большое описание, большое описание,\n' +
    'большое описание, большое описание, большое описание, большое описание, большое описание,\n' +
    'Все супер, заселяйтесь!';

export const imageUploader: DeepPartial<IImageUploaderStore> = {
    '000000': {
        images: [
            {
                entityId: '000000',
                imageId: '0' as ImageUploaderImageId,
                previewUrl: '//avatars.mdst.yandex.net/get-arenda-feed/65725/29331d1b4e609836064ed5086302d2df/250x250',
                largeUrl: '//avatars.mdst.yandex.net/get-arenda-feed/65725/29331d1b4e609836064ed5086302d2df/250x250',
                uploaderData: {
                    groupId: 65725,
                    name: '29331d1b4e609836064ed5086302d2df',
                },
                status: ImageUploaderImageStatus.SAVED,
            },
            {
                entityId: '000000',
                imageId: '1' as ImageUploaderImageId,
                previewUrl: '//avatars.mdst.yandex.net/get-arenda-feed/65725/29331d1b4e609836064ed5086302d2df/250x250',
                largeUrl: '//avatars.mdst.yandex.net/get-arenda-feed/65725/29331d1b4e609836064ed5086302d2df/250x250',
                uploaderData: {
                    groupId: 65725,
                    name: 'dc49706c93fe3a08d69c732db8e0a323',
                },
                status: ImageUploaderImageStatus.SAVED,
            },
            {
                entityId: '000000',
                imageId: '2' as ImageUploaderImageId,
                previewUrl: '//avatars.mdst.yandex.net/get-arenda-feed/65725/29331d1b4e609836064ed5086302d2df/250x250',
                largeUrl: '//avatars.mdst.yandex.net/get-arenda-feed/65725/29331d1b4e609836064ed5086302d2df/250x250',
                uploaderData: {
                    groupId: 65725,
                    name: '97d6a8aba066648dede91edcc554cc28',
                },
                status: ImageUploaderImageStatus.SAVED,
            },
            {
                entityId: '000000',
                imageId: '3' as ImageUploaderImageId,
                previewUrl: '//avatars.mdst.yandex.net/get-arenda-feed/65725/29331d1b4e609836064ed5086302d2df/250x250',
                largeUrl: '//avatars.mdst.yandex.net/get-arenda-feed/65725/29331d1b4e609836064ed5086302d2df/250x250',
                uploaderData: {
                    groupId: 1,
                    name: '64e8292b3107c2c0739ecca649a1dbc2',
                },
                status: ImageUploaderImageStatus.SAVED,
            },
        ],
    },
};

export const flatQuestionnaire: DeepPartial<IFlatQuestionnaire> = {
    building: {
        floors: 10,
        elevators: {
            passengerAmount: 1,
            cargoAmount: 1,
        },
        hasConcierge: true,
        hasGarbageChute: true,
        hasWheelchairStorage: true,
        hasBarrier: true,
        hasModernEntrance: true,
        hasOption: 'опция',
        parking: [FlatQuestionnaireBuildingParkingNamespaceParking.UNDERGROUND],
    },
    flat: {
        entrance: 1,
        floor: 12,
        intercom: {
            code: '44',
        },
        rooms: FlatQuestionnaireFlatRoomsNamespaceRooms.THREE,
        area: 1000,
        balcony: {
            loggiaAmount: 2,
        },
        bathroom: {
            combinedAmount: 1,
            separatedAmount: 1,
        },
        windowSide: [FlatQuestionnaireFlatWindowSideType.YARD_SIDE],
        worldSide: [FlatQuestionnaireFlatWorldSideType.NORTH, FlatQuestionnaireFlatWorldSideType.SOUTH],
        doesOwnerGiveKey: true,
        keyLocation: IFlatQuestionnaireFlatKeyLocationNamespaceKeyLocation.IN_OFFICE,
        renovation: FlatQuestionnaireFlatRenovationTypeNamespaceRenovationType.EURO,
        flatType: FlatQuestionnaireFlatFlatTypeNamespaceFlatType.APARTMENTS,
        ownerDescription: 'Новый дом в ЖК комфорт класса',
        entranceInstruction: 'Два входа: с улицы и со двора',
        rentHistory: {
            whoRented: FlatQuestionnaireFlatRentHistoryWhoRentedTypeNamespaceWhoRentedType.NOBODY,
        },
    },
    furniture: {
        tv: {
            isPresent: true,
        },
        oven: {
            ovenType: FlatQuestionnaire_Furniture_Oven_OvenTypeNamespace_OvenType.ELECTRIC,
        },
        internet: {
            internetType: FlatQuestionnaire_Furniture_Internet_InternetTypeNamespace_InternetType.NO_CABLE,
            internetProvider: 'УЮТ 100 Мбс',
            price: 2000,
        },
        washingMachine: {
            isPresent: true,
        },
        dishWasher: {
            isPresent: true,
        },
        fridge: {
            isPresent: true,
        },
        conditioner: {
            isPresent: true,
        },
        other: {
            description: 'много другого',
        },
        warmFloor: {
            isPresent: true,
        },
        boiler: {
            isPresent: true,
        },
    },
    counters: {
        avgCounters: '5000',
        hasWaterCounter: true,
        hasElectricCounter: true,
        hasGasCounter: true,
        hasHeatingCounter: true,
    },
    payments: {
        adValue: '4000000',
        rentalValue: '5000000',
        needElectricPayment: true,
        needSanitationPayment: true,
        needGasPayment: true,
        needHeatingPayment: true,
        needInternetPayment: true,
        needAllReceiptPayments: true,
        needBarrierPayment: true,
        needParkingPayment: true,
        needConciergePayment: true,
    },
    tenantRequirements: {
        maxTenantCount: 5,
        hasWithChildrenRequirement: true,
        hasWithPetsRequirement: true,
        preferencesForTenants: 'Просто молодца',
    },
    media: {
        photoRawUrl: 'https//:yandex.ru',
        tour3dUrl: 'https//yandex.ru',
    },
    offerCopyright: '',
    yandexRentConditions: {
        guaranteedPaymentStatus: IFlatQuestionnaireGuaranteedPaymentStatus.YES,
    },
};

export const baseStore: DeepPartial<IUniversalStore> = {
    ...commonStore,
    page: {
        params: {
            role: OutstaffRoles.copywriter,
        },
    },
    outstaffCopywriterForm: {
        fields: copywriterFormFields,
        network: copywriterFormNetwork,
    },
    config: { isMobile: '' },
};

export const skeletonStore: DeepPartial<IUniversalStore> = {
    ...baseStore,
    spa: {
        status: RequestStatus.PENDING,
    },
};

export const withFlatQuesstionnaireStore: DeepPartial<IUniversalStore> = {
    ...baseStore,
    managerFlatQuestionnaire: {
        questionnaire: flatQuestionnaire,
    },
};

export const fullFilledStore: DeepPartial<IUniversalStore> = {
    ...baseStore,
    managerFlatQuestionnaire: {
        questionnaire: {
            ...flatQuestionnaire,
            offerCopyright,
        },
    },
    outstaffCopywriterForm: {
        fields: {
            [Fields.OFFER_COPYRIGHT]: {
                id: Fields.OFFER_COPYRIGHT,
                value: offerCopyright,
            },
        },
        network: copywriterFormNetwork,
    },
    outstaffFlat: getOutstaffFlat(),
    imageUploader,
};

export const withFlatApprovingPublishingStore: DeepPartial<IUniversalStore> = {
    ...fullFilledStore,
    outstaffFlat: getOutstaffFlat(FlatStatus.WORK_IN_PROGRESS),
};

export function getClearFormStore(isMobile?: boolean): DeepPartial<IUniversalStore> {
    return {
        ...baseStore,
        managerFlatQuestionnaire: {
            questionnaire: {
                ...flatQuestionnaire,
                offerCopyright: 'Хорошая квартира',
            },
        },
        config: { isMobile: isMobile ? 'IOS' : undefined },
    };
}

export function getOutstaffFlat(status?: FlatStatus): DeepPartial<IOutstaffFlatStore> {
    return {
        flat: {
            flatId: '000000' as FlatId,
            address: {
                address: 'г Санкт‑Петербург, Старо‑Петергофский пр‑кт, д 19',
                flatNumber: '7',
            },
            status: status || FlatStatus.LOOKING_FOR_TENANT,
            code: '19-COVID',
        },
        retouchedPhotos: [
            {
                namespace: ImageNamespaces.ARENDA_FEED,
                groupId: 65725,
                name: '29331d1b4e609836064ed5086302d2df',
                imageUrls: [
                    {
                        alias: ArendaFeedNamespaceAliasesBase._250x250,
                        url: '//avatars.mdst.yandex.net/get-arenda-feed/65725/29331d1b4e609836064ed5086302d2df/250x250',
                    },
                ],
            },
            {
                namespace: ImageNamespaces.ARENDA_FEED,
                groupId: 65725,
                name: 'dc49706c93fe3a08d69c732db8e0a323',
                imageUrls: [
                    {
                        alias: ArendaFeedNamespaceAliasesBase._250x250,
                        url: '//avatars.mdst.yandex.net/get-arenda-feed/65725/dc49706c93fe3a08d69c732db8e0a323/250x250',
                    },
                ],
            },
            {
                namespace: ImageNamespaces.ARENDA_FEED,
                groupId: 65725,
                name: '97d6a8aba066648dede91edcc554cc28',
                imageUrls: [
                    {
                        alias: ArendaFeedNamespaceAliasesBase._250x250,
                        url: '//avatars.mdst.yandex.net/get-arenda-feed/65725/97d6a8aba066648dede91edcc554cc28/250x250',
                    },
                ],
            },
            {
                namespace: ImageNamespaces.ARENDA_FEED,
                groupId: 65725,
                name: '64e8292b3107c2c0739ecca649a1dbc2',
                imageUrls: [
                    {
                        alias: ArendaFeedNamespaceAliasesBase._250x250,
                        url: '//avatars.mdst.yandex.net/get-arenda-feed/65725/64e8292b3107c2c0739ecca649a1dbc2/250x250',
                    },
                ],
            },
        ],
    };
}

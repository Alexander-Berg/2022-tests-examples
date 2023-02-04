import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { Flavor } from 'realty-core/types/utils';

import { FlatId, FlatStatus } from 'types/flat';
import { ClassifiedType } from 'types/publishing';
import { ImageNamespaces, DefaultNamespaceAliases, IFlatFeedImage } from 'types/image';

import {
    FlatQuestionnaireBuildingHouseType,
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

import { ImageUploaderImageId, ImageUploaderImageStatus } from 'types/imageUploader';

import { IUniversalStore } from 'view/modules/types';
import { Fields } from 'view/modules/managerFlatPublishingForm/types';
// eslint-disable-next-line max-len
import { initialState as flatPublishingFieldsInitialState } from 'view/modules/managerFlatPublishingForm/reducers/fields';
// eslint-disable-next-line max-len
import { initialState as flatPublishingNetworkInitialState } from 'view/modules/managerFlatPublishingForm/reducers/network';

import { IImageUploaderStore } from 'view/modules/imageUploader/types';

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
        getImageUploaderUrlStatus: RequestStatus.LOADED,
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
        parking: [FlatQuestionnaireBuildingParkingNamespaceParking.PUBLIC_PAID],
        houseType: FlatQuestionnaireBuildingHouseType.BUILDING_TYPE_WOOD,
    },
    flat: {
        entrance: 1,
        floor: 12,
        intercom: {
            code: '44',
        },
        rooms: FlatQuestionnaireFlatRoomsNamespaceRooms.THREE,
        area: 1000,
        kitchenSpace: 1002,
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
        oven: {},
        internet: {
            internetProvider: 'УЮТ 100 Мбс',
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

export const retouchedPhotos: DeepPartial<IFlatFeedImage[]> = [
    {
        namespace: 'arenda-feed' as ImageNamespaces.ARENDA_FEED,
        groupId: 1401666,
        name: '866aa414ecce687c6ea48462fee972ea',
        imageUrls: [
            {
                alias: 'orig' as DefaultNamespaceAliases,
                url: '//avatars.mdst.yandex.net/get-arenda-feed/1401666/866aa414ecce687c6ea48462fee972ea/orig',
            },
            {
                alias: '250x250' as DefaultNamespaceAliases,
                url:
                    // eslint-disable-next-line max-len
                    '//avatars.mdst.yandex.net/get-arenda-feed/1401666/866aa414ecce687c6ea48462fee972ea/250x250',
            },
            {
                alias: '1024x1024' as DefaultNamespaceAliases,
                url:
                    // eslint-disable-next-line max-len
                    '//avatars.mdst.yandex.net/get-arenda-feed/1401666/866aa414ecce687c6ea48462fee972ea/1024x1024',
            },
        ],
    },
    {
        namespace: 'arenda-feed' as ImageNamespaces.ARENDA_FEED,
        groupId: 1401666,
        name: 'afb0ff108270ec867f580c00e79d85dd',
        imageUrls: [
            {
                alias: 'orig' as DefaultNamespaceAliases,
                url: '//avatars.mdst.yandex.net/get-arenda-feed/1401666/afb0ff108270ec867f580c00e79d85dd/orig',
            },
            {
                alias: '250x250' as DefaultNamespaceAliases,
                url:
                    // eslint-disable-next-line max-len
                    '//avatars.mdst.yandex.net/get-arenda-feed/1401666/afb0ff108270ec867f580c00e79d85dd/250x250',
            },
            {
                alias: '1024x1024' as DefaultNamespaceAliases,
                url:
                    // eslint-disable-next-line max-len
                    '//avatars.mdst.yandex.net/get-arenda-feed/1401666/afb0ff108270ec867f580c00e79d85dd/1024x1024',
            },
        ],
    },
    {
        namespace: 'arenda-feed' as ImageNamespaces.ARENDA_FEED,
        groupId: 65725,
        name: '921693eee1620a3189963d3e95df269d',
        imageUrls: [
            {
                alias: 'orig' as DefaultNamespaceAliases,
                url: '//avatars.mdst.yandex.net/get-arenda-feed/65725/921693eee1620a3189963d3e95df269d/orig',
            },
            {
                alias: '250x250' as DefaultNamespaceAliases,
                url: '//avatars.mdst.yandex.net/get-arenda-feed/65725/921693eee1620a3189963d3e95df269d/250x250',
            },
            {
                alias: '1024x1024' as DefaultNamespaceAliases,
                url:
                    // eslint-disable-next-line max-len
                    '//avatars.mdst.yandex.net/get-arenda-feed/65725/921693eee1620a3189963d3e95df269d/1024x1024',
            },
        ],
    },
    {
        namespace: 'arenda-feed' as ImageNamespaces.ARENDA_FEED,
        groupId: 65725,
        name: 'fdaaa8ac3df70a54ddab3e9464f722fd',
        imageUrls: [
            {
                alias: 'orig' as DefaultNamespaceAliases,
                url: '//avatars.mdst.yandex.net/get-arenda-feed/65725/fdaaa8ac3df70a54ddab3e9464f722fd/orig',
            },
            {
                alias: '250x250' as DefaultNamespaceAliases,
                url: '//avatars.mdst.yandex.net/get-arenda-feed/65725/fdaaa8ac3df70a54ddab3e9464f722fd/250x250',
            },
            {
                alias: '1024x1024' as DefaultNamespaceAliases,
                url:
                    // eslint-disable-next-line max-len
                    '//avatars.mdst.yandex.net/get-arenda-feed/65725/fdaaa8ac3df70a54ddab3e9464f722fd/1024x1024',
            },
        ],
    },
];

export const offerCopyright =
    'Большая квартира\n' +
    ' - описание\n' +
    ' - описание\n' +
    ' - описание\n' +
    'Смайлик выглядел криповым\n' +
    'Текст текст текст текст текст текст текст';

export const store: DeepPartial<IUniversalStore> = {
    managerFlat: {
        flat: {
            flatId: '000000' as FlatId,
            address: {
                address: 'г Санкт‑Петербург, Старо‑Петергофский пр‑кт, д 19',
                flatNumber: '7',
            },
            status: FlatStatus.LOOKING_FOR_TENANT,
            code: '19-COVID',
        },
    },
    page: {
        params: {
            flatId: '000000' as Flavor<string, 'FlatID'>,
        },
    },
    spa: {
        status: RequestStatus.LOADED,
    },
    managerFlatPublishingForm: {
        fields: {
            [Fields.OFFER_3D_TOUR_URL]: {
                value: 'https://st.yandex-team.ru/REALTYBACK-5792',
            },
            [Fields.OFFER_COPYRIGHT]: {
                value: offerCopyright,
            },
        },
        network: {
            updateOfferMediaStatus: RequestStatus.LOADED,
        },
    },
    managerFlatQuestionnaire: {},
    imageUploader: { '000000': { images: [], getImageUploaderUrlStatus: RequestStatus.LOADED } },
    config: { isMobile: '' },
};

export const invalidStatusStore: DeepPartial<IUniversalStore> = {
    managerFlat: {
        flat: {
            flatId: '000000' as FlatId,
            address: {
                address: 'г Санкт‑Петербург, Старо‑Петергофский пр‑кт, д 19',
                flatNumber: '7',
            },
            status: FlatStatus.RENTED,
            code: '19-COVID',
        },
    },
    page: {
        params: {
            flatId: '000000' as Flavor<string, 'FlatID'>,
        },
    },
    spa: {
        status: RequestStatus.LOADED,
    },
    managerFlatPublishingForm: {
        fields: {
            [Fields.OFFER_3D_TOUR_URL]: {
                value: 'https://st.yandex-team.ru/REALTYBACK-5792',
            },
            [Fields.OFFER_COPYRIGHT]: {
                value: offerCopyright,
            },
        },
        network: {
            updateOfferMediaStatus: RequestStatus.LOADED,
        },
    },
    managerFlatQuestionnaire: {},
    imageUploader: { '000000': { images: [], getImageUploaderUrlStatus: RequestStatus.LOADED } },
    config: { isMobile: '' },
};

export const skeletonStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.PENDING,
    },
    managerFlatPublishingForm: {
        fields: {
            [Fields.OFFER_3D_TOUR_URL]: {
                value: 'https://st.yandex-team.ru/REALTYBACK-5792',
            },
            [Fields.OFFER_COPYRIGHT]: {
                value: offerCopyright,
            },
        },
        network: {
            updateOfferMediaStatus: RequestStatus.LOADED,
        },
    },
    managerFlatQuestionnaire: {},
    imageUploader: { '000000': { images: [], getImageUploaderUrlStatus: RequestStatus.LOADED } },
    config: { isMobile: '' },
};

export const publishedStore: DeepPartial<IUniversalStore> = {
    config: {
        realtyUrl: 'https://realty.test.vertis.yandex.ru/',
        isMobile: '',
    },
    managerFlat: {
        flat: {
            flatId: '000000' as FlatId,
            address: {
                address: 'г Санкт‑Петербург, Старо‑Петергофский пр‑кт, д 19',
                flatNumber: '7',
            },
            status: FlatStatus.LOOKING_FOR_TENANT,
            code: '19-COVID',
            realtyOfferId: 'CAESJwgBEg4IARoKMTA2OTI1MzI4MRoTMzg0MjgyNjA0MjE0OTQzNDU2NRoA',
        },
        retouchedPhotos,
        classifiedsPubStatuses: [
            {
                classifiedType: ClassifiedType.YANDEX_REALTY,
                enabled: true,
            },
            {
                classifiedType: ClassifiedType.AVITO,
                enabled: true,
            },
            {
                classifiedType: ClassifiedType.CIAN,
                enabled: true,
            },
        ],
    },
    page: {
        params: {
            flatId: '000000' as Flavor<string, 'FlatID'>,
        },
    },
    spa: {
        status: RequestStatus.LOADED,
    },
    managerFlatPublishingForm: {
        fields: {
            [Fields.OFFER_3D_TOUR_URL]: {
                value: 'https://st.yandex-team.ru/REALTYBACK-5792',
            },
            [Fields.OFFER_COPYRIGHT]: {
                value: offerCopyright,
            },
        },
        network: {
            updateOfferMediaStatus: RequestStatus.LOADED,
        },
    },
    managerFlatQuestionnaire: {
        questionnaire: {
            ...flatQuestionnaire,
            offerCopyright,
        },
    },
    imageUploader,
};

export const notPublishedStore: DeepPartial<IUniversalStore> = {
    managerFlat: {
        flat: {
            flatId: '000000' as FlatId,
            address: {
                address: 'г Санкт‑Петербург, Старо‑Петергофский пр‑кт, д 19',
                flatNumber: '7',
            },
            status: FlatStatus.LOOKING_FOR_TENANT,
            code: '19-COVID',
        },
        retouchedPhotos,
        classifiedsPubStatuses: [
            {
                classifiedType: ClassifiedType.YANDEX_REALTY,
                enabled: false,
            },
            {
                classifiedType: ClassifiedType.AVITO,
                enabled: false,
            },
            {
                classifiedType: ClassifiedType.CIAN,
                enabled: false,
            },
        ],
    },
    page: {
        params: {
            flatId: '000000' as Flavor<string, 'FlatID'>,
        },
    },
    spa: {
        status: RequestStatus.LOADED,
    },
    managerFlatPublishingForm: {
        fields: {
            [Fields.OFFER_3D_TOUR_URL]: {
                value: 'https://st.yandex-team.ru/REALTYBACK-5792',
            },
            [Fields.OFFER_COPYRIGHT]: {
                value: offerCopyright,
            },
        },
        network: {
            updateOfferMediaStatus: RequestStatus.LOADED,
        },
    },
    managerFlatQuestionnaire: {
        questionnaire: {
            ...flatQuestionnaire,
            offerCopyright,
        },
    },
    imageUploader,
    config: { isMobile: '' },
};

export const waitingForApprovingStore: DeepPartial<IUniversalStore> = {
    managerFlat: {
        flat: {
            flatId: '000000' as FlatId,
            address: {
                address: 'г Санкт‑Петербург, Старо‑Петергофский пр‑кт, д 19',
                flatNumber: '7',
            },
            status: FlatStatus.WORK_IN_PROGRESS,
            code: '19-COVID',
        },
        retouchedPhotos,
    },
    page: {
        params: {
            flatId: '000000' as Flavor<string, 'FlatID'>,
        },
    },
    spa: {
        status: RequestStatus.LOADED,
    },
    managerFlatPublishingForm: {
        fields: {
            [Fields.OFFER_3D_TOUR_URL]: {
                value: 'https://st.yandex-team.ru/REALTYBACK-5792',
            },
            [Fields.OFFER_COPYRIGHT]: {
                value: offerCopyright,
            },
        },
        network: {
            updateOfferMediaStatus: RequestStatus.LOADED,
        },
    },
    managerFlatQuestionnaire: {
        questionnaire: {
            ...flatQuestionnaire,
            offerCopyright,
        },
    },
    imageUploader,
    config: { isMobile: '' },
};

export const formNotFilledStore: DeepPartial<IUniversalStore> = {
    managerFlat: {
        flat: {
            flatId: '000000' as FlatId,
            address: {
                address: 'г Санкт‑Петербург, Старо‑Петергофский пр‑кт, д 19',
                flatNumber: '7',
            },
            status: FlatStatus.WORK_IN_PROGRESS,
            code: '19-COVID',
        },
        retouchedPhotos,
    },
    page: {
        params: {
            flatId: '000000' as Flavor<string, 'FlatID'>,
        },
    },
    spa: {
        status: RequestStatus.LOADED,
    },
    managerFlatPublishingForm: {
        fields: flatPublishingFieldsInitialState,
        network: flatPublishingNetworkInitialState,
    },
    managerFlatQuestionnaire: {
        questionnaire: {
            ...flatQuestionnaire,
            offerCopyright,
        },
    },
    imageUploader,
    config: { isMobile: '' },
};

export const formNotFilledMobileStore: DeepPartial<IUniversalStore> = {
    config: { isMobile: 'iOS' },
    managerFlat: {
        flat: {
            flatId: '000000' as FlatId,
            address: {
                address: 'г Санкт‑Петербург, Старо‑Петергофский пр‑кт, д 19',
                flatNumber: '7',
            },
            status: FlatStatus.WORK_IN_PROGRESS,
            code: '19-COVID',
        },
        retouchedPhotos,
    },
    page: {
        params: {
            flatId: '000000' as Flavor<string, 'FlatID'>,
        },
    },
    spa: {
        status: RequestStatus.LOADED,
    },
    managerFlatPublishingForm: {
        fields: flatPublishingFieldsInitialState,
        network: flatPublishingNetworkInitialState,
    },
    managerFlatQuestionnaire: {
        questionnaire: {
            ...flatQuestionnaire,
            offerCopyright,
        },
    },
    imageUploader,
};

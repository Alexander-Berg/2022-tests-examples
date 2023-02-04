import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';
import { Flavor } from 'realty-core/types/utils';

import { IFLatQuestionnaireFlatCleannessType } from 'types/flatQuestionnaire';
import { PaymentsCommissionValue } from 'types/flat';
import { FlatId, FlatStatus, FlatInfoFlatType, FlatInfoFlatRooms } from 'types/flat';

import { Fields } from 'view/modules/managerFlatQuestionnaireForm/types';
import { IUniversalStore } from 'view/modules/types';
// eslint-disable-next-line max-len
import { initialState as flatQuestionnaireFieldsInitialState } from 'view/modules/managerFlatQuestionnaireForm/reducers/fields';
// eslint-disable-next-line max-len
import { initialState as flatQuestionnaireNetworkInitialState } from 'view/modules/managerFlatQuestionnaireForm/reducers/network';

export const offerCopyright =
    'Большая квартира\n' +
    ' - описание\n' +
    ' - описание\n' +
    ' - описание\n' +
    'Смайлик выглядил криповым\n' +
    'Текст текста текст текст текст текст текст';

export const store: DeepPartial<IUniversalStore> = {
    page: {
        params: {
            flatId: 'ffc21a950bf247818405d91537154696' as Flavor<string, 'FlatID'>,
        },
    },
    spa: {
        status: RequestStatus.LOADED,
    },
    imageUploader: {
        ffc21a950bf247818405d91537154696: {
            images: [],
        },
    },
    managerFlatQuestionnaire: {},
    managerFlatQuestionnaireForm: {
        fields: flatQuestionnaireFieldsInitialState,
        network: flatQuestionnaireNetworkInitialState,
    },
    managerFlat: {
        flat: {
            flatId: 'ffc21a950bf247818405d91537154696' as FlatId,
            status: FlatStatus.CONFIRMED,
            flatInfo: {},
        },
        flatExcerptsRequests: [],
    },
    config: { isMobile: '' },
};

export const mobileStore: DeepPartial<IUniversalStore> = {
    config: { isMobile: 'iOS' },
    spa: {
        status: RequestStatus.LOADED,
    },
    imageUploader: {
        ffc21a950bf247818405d91537154696: {
            images: [],
        },
    },
    managerFlatQuestionnaireForm: {
        fields: flatQuestionnaireFieldsInitialState,
        network: flatQuestionnaireNetworkInitialState,
    },
    managerFlat: {
        flat: {
            flatId: 'ffc21a950bf247818405d91537154696' as FlatId,
            status: FlatStatus.CONFIRMED,
            flatInfo: {},
        },
        documents: {},
        flatExcerptsRequests: [],
    },
};

export const skeletonStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.PENDING,
    },
    imageUploader: {
        ffc21a950bf247818405d91537154696: {
            images: [],
        },
    },
    managerFlatQuestionnaireForm: {
        fields: flatQuestionnaireFieldsInitialState,
        network: flatQuestionnaireNetworkInitialState,
    },
    managerFlat: {
        flat: {
            flatId: 'ffc21a950bf247818405d91537154696' as FlatId,
            status: FlatStatus.CONFIRMED,
            flatInfo: {},
        },
        flatExcerptsRequests: [],
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
    imageUploader: {
        ffc21a950bf247818405d91537154696: {
            images: [],
        },
    },
    managerFlatQuestionnaire: {
        questionnaire: {
            media: {
                photoRawUrl: '1',
                photoRetouchedUrl: '2',
                tour3dUrl: '3',
            },
            offerCopyright,
        },
    },
    managerFlatQuestionnaireForm: {
        fields: {
            [Fields.BUILDING_FLOORS]: {
                id: Fields.BUILDING_FLOORS,
                value: 22,
            },
            [Fields.BUILDING_ELEVATORS_PASSENGER_AMOUNT]: {
                id: Fields.BUILDING_ELEVATORS_PASSENGER_AMOUNT,
                value: 5,
            },
            [Fields.BUILDING_ELEVATORS_CARGO_AMOUNT]: {
                id: Fields.BUILDING_ELEVATORS_CARGO_AMOUNT,
                value: 12,
            },
            [Fields.BUILDING_HAS_CONCIERGE]: {
                id: Fields.BUILDING_HAS_CONCIERGE,
                value: true,
            },
            [Fields.BUILDING_HAS_GARBAGE_CHUTE]: {
                id: Fields.BUILDING_HAS_GARBAGE_CHUTE,
                value: true,
            },
            [Fields.BUILDING_HAS_WHEELCHAIR_STORAGE]: {
                id: Fields.BUILDING_HAS_WHEELCHAIR_STORAGE,
                value: true,
            },
            [Fields.BUILDING_HAS_BARRIER]: {
                id: Fields.BUILDING_HAS_BARRIER,
                value: true,
            },
            [Fields.BUILDING_HOUSE_TYPE]: {
                id: Fields.BUILDING_HOUSE_TYPE,
                value: undefined,
            },
            [Fields.BUILDING_OTHER_THINGS]: {
                id: Fields.BUILDING_OTHER_THINGS,
                value: 'Детский сад в доме',
            },
            [Fields.BUILDING_PARKING_TYPE_BACKYARD_OR_PUBLIC_FREE]: {
                id: Fields.BUILDING_PARKING_TYPE_BACKYARD_OR_PUBLIC_FREE,
                value: true,
            },
            [Fields.BUILDING_PARKING_TYPE_PUBLIC_PAID]: {
                id: Fields.BUILDING_PARKING_TYPE_PUBLIC_PAID,
                value: true,
            },
            [Fields.BUILDING_PARKING_TYPE_BEHIND_BARRIER]: {
                id: Fields.BUILDING_PARKING_TYPE_BEHIND_BARRIER,
                value: true,
            },
            [Fields.BUILDING_PARKING_TYPE_UNDERGROUND]: {
                id: Fields.BUILDING_PARKING_TYPE_UNDERGROUND,
                value: true,
            },
            [Fields.BUILDING_UNDERGROUND_PARKING_INFO_COMMENT]: {
                id: Fields.BUILDING_UNDERGROUND_PARKING_INFO_COMMENT,
                value: 'Подземная парковка',
            },
            [Fields.BUILDING_TRANSPORT_ACCESSIBILITY]: {
                id: Fields.BUILDING_TRANSPORT_ACCESSIBILITY,
                value: '',
            },
            [Fields.BUILDING_HAS_MODERN_ENTRANCE]: {
                id: Fields.BUILDING_HAS_MODERN_ENTRANCE,
                value: true,
            },
            [Fields.FLAT_ENTRANCE]: {
                id: Fields.FLAT_ENTRANCE,
                value: 4,
            },
            [Fields.FLAT_ENTRANCE_INSTRUCTION]: {
                id: Fields.FLAT_ENTRANCE_INSTRUCTION,
                value: 'Открой дверь ключом и входи',
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
            [Fields.FLAT_KITCHEN_SPACE]: {
                id: Fields.FLAT_KITCHEN_SPACE,
                value: 100,
            },
            [Fields.FLAT_BALCONY_AMOUNT]: {
                id: Fields.FLAT_BALCONY_AMOUNT,
                value: 3,
            },
            [Fields.FLAT_LOGGIA_AMOUNT]: {
                id: Fields.FLAT_LOGGIA_AMOUNT,
                value: 2,
            },
            [Fields.FLAT_BATHROOM_COMBINED_AMOUNT]: {
                id: Fields.FLAT_BATHROOM_COMBINED_AMOUNT,
                value: 2,
            },
            [Fields.FLAT_BATHROOM_SEPARATED_AMOUNT]: {
                id: Fields.FLAT_BATHROOM_SEPARATED_AMOUNT,
                value: 2,
            },
            [Fields.FLAT_RENOVATION_TYPE]: {
                id: Fields.FLAT_RENOVATION_TYPE,
                value: 'BY_DESIGN',
            },
            [Fields.FLAT_WINDOW_SIDE]: {
                id: Fields.FLAT_WINDOW_SIDE,
                value: ['YARD_SIDE', 'STREET_SIDE'],
            },
            [Fields.FLAT_WORLD_SIDE]: {
                id: Fields.FLAT_WORLD_SIDE,
                value: ['NORTH', 'NORTH_EAST', 'SOUTH', 'EAST', 'WEST', 'NORTH_WEST', 'SOUTH_EAST', 'SOUTH_WEST'],
            },
            [Fields.FLAT_DOES_OWNER_GIVE_KEY]: {
                id: Fields.FLAT_DOES_OWNER_GIVE_KEY,
                value: true,
            },
            [Fields.FLAT_KEY_LOCATION]: {
                id: Fields.FLAT_KEY_LOCATION,
                value: 'IN_OFFICE',
            },
            [Fields.FLAT_CLEANNESS]: {
                id: Fields.FLAT_CLEANNESS,
                value: IFLatQuestionnaireFlatCleannessType.CLEAN,
            },
            [Fields.FLAT_CLEANNESS_COMMENT]: {
                id: Fields.FLAT_CLEANNESS_COMMENT,
            },
            [Fields.FLAT_RENT_HISTORY_WHO_RENTED_BEFORE]: {
                id: Fields.FLAT_RENT_HISTORY_WHO_RENTED_BEFORE,
                value: 'OWNER',
            },
            [Fields.FLAT_OWNER_DESCRIPTION]: {
                id: Fields.FLAT_OWNER_DESCRIPTION,
                value: 'Квартира огонь',
            },

            [Fields.FURNITURE_TV_IS_PRESENT]: {
                id: Fields.FURNITURE_TV_IS_PRESENT,
                value: true,
            },
            [Fields.FURNITURE_INTERNET_TYPE]: {
                id: Fields.FURNITURE_INTERNET_TYPE,
                value: 'ONLY_CABLE',
            },
            [Fields.FURNITURE_INTERNET_PROVIDER]: {
                id: Fields.FURNITURE_INTERNET_PROVIDER,
                value: 'Ростелеком',
            },
            [Fields.FURNITURE_INTERNET_PRICE]: {
                id: Fields.FURNITURE_INTERNET_PRICE,
                value: 300,
            },
            [Fields.FURNITURE_INTERNET_CAN_RENEW_PROVIDER_CONTRACT]: {
                id: Fields.FURNITURE_INTERNET_CAN_RENEW_PROVIDER_CONTRACT,
                value: true,
            },
            [Fields.FURNITURE_OVEN_IS_PRESENT]: {
                id: Fields.FURNITURE_OVEN_IS_PRESENT,
                value: true,
            },
            [Fields.FURNITURE_OVEN_TYPE]: {
                id: Fields.FURNITURE_OVEN_TYPE,
                value: 'ELECTRIC',
            },
            [Fields.FURNITURE_WASHING_MACHINE_IS_PRESENT]: {
                id: Fields.FURNITURE_WASHING_MACHINE_IS_PRESENT,
                value: true,
            },
            [Fields.FURNITURE_DISH_WASHER_IS_PRESENT]: {
                id: Fields.FURNITURE_DISH_WASHER_IS_PRESENT,
                value: true,
            },
            [Fields.FURNITURE_DRYING_MACHINE_IS_PRESENT]: {
                id: Fields.FURNITURE_DRYING_MACHINE_IS_PRESENT,
                value: true,
            },
            [Fields.FURNITURE_FRIDGE_IS_PRESENT]: {
                id: Fields.FURNITURE_FRIDGE_IS_PRESENT,
                value: true,
            },
            [Fields.FURNITURE_CONDITIONER_IS_PRESENT]: {
                id: Fields.FURNITURE_CONDITIONER_IS_PRESENT,
                value: true,
            },
            [Fields.FURNITURE_OTHER_THINGS]: {
                id: Fields.FURNITURE_OTHER_THINGS,
                value: 'Рисоварка',
            },
            [Fields.FURNITURE_BOILER_IS_PRESENT]: {
                id: Fields.FURNITURE_BOILER_IS_PRESENT,
                value: true,
            },
            [Fields.FURNITURE_WARM_FLOOR_IS_PRESENT]: {
                id: Fields.FURNITURE_WARM_FLOOR_IS_PRESENT,
                value: true,
            },

            [Fields.COUNTERS_AVERAGE_CONSUMPTION]: {
                id: Fields.COUNTERS_AVERAGE_CONSUMPTION,
                value: '100500',
            },
            [Fields.COUNTERS_HAS_WATER_COUNTER]: {
                id: Fields.COUNTERS_HAS_WATER_COUNTER,
                value: true,
            },
            [Fields.COUNTERS_HAS_ELECTRIC_COUNTER]: {
                id: Fields.COUNTERS_HAS_ELECTRIC_COUNTER,
                value: true,
            },
            [Fields.COUNTERS_HAS_GAS_COUNTER]: {
                id: Fields.COUNTERS_HAS_GAS_COUNTER,
                value: true,
            },
            [Fields.COUNTERS_HAS_HEATING_COUNTER]: {
                id: Fields.COUNTERS_HAS_HEATING_COUNTER,
                value: true,
            },

            [Fields.PAYMENTS_RENTAL_VALUE]: {
                id: Fields.PAYMENTS_RENTAL_VALUE,
                value: 50000000,
            },
            [Fields.PAYMENTS_TEMPORARY_RENTAL_VALUE]: {
                id: Fields.PAYMENTS_TEMPORARY_RENTAL_VALUE,
                value: 40000000,
            },
            [Fields.PAYMENTS_TEMPORARY_PERIOD_MONTHS]: {
                id: Fields.PAYMENTS_TEMPORARY_PERIOD_MONTHS,
                value: 5,
            },
            [Fields.PAYMENTS_COMMISSION_VALUE]: {
                id: Fields.PAYMENTS_COMMISSION_VALUE,
                value: PaymentsCommissionValue.SEVEN,
            },
            [Fields.PAYMENTS_NEED_ELECTRIC_PAYMENT]: {
                id: Fields.PAYMENTS_NEED_ELECTRIC_PAYMENT,
                value: true,
            },
            [Fields.PAYMENTS_NEED_SANITATION_PAYMENT]: {
                id: Fields.PAYMENTS_NEED_SANITATION_PAYMENT,
                value: true,
            },
            [Fields.PAYMENTS_NEED_GAS_PAYMENT]: {
                id: Fields.PAYMENTS_NEED_GAS_PAYMENT,
                value: true,
            },
            [Fields.PAYMENTS_NEED_HEATING_PAYMENT]: {
                id: Fields.PAYMENTS_NEED_HEATING_PAYMENT,
                value: true,
            },
            [Fields.PAYMENTS_NEED_INTERNET_PAYMENT]: {
                id: Fields.PAYMENTS_NEED_INTERNET_PAYMENT,
                value: true,
            },
            [Fields.PAYMENTS_NEED_ALL_RECEIPT_PAYMENTS]: {
                id: Fields.PAYMENTS_NEED_ALL_RECEIPT_PAYMENTS,
                value: true,
            },
            [Fields.PAYMENTS_NEED_CONCIERGE_PAYMENT]: {
                id: Fields.PAYMENTS_NEED_CONCIERGE_PAYMENT,
                value: true,
            },
            [Fields.PAYMENTS_NEED_PARKING_PAYMENT]: {
                id: Fields.PAYMENTS_NEED_PARKING_PAYMENT,
                value: true,
            },
            [Fields.PAYMENTS_NEED_BARRIER_PAYMENT]: {
                id: Fields.PAYMENTS_NEED_BARRIER_PAYMENT,
                value: true,
            },

            [Fields.TENANT_REQUIREMENTS_MAX_TENANT_COUNT]: {
                id: Fields.TENANT_REQUIREMENTS_MAX_TENANT_COUNT,
                value: 90,
            },
            [Fields.TENANT_REQUIREMENTS_HAS_WITH_CHILDREN]: {
                id: Fields.TENANT_REQUIREMENTS_HAS_WITH_CHILDREN,
                value: true,
            },
            [Fields.TENANT_REQUIREMENTS_HAS_WITH_PETS]: {
                id: Fields.TENANT_REQUIREMENTS_HAS_WITH_PETS,
                value: true,
            },
            [Fields.TENANT_REQUIREMENTS_PREFERENCES_FOR_TENANTS]: {
                id: Fields.TENANT_REQUIREMENTS_PREFERENCES_FOR_TENANTS,
                value: 'Просто молодца',
            },

            [Fields.MEDIA_PHOTO_RAW_URL]: {
                id: Fields.MEDIA_PHOTO_RAW_URL,
                value: '1',
            },
            [Fields.MEDIA_PHOTO_RETOUCHED_URL]: {
                id: Fields.MEDIA_PHOTO_RETOUCHED_URL,
                value: '2',
            },
            [Fields.MEDIA_TOUR_3D_URL]: {
                id: Fields.MEDIA_TOUR_3D_URL,
                value: '3',
            },
            [Fields.OFFER_COPYRIGHT]: {
                id: Fields.OFFER_COPYRIGHT,
                value: offerCopyright,
            },
            [Fields.GUARANTEED_PAYMENT_STATUS]: {
                id: Fields.GUARANTEED_PAYMENT_STATUS,
                value: 'YES',
            },
            [Fields.GUARANTEED_PAYMENT_FROM]: {
                id: Fields.GUARANTEED_PAYMENT_FROM,
                value: '',
            },
            [Fields.ONLY_ONLINE_SHOWINGS]: {
                id: Fields.ONLY_ONLINE_SHOWINGS,
                value: false,
            },
            [Fields.POSSIBLE_CHECK_IN_DATE]: {
                id: Fields.POSSIBLE_CHECK_IN_DATE,
                value: '',
            },
            [Fields.ADDITIONAL_COMMENT]: {
                id: Fields.ADDITIONAL_COMMENT,
                value: '',
            },
            [Fields.MOVE_OUT_DATE]: {
                id: Fields.MOVE_OUT_DATE,
                value: '',
            },
            [Fields.PAYMENTS_SUM]: {
                id: Fields.PAYMENTS_SUM,
                value: 22,
            },
            [Fields.PAYMENTS_NOT_NEEDED]: {
                id: Fields.PAYMENTS_NOT_NEEDED,
                value: false,
            },
            [Fields.PAYMENTS_AVERAGE_ALL_RECEIPT_COST]: {
                id: Fields.PAYMENTS_AVERAGE_ALL_RECEIPT_COST,
                value: 33,
            },
            [Fields.PAYMENTS_AVERAGE_CONCIERGE_COST]: {
                id: Fields.PAYMENTS_AVERAGE_CONCIERGE_COST,
                value: 33,
            },
            [Fields.PAYMENTS_AVERAGE_PARKING_COST]: {
                id: Fields.PAYMENTS_AVERAGE_PARKING_COST,
                value: 33,
            },
            [Fields.PAYMENTS_AVERAGE_BARRIER_COST]: {
                id: Fields.PAYMENTS_AVERAGE_BARRIER_COST,
                value: 33,
            },
            [Fields.PAYMENTS_AVERAGE_INTERNET_COST]: {
                id: Fields.PAYMENTS_AVERAGE_INTERNET_COST,
                value: 33,
            },
            [Fields.PAYMENTS_AVERAGE_HEATING_COST]: {
                id: Fields.PAYMENTS_AVERAGE_HEATING_COST,
                value: 33,
            },
            [Fields.PAYMENTS_AVERAGE_GAS_COST]: {
                id: Fields.PAYMENTS_AVERAGE_GAS_COST,
                value: 33,
            },
            [Fields.PAYMENTS_AVERAGE_SANITATION_COST]: {
                id: Fields.PAYMENTS_AVERAGE_SANITATION_COST,
                value: 33,
            },
            [Fields.PAYMENTS_AVERAGE_WATER_COST]: {
                id: Fields.PAYMENTS_AVERAGE_WATER_COST,
                value: 33,
            },
            [Fields.PAYMENTS_NEED_WATER_PAYMENT]: {
                id: Fields.PAYMENTS_NEED_WATER_PAYMENT,
                value: true,
            },
            [Fields.PAYMENTS_AVERAGE_ELECTRIC_COST]: {
                id: Fields.PAYMENTS_AVERAGE_ELECTRIC_COST,
                value: 22,
            },
            [Fields.FURNITURE_VACUUM_CLEANER_IS_PRESENT]: {
                id: Fields.FURNITURE_VACUUM_CLEANER_IS_PRESENT,
                value: true,
            },
            [Fields.FURNITURE_MICROWAVE_IS_PRESENT]: {
                id: Fields.FURNITURE_MICROWAVE_IS_PRESENT,
                value: true,
            },
            [Fields.FURNITURE_SOFA_UNFOLDS]: {
                id: Fields.FURNITURE_SOFA_UNFOLDS,
                value: true,
            },
            [Fields.FURNITURE_BEDCLOTHES_IS_PRESENT]: {
                id: Fields.FURNITURE_BEDCLOTHES_IS_PRESENT,
                value: true,
            },
            [Fields.FURNITURE_DISHES_DESCRIPTION]: {
                id: Fields.FURNITURE_DISHES_DESCRIPTION,
                value: '',
            },
            [Fields.FURNITURE_DISHES_IS_PRESENT]: {
                id: Fields.FURNITURE_DISHES_IS_PRESENT,
                value: true,
            },
        },
        network: {
            updateManagerFlatQuestionnaireStatus: RequestStatus.LOADED,
        },
    },
    managerFlat: {
        flat: {
            flatId: 'ffc21a950bf247818405d91537154696' as FlatId,
            status: FlatStatus.CONFIRMED,
            flatInfo: {
                entrance: 8,
                floor: 8,
                intercom: {
                    code: '88',
                },
                flatType: FlatInfoFlatType.APARTMENTS,
                rooms: FlatInfoFlatRooms.TWO,
                area: 88,
                desiredRentPrice: '8888800',
            },
        },
        flatExcerptsRequests: [],
    },
};

import { DeepPartial } from 'utility-types';

import dayjs from '@realty-front/dayjs';

import { RequestStatus } from 'realty-core/types/network';

import { FlatId, FlatShowingType, ShowingId } from 'types/flat';
import { FlatShowingStatus } from 'types/showing';

import { IUniversalStore } from 'view/modules/types';
import { Fields } from 'view/modules/outstaffCallCenterForm/types';
import { initialState as initialFieldsState } from 'view/modules/outstaffCallCenterForm/reducers/fields';
import { initialState as initialNetworkState } from 'view/modules/outstaffCallCenterForm/reducers/network';

export const store: DeepPartial<IUniversalStore> = {
    spa: {
        status: RequestStatus.LOADED,
    },
    outstaffCallCenterForm: {
        fields: initialFieldsState,
        network: initialNetworkState,
    },
    config: {
        isMobile: '',
    },
};

export const skeletonStore: DeepPartial<IUniversalStore> = {
    ...store,
    spa: {
        status: RequestStatus.PENDING,
    },
};

export const storeWithAddress: DeepPartial<IUniversalStore> = {
    ...store,
    config: {
        ...store.config,
        serverTimeStamp: dayjs('2021-12-24 12:00').valueOf(),
    },
    outstaffCallCenterForm: {
        fields: {
            ...store.outstaffCallCenterForm?.fields,
            [Fields.ADDRESS]: {
                id: Fields.ADDRESS,
                unified: 'Россия, Санкт-Петербург, Пискаревский пр-кт',
                value: {
                    address: 'Россия, Санкт-Петербург, Пискаревский пр-кт',
                },
            },
        },
    },
};

export const mobileStore: DeepPartial<IUniversalStore> = {
    ...storeWithAddress,
    config: {
        isMobile: 'iPhone',
    },
};

export const questionnaire = {
    flat: {
        floor: 4,
        area: 666,
    },
    payments: {
        adValue: 7733300,
    },
    yandexRentConditions: {
        onlyOnlineShowings: true,
    },
};

export const storeWithFilledForm: DeepPartial<IUniversalStore> = {
    ...store,
    outstaffCallCenterForm: {
        fields: {
            [Fields.ADDRESS]: {
                id: Fields.ADDRESS,
                unified: 'Россия, Санкт-Петербург, Пискаревский пр-кт',
                value: {
                    address: 'Россия, Санкт-Петербург, Пискаревский пр-кт',
                },
            },
            [Fields.FLAT_ID]: {
                id: Fields.FLAT_ID,
                value: '1',
            },
            [Fields.TENANT_NAME]: {
                id: Fields.TENANT_NAME,
                value: 'Иван',
            },
            [Fields.TENANT_PHONE]: {
                id: Fields.TENANT_PHONE,
                value: '+79115559922',
            },
            [Fields.NUMBER_OF_ADULTS]: {
                id: Fields.NUMBER_OF_ADULTS,
                value: 3,
            },
            [Fields.NUMBER_OF_CHILDREN]: {
                id: Fields.NUMBER_OF_CHILDREN,
                value: 0,
            },
            [Fields.WITH_PETS]: {
                id: Fields.WITH_PETS,
                value: true,
            },
            [Fields.DESCRIPTION_OF_PETS]: {
                id: Fields.DESCRIPTION_OF_PETS,
                value: 'Кинг-Конг',
            },
            [Fields.SHOWING_TYPE]: {
                id: Fields.SHOWING_TYPE,
                value: 'OFFLINE',
            },
            [Fields.SHOWING_DATE]: {
                id: Fields.SHOWING_DATE,
                value: 'в понедельник после восьми',
            },
            [Fields.ONLINE_SHOWING_DATE]: {
                id: Fields.ONLINE_SHOWING_DATE,
                value: '',
            },
            [Fields.ONLINE_SHOWING_SLOT]: {
                id: Fields.ONLINE_SHOWING_SLOT,
            },
            [Fields.TENANT_QUESTION]: {
                id: Fields.TENANT_QUESTION,
                value: '',
            },
            [Fields.TENANT_COMMENT]: {
                id: Fields.TENANT_COMMENT,
                value: '',
            },
            [Fields.ESTIMATED_RENT_DURATION]: {
                id: Fields.ESTIMATED_RENT_DURATION,
                value: undefined,
            },
        },
        network: {
            flatsSuggestStatus: RequestStatus.LOADED,
            flatsSuggest: [
                {
                    address: 'Россия, Санкт-Петербург, Пискаревский пр-кт, 1',
                    flatId: '1' as FlatId,
                    questionnaire,
                },
                {
                    address: 'Россия, Санкт-Петербург, Пискаревский пр-кт, 2',
                    flatId: '2' as FlatId,
                    questionnaire,
                },
            ],
            createFlatShowingStatus: RequestStatus.LOADED,
        },
    },
};

export const storeWithALotOfShowings: DeepPartial<IUniversalStore> = {
    ...store,
    outstaffCallCenterForm: {
        fields: {
            ...store.outstaffCallCenterForm?.fields,
            [Fields.ADDRESS]: {
                id: Fields.ADDRESS,
                unified: 'Россия, Санкт-Петербург, Пискаревский пр-кт',
                value: {
                    address: 'Россия, Санкт-Петербург, Пискаревский пр-кт',
                },
            },
        },
        network: {
            flatsSuggestStatus: RequestStatus.LOADED,
            flatsSuggest: [
                {
                    address: 'Россия, Санкт-Петербург, Пискаревский пр-кт, 1',
                    flatId: '1' as FlatId,
                    questionnaire,
                    showings: {
                        showingsOnline: [
                            {
                                showingId: '2214c544ea09b5' as ShowingId,
                                status: FlatShowingStatus.APPLICATION_PROCESSED,
                                showingType: FlatShowingType.ONLINE,
                            },
                            {
                                showingId: '2214c544ea09b5' as ShowingId,
                                status: FlatShowingStatus.SHOWING_APPOINTED,
                                showingType: FlatShowingType.ONLINE,
                            },
                        ],
                        showingsOffline: [
                            {
                                showingId: '2214c544ea09b5' as ShowingId,
                                status: FlatShowingStatus.CONFIRMED_BY_TENANT,
                                showingType: FlatShowingType.OFFLINE,
                            },
                            {
                                showingId: '2214c544ea09b5' as ShowingId,
                                status: FlatShowingStatus.QUESTIONNAIRE_RECEIVED,
                                showingType: FlatShowingType.OFFLINE,
                            },
                        ],
                        withoutShowing: [
                            {
                                showingId: '2214c544ea09b5' as ShowingId,
                                status: FlatShowingStatus.QUESTIONNAIRE_RECEIVED,
                                showingType: FlatShowingType.WITHOUT_SHOWING,
                            },
                        ],
                    },
                },
            ],
            createFlatShowingStatus: RequestStatus.LOADED,
        },
    },
};

export const storeWithPreFinalShowings: DeepPartial<IUniversalStore> = {
    ...store,
    outstaffCallCenterForm: {
        fields: {
            ...store.outstaffCallCenterForm?.fields,
            [Fields.ADDRESS]: {
                id: Fields.ADDRESS,
                unified: 'Россия, Санкт-Петербург, Пискаревский пр-кт',
                value: {
                    address: 'Россия, Санкт-Петербург, Пискаревский пр-кт',
                },
            },
        },
        network: {
            flatsSuggestStatus: RequestStatus.LOADED,
            flatsSuggest: [
                {
                    address: 'Россия, Санкт-Петербург, Пискаревский пр-кт, 1',
                    flatId: '1' as FlatId,
                    questionnaire,
                    showings: {
                        withoutShowing: [
                            {
                                showingId: '2214c544ea09b5' as ShowingId,
                                status: FlatShowingStatus.CONFIRMED_BY_OWNER,
                                showingType: FlatShowingType.WITHOUT_SHOWING,
                            },
                        ],
                    },
                },
            ],
            createFlatShowingStatus: RequestStatus.LOADED,
        },
    },
};

export const storeWithNoShowing: DeepPartial<IUniversalStore> = {
    ...store,
    outstaffCallCenterForm: {
        fields: {
            ...store.outstaffCallCenterForm?.fields,
            [Fields.ADDRESS]: {
                id: Fields.ADDRESS,
                unified: 'Россия, Санкт-Петербург, Пискаревский пр-кт',
                value: {
                    address: 'Россия, Санкт-Петербург, Пискаревский пр-кт',
                },
            },
        },
        network: {
            flatsSuggestStatus: RequestStatus.LOADED,
            flatsSuggest: [
                {
                    address: 'Россия, Санкт-Петербург, Пискаревский пр-кт, 1',
                    flatId: '1' as FlatId,
                    questionnaire,
                    showings: {},
                },
            ],
            createFlatShowingStatus: RequestStatus.LOADED,
        },
    },
};

export const onlyOnlineShowingsStore: DeepPartial<IUniversalStore> = {
    ...store,
    outstaffCallCenterForm: {
        fields: {
            ...store.outstaffCallCenterForm?.fields,
            [Fields.ADDRESS]: {
                id: Fields.ADDRESS,
                unified: 'Россия, Санкт-Петербург, Пискаревский пр-кт',
                value: {
                    address: 'Россия, Санкт-Петербург, Пискаревский пр-кт',
                },
            },
        },
        network: {
            flatsSuggestStatus: RequestStatus.LOADED,
            flatsSuggest: [
                {
                    address: 'Россия, Санкт-Петербург, Пискаревский пр-кт, 1',
                    flatId: '1' as FlatId,
                    questionnaire,
                    showings: {},
                },
            ],
            createFlatShowingStatus: RequestStatus.LOADED,
        },
    },
};

import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { Flavor } from 'realty-core/types/utils';

import { OwnerType } from 'realty-core/view/react/common/types/egrnPaidReport';

import { IUniversalStore } from 'view/modules/types';
import { ExcerptStatus, FlatStatus, RightMovementsExcerptStatus } from 'types/flat';

export const skeletonStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.PENDING,
    },
    managerFlatExcerpts: {
        network: {
            excerptRequestStatus: RequestStatus.LOADED,
        },
    },
    managerFlat: {
        flat: {
            flatId: '5b95aaebd56a41748e493b616ff2ec31' as Flavor<string, 'FlatID'>,
            address: {
                address: 'г Москва, ул Фрязевская, д 11 к 2',
                flatNumber: '184',
            },
            status: FlatStatus.RENTED,
        },
        flatExcerptsRequests: [],
    },
};

export const emptyStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    managerFlatExcerpts: {
        network: {
            excerptRequestStatus: RequestStatus.LOADED,
        },
    },
    managerFlat: {
        flat: {
            flatId: '5b95aaebd56a41748e493b616ff2ec31' as Flavor<string, 'FlatID'>,
            address: {
                address: 'г Москва, ул Фрязевская, д 11 к 2',
                flatNumber: '184',
            },
            status: FlatStatus.RENTED,
        },
        flatExcerptsRequests: [],
    },
};

export const notActiveFlatStatusStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    managerFlatExcerpts: {
        network: {
            excerptRequestStatus: RequestStatus.LOADED,
        },
    },
    managerFlat: {
        flat: {
            flatId: '5b95aaebd56a41748e493b616ff2ec31' as Flavor<string, 'FlatID'>,
            address: {
                address: 'г Москва, ул Фрязевская, д 11 к 2',
                flatNumber: '184',
            },
            status: FlatStatus.DENIED,
        },
        flatExcerptsRequests: [],
    },
};

export const waitingFirstExcerptStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    managerFlatExcerpts: {
        network: {
            excerptRequestStatus: RequestStatus.LOADED,
        },
    },
    managerFlat: {
        flat: {
            flatId: '5b95aaebd56a41748e493b616ff2ec31' as Flavor<string, 'FlatID'>,
            address: {
                address: 'г Москва, ул Фрязевская, д 11 к 2',
                flatNumber: '184',
            },
            status: FlatStatus.RENTED,
        },
        flatExcerptsRequests: [
            {
                initialTime: '2021-04-22T15:12:39.841Z',
                status: ExcerptStatus.WAITING_FOR_EXCERPTS,
                evaluatedObjectInfo: {
                    unifiedAddress: 'Россия, Москва, Фрязевская улица, 11к2',
                    floor: '11',
                    area: 35.2,
                    cadastralNumber: '77:03:0006021:5342',
                    subjectFederationId: 1,
                    rrAddress: 'Москва, ул Фрязевская, д 11, корп 2, кв 184',
                    unifiedRrAddress: 'Россия, Москва, Фрязевская улица, 11к2',
                },
                rightMovementsExcerpt: {
                    status: RightMovementsExcerptStatus.IN_PROGRESS,
                },
            },
        ],
    },
};

export const addressErrorStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    managerFlatExcerpts: {
        network: {
            excerptRequestStatus: RequestStatus.LOADED,
        },
    },
    managerFlat: {
        flat: {
            flatId: '5b95aaebd56a41748e493b616ff2ec31' as Flavor<string, 'FlatID'>,
            address: {
                address: 'г Москва, ул Фрязевская, д 11 к 2',
                flatNumber: '184',
            },
            status: FlatStatus.RENTED,
        },
        flatExcerptsRequests: [
            {
                initialTime: '2021-04-22T15:12:39.841Z',
                status: ExcerptStatus.ADDRESS_INFO_CALCULATION_ERROR,
                evaluatedObjectInfo: {
                    unifiedAddress: 'Россия, Москва, Фрязевская улица, 11к2',
                    floor: '11',
                    area: 35.2,
                    cadastralNumber: '77:03:0006021:5342',
                    subjectFederationId: 1,
                    rrAddress: 'Москва, ул Фрязевская, д 11, корп 2, кв 184',
                    unifiedRrAddress: 'Россия, Москва, Фрязевская улица, 11к2',
                },
                rightMovementsExcerpt: {
                    status: RightMovementsExcerptStatus.ERROR,
                },
            },
        ],
    },
};

export const excerptErrorStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    managerFlatExcerpts: {
        network: {
            excerptRequestStatus: RequestStatus.LOADED,
        },
    },
    managerFlat: {
        flat: {
            flatId: '5b95aaebd56a41748e493b616ff2ec31' as Flavor<string, 'FlatID'>,
            address: {
                address: 'г Москва, ул Фрязевская, д 11 к 2',
                flatNumber: '184',
            },
            status: FlatStatus.RENTED,
        },
        flatExcerptsRequests: [
            {
                initialTime: '2021-04-22T15:12:39.841Z',
                status: ExcerptStatus.EXCERPTS_ERROR,
                evaluatedObjectInfo: {
                    unifiedAddress: 'Россия, Москва, Фрязевская улица, 11к2',
                    floor: '11',
                    area: 35.2,
                    cadastralNumber: '77:03:0006021:5342',
                    subjectFederationId: 1,
                    rrAddress: 'Москва, ул Фрязевская, д 11, корп 2, кв 184',
                    unifiedRrAddress: 'Россия, Москва, Фрязевская улица, 11к2',
                },
                rightMovementsExcerpt: {
                    status: RightMovementsExcerptStatus.ERROR,
                },
            },
        ],
    },
};

export const excerptReadyStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    managerFlatExcerpts: {
        network: {
            excerptRequestStatus: RequestStatus.LOADED,
        },
    },
    managerFlat: {
        flat: {
            flatId: '5b95aaebd56a41748e493b616ff2ec31' as Flavor<string, 'FlatID'>,
            address: {
                address: 'г Москва, ул Фрязевская, д 11 к 2',
                flatNumber: '184',
            },
            status: FlatStatus.RENTED,
        },
        flatExcerptsRequests: [
            {
                initialTime: '2021-04-23T08:18:16.684Z',
                status: ExcerptStatus.EXCERPTS_READY,
                evaluatedObjectInfo: {
                    unifiedAddress: 'Россия, Санкт-Петербург, проспект Энергетиков, 30к1',
                    floor: '3',
                    area: 44.3,
                    cadastralNumber: '78:11:0006040:3225',
                    subjectFederationId: 10174,
                    rrAddress: 'Санкт-Петербург, пр-кт Энергетиков, д 30, корп 1, литера А, кв 77',
                    unifiedRrAddress: 'Россия, Санкт-Петербург, проспект Энергетиков, 30к1',
                },
                rightMovementsExcerpt: {
                    excerptId: '02b04df9ea4e4091b18782b17719f230',
                    status: RightMovementsExcerptStatus.READY,
                    currentOwners: [
                        {
                            type: OwnerType.NATURAL_PERSON,
                            name: 'Владимир',
                            person: {
                                name: 'Владимир',
                                surname: 'Айзиков',
                                patronymic: 'Ханунович',
                            },
                        },
                    ],
                    number: '99/2021/427160124',
                    date: '2021-10-27T00:00:00Z',
                },
            },
        ],
    },
};

export const excerptReadyWithAtticStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    managerFlatExcerpts: {
        network: {
            excerptRequestStatus: RequestStatus.LOADED,
        },
    },
    managerFlat: {
        flat: {
            flatId: '5b95aaebd56a41748e493b616ff2ec31' as Flavor<string, 'FlatID'>,
            address: {
                address: 'г Москва, ул Фрязевская, д 11 к 2',
                flatNumber: '184',
            },
            status: FlatStatus.RENTED,
        },
        flatExcerptsRequests: [
            {
                initialTime: '2021-04-23T08:18:16.684Z',
                status: ExcerptStatus.EXCERPTS_READY,
                evaluatedObjectInfo: {
                    unifiedAddress: 'Россия, Санкт-Петербург, проспект Энергетиков, 30к1',
                    floor: '5, мансарда',
                    area: 44.3,
                    cadastralNumber: '78:11:0006040:3225',
                    subjectFederationId: 10174,
                    rrAddress: 'Санкт-Петербург, пр-кт Энергетиков, д 30, корп 1, литера А, кв 77',
                    unifiedRrAddress: 'Россия, Санкт-Петербург, проспект Энергетиков, 30к1',
                },
                rightMovementsExcerpt: {
                    excerptId: '02b04df9ea4e4091b18782b17719f230',
                    status: RightMovementsExcerptStatus.READY,
                    currentOwners: [
                        {
                            type: OwnerType.NATURAL_PERSON,
                            name: 'Владимир',
                            person: {
                                name: 'Владимир',
                                surname: 'Айзиков',
                                patronymic: 'Ханунович',
                            },
                        },
                    ],
                    number: '99/2021/427160124',
                    date: '2021-10-27T00:00:00Z',
                },
            },
        ],
    },
};

export const withoutDataStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    managerFlatExcerpts: {
        network: {
            excerptRequestStatus: RequestStatus.LOADED,
        },
    },
    managerFlat: {
        flat: {
            flatId: '5b95aaebd56a41748e493b616ff2ec31' as Flavor<string, 'FlatID'>,
            address: {
                address: 'г Москва, ул Фрязевская, д 11 к 2',
                flatNumber: '184',
            },
            status: FlatStatus.RENTED,
        },
        flatExcerptsRequests: [
            {
                initialTime: '2021-04-23T08:18:16.684Z',
                status: ExcerptStatus.EXCERPTS_READY,
                evaluatedObjectInfo: {
                    unifiedAddress: 'Россия, Санкт-Петербург, проспект Энергетиков, 30к1',
                    area: 44.3,
                    cadastralNumber: '78:11:0006040:3225',
                    subjectFederationId: 10174,
                    unifiedRrAddress: 'Россия, Санкт-Петербург, проспект Энергетиков, 30к1',
                },
                rightMovementsExcerpt: {
                    excerptId: '02b04df9ea4e4091b18782b17719f230',
                    status: RightMovementsExcerptStatus.READY,
                    currentOwners: [
                        {
                            type: OwnerType.NATURAL_PERSON,
                            name: 'Владимир',
                            person: {
                                name: 'Владимир',
                                surname: 'Айзиков',
                                patronymic: 'Ханунович',
                            },
                        },
                    ],
                    number: '99/2021/427160124',
                    date: '2021-10-27T00:00:00Z',
                },
            },
        ],
    },
};

export const withoutOwnerStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    managerFlatExcerpts: {
        network: {
            excerptRequestStatus: RequestStatus.LOADED,
        },
    },
    managerFlat: {
        flat: {
            flatId: '5b95aaebd56a41748e493b616ff2ec31' as Flavor<string, 'FlatID'>,
            address: {
                address: 'г Москва, ул Фрязевская, д 11 к 2',
                flatNumber: '184',
            },
            status: FlatStatus.RENTED,
        },
        flatExcerptsRequests: [
            {
                initialTime: '2021-04-23T08:18:16.684Z',
                status: ExcerptStatus.EXCERPTS_READY,
                evaluatedObjectInfo: {
                    unifiedAddress: 'Россия, Санкт-Петербург, проспект Энергетиков, 30к1',
                    floor: '3',
                    area: 44.3,
                    cadastralNumber: '78:11:0006040:3225',
                    subjectFederationId: 10174,
                    rrAddress: 'Санкт-Петербург, пр-кт Энергетиков, д 30, корп 1, литера А, кв 77',
                    unifiedRrAddress: 'Россия, Санкт-Петербург, проспект Энергетиков, 30к1',
                },
                rightMovementsExcerpt: {
                    excerptId: '02b04df9ea4e4091b18782b17719f230',
                    status: RightMovementsExcerptStatus.READY,
                    number: '99/2021/427160124',
                    date: '2021-10-27T00:00:00Z',
                },
            },
        ],
    },
};

export const fewOwnersStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    managerFlatExcerpts: {
        network: {
            excerptRequestStatus: RequestStatus.LOADED,
        },
    },
    managerFlat: {
        flat: {
            flatId: '5b95aaebd56a41748e493b616ff2ec31' as Flavor<string, 'FlatID'>,
            address: {
                address: 'г Москва, ул Фрязевская, д 11 к 2',
                flatNumber: '184',
            },
            status: FlatStatus.RENTED,
        },
        flatExcerptsRequests: [
            {
                initialTime: '2021-04-23T08:18:16.684Z',
                status: ExcerptStatus.EXCERPTS_READY,
                evaluatedObjectInfo: {
                    unifiedAddress: 'Россия, Санкт-Петербург, проспект Энергетиков, 30к1',
                    floor: '3',
                    area: 44.3,
                    cadastralNumber: '78:11:0006040:3225',
                    subjectFederationId: 10174,
                    rrAddress: 'Санкт-Петербург, пр-кт Энергетиков, д 30, корп 1, литера А, кв 77',
                    unifiedRrAddress: 'Россия, Санкт-Петербург, проспект Энергетиков, 30к1',
                },
                rightMovementsExcerpt: {
                    excerptId: '02b04df9ea4e4091b18782b17719f230',
                    status: RightMovementsExcerptStatus.READY,
                    currentOwners: [
                        {
                            type: OwnerType.NATURAL_PERSON,
                            name: 'Владимир',
                            person: {
                                name: 'Владимир',
                                surname: 'Айзиков',
                                patronymic: 'Ханунович',
                            },
                        },
                        {
                            type: OwnerType.NATURAL_PERSON,
                            name: 'Владимир',
                            person: {
                                name: 'Владимир',
                                surname: 'Айзиков',
                                patronymic: 'Ханунович',
                            },
                        },
                        {
                            type: OwnerType.NATURAL_PERSON,
                            name: 'Татьяна',
                            person: {
                                name: 'Татьяна',
                                surname: 'Данилова',
                                patronymic: 'Сергеевна',
                            },
                        },
                    ],
                    number: '99/2021/427160124',
                    date: '2021-10-27T00:00:00Z',
                },
            },
        ],
    },
};

export const fewSuccessExcerptsStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    managerFlatExcerpts: {
        network: {
            excerptRequestStatus: RequestStatus.LOADED,
        },
    },
    managerFlat: {
        flat: {
            flatId: '5b95aaebd56a41748e493b616ff2ec31' as Flavor<string, 'FlatID'>,
            address: {
                address: 'г Москва, ул Фрязевская, д 11 к 2',
                flatNumber: '184',
            },
            status: FlatStatus.RENTED,
        },
        flatExcerptsRequests: [
            {
                initialTime: '2021-04-21T09:06:23.148Z',
                status: ExcerptStatus.EXCERPTS_READY,
                evaluatedObjectInfo: {
                    unifiedAddress: 'Россия, Санкт-Петербург, улица Грибалёвой, 7к1',
                    floor: '5',
                    area: 62.4,
                    cadastralNumber: '78:36:0005121:2439',
                    subjectFederationId: 10174,
                    rrAddress: 'Санкт-Петербург, ул Грибалёвой, д 7, корп 1, строен 1, кв 99',
                    unifiedRrAddress: 'Россия, Санкт-Петербург, улица Грибалёвой, 7к1',
                },
                rightMovementsExcerpt: {
                    excerptId: 'aa33226f59ad48129aeccc70b8d9a114',
                    status: RightMovementsExcerptStatus.READY,
                    currentOwners: [
                        {
                            type: OwnerType.NATURAL_PERSON,
                            name: 'Валерия',
                            person: {
                                name: 'Валерия',
                                surname: 'Бебешко',
                                patronymic: 'Максимовна',
                            },
                        },
                    ],
                    number: '99/2021/427160124',
                    date: '2021-10-27T00:00:00Z',
                },
            },
            {
                initialTime: '2021-04-22T09:26:29.495Z',
                status: ExcerptStatus.EXCERPTS_READY,
                evaluatedObjectInfo: {
                    unifiedAddress: 'Россия, Санкт-Петербург, улица Грибалёвой, 7к1',
                    floor: '5',
                    area: 62.4,
                    cadastralNumber: '78:36:0005121:2439',
                    subjectFederationId: 10174,
                    rrAddress: 'Санкт-Петербург, ул Грибалёвой, д 7, корп 1, строен 1, кв 99',
                    unifiedRrAddress: 'Россия, Санкт-Петербург, улица Грибалёвой, 7к1',
                },
                rightMovementsExcerpt: {
                    excerptId: 'aa33226f59ad48129aeccc70b8d9a114',
                    status: RightMovementsExcerptStatus.READY,
                    currentOwners: [
                        {
                            type: OwnerType.NATURAL_PERSON,
                            name: 'Валерия',
                            person: {
                                name: 'Валерия',
                                surname: 'Бебешко',
                                patronymic: 'Максимовна',
                            },
                        },
                    ],
                    number: '99/2021/427160124',
                    date: '2021-10-27T00:00:00Z',
                },
            },
            {
                initialTime: '2021-04-22T10:38:47.022Z',
                status: ExcerptStatus.EXCERPTS_READY,
                evaluatedObjectInfo: {
                    unifiedAddress: 'Россия, Санкт-Петербург, улица Грибалёвой, 7к1',
                    floor: '5',
                    area: 62.4,
                    cadastralNumber: '78:36:0005121:2439',
                    subjectFederationId: 10174,
                    rrAddress: 'Санкт-Петербург, ул Грибалёвой, д 7, корп 1, строен 1, кв 99',
                    unifiedRrAddress: 'Россия, Санкт-Петербург, улица Грибалёвой, 7к1',
                },
                rightMovementsExcerpt: {
                    excerptId: 'aa33226f59ad48129aeccc70b8d9a114',
                    status: RightMovementsExcerptStatus.READY,
                    currentOwners: [
                        {
                            type: OwnerType.NATURAL_PERSON,
                            name: 'Валерия',
                            person: {
                                name: 'Валерия',
                                surname: 'Бебешко',
                                patronymic: 'Максимовна',
                            },
                        },
                    ],
                    number: '99/2021/427160124',
                    date: '2021-10-27T00:00:00Z',
                },
            },
        ],
    },
};

export const fewExcerptsWithErrorStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    managerFlatExcerpts: {
        network: {
            excerptRequestStatus: RequestStatus.LOADED,
        },
    },
    managerFlat: {
        flat: {
            flatId: '5b95aaebd56a41748e493b616ff2ec31' as Flavor<string, 'FlatID'>,
            address: {
                address: 'г Москва, ул Фрязевская, д 11 к 2',
                flatNumber: '184',
            },
            status: FlatStatus.RENTED,
        },
        flatExcerptsRequests: [
            {
                initialTime: '2021-04-21T09:06:23.148Z',
                status: ExcerptStatus.EXCERPTS_READY,
                evaluatedObjectInfo: {
                    unifiedAddress: 'Россия, Санкт-Петербург, улица Грибалёвой, 7к1',
                    floor: '5',
                    area: 62.4,
                    cadastralNumber: '78:36:0005121:2439',
                    subjectFederationId: 10174,
                    rrAddress: 'Санкт-Петербург, ул Грибалёвой, д 7, корп 1, строен 1, кв 99',
                    unifiedRrAddress: 'Россия, Санкт-Петербург, улица Грибалёвой, 7к1',
                },
                rightMovementsExcerpt: {
                    excerptId: 'aa33226f59ad48129aeccc70b8d9a114',
                    status: RightMovementsExcerptStatus.READY,
                    currentOwners: [
                        {
                            type: OwnerType.NATURAL_PERSON,
                            name: 'Валерия',
                            person: {
                                name: 'Валерия',
                                surname: 'Бебешко',
                                patronymic: 'Максимовна',
                            },
                        },
                    ],
                    number: '99/2021/427160124',
                    date: '2021-10-27T00:00:00Z',
                },
            },
            {
                initialTime: '2021-04-22T09:26:29.495Z',
                status: ExcerptStatus.ADDRESS_INFO_CALCULATION_ERROR,
                evaluatedObjectInfo: {
                    unifiedAddress: 'Россия, Санкт-Петербург, улица Грибалёвой, 7к1',
                    floor: '5',
                    area: 62.4,
                    cadastralNumber: '78:36:0005121:2439',
                    subjectFederationId: 10174,
                    rrAddress: 'Санкт-Петербург, ул Грибалёвой, д 7, корп 1, строен 1, кв 99',
                    unifiedRrAddress: 'Россия, Санкт-Петербург, улица Грибалёвой, 7к1',
                },
                rightMovementsExcerpt: {
                    excerptId: 'aa33226f59ad48129aeccc70b8d9a114',
                    status: RightMovementsExcerptStatus.ERROR,
                },
            },
            {
                initialTime: '2021-04-22T10:38:47.022Z',
                status: ExcerptStatus.EXCERPTS_READY,
                evaluatedObjectInfo: {
                    unifiedAddress: 'Россия, Санкт-Петербург, улица Грибалёвой, 7к1',
                    floor: '5',
                    area: 62.4,
                    cadastralNumber: '78:36:0005121:2439',
                    subjectFederationId: 10174,
                    rrAddress: 'Санкт-Петербург, ул Грибалёвой, д 7, корп 1, строен 1, кв 99',
                    unifiedRrAddress: 'Россия, Санкт-Петербург, улица Грибалёвой, 7к1',
                },
                rightMovementsExcerpt: {
                    excerptId: 'aa33226f59ad48129aeccc70b8d9a114',
                    status: RightMovementsExcerptStatus.READY,
                    currentOwners: [
                        {
                            type: OwnerType.NATURAL_PERSON,
                            name: 'Валерия',
                            person: {
                                name: 'Валерия',
                                surname: 'Бебешко',
                                patronymic: 'Максимовна',
                            },
                        },
                    ],
                    number: '99/2021/427160124',
                    date: '2021-10-27T00:00:00Z',
                },
            },
        ],
    },
};

export const fewExcerptsWithWaitingStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    managerFlatExcerpts: {
        network: {
            excerptRequestStatus: RequestStatus.LOADED,
        },
    },
    managerFlat: {
        flat: {
            flatId: '5b95aaebd56a41748e493b616ff2ec31' as Flavor<string, 'FlatID'>,
            address: {
                address: 'г Москва, ул Фрязевская, д 11 к 2',
                flatNumber: '184',
            },
            status: FlatStatus.RENTED,
        },
        flatExcerptsRequests: [
            {
                initialTime: '2021-04-21T09:06:23.148Z',
                status: ExcerptStatus.EXCERPTS_READY,
                evaluatedObjectInfo: {
                    unifiedAddress: 'Россия, Санкт-Петербург, улица Грибалёвой, 7к1',
                    floor: '5',
                    area: 62.4,
                    cadastralNumber: '78:36:0005121:2439',
                    subjectFederationId: 10174,
                    rrAddress: 'Санкт-Петербург, ул Грибалёвой, д 7, корп 1, строен 1, кв 99',
                    unifiedRrAddress: 'Россия, Санкт-Петербург, улица Грибалёвой, 7к1',
                },
                rightMovementsExcerpt: {
                    excerptId: 'aa33226f59ad48129aeccc70b8d9a114',
                    status: RightMovementsExcerptStatus.READY,
                    currentOwners: [
                        {
                            type: OwnerType.NATURAL_PERSON,
                            name: 'Валерия',
                            person: {
                                name: 'Валерия',
                                surname: 'Бебешко',
                                patronymic: 'Максимовна',
                            },
                        },
                    ],
                    number: '99/2021/427160124',
                    date: '2021-10-27T00:00:00Z',
                },
            },
            {
                initialTime: '2021-04-22T09:26:29.495Z',
                status: ExcerptStatus.EXCERPTS_READY,
                evaluatedObjectInfo: {
                    unifiedAddress: 'Россия, Санкт-Петербург, улица Грибалёвой, 7к1',
                    floor: '5',
                    area: 62.4,
                    cadastralNumber: '78:36:0005121:2439',
                    subjectFederationId: 10174,
                    rrAddress: 'Санкт-Петербург, ул Грибалёвой, д 7, корп 1, строен 1, кв 99',
                    unifiedRrAddress: 'Россия, Санкт-Петербург, улица Грибалёвой, 7к1',
                },
                rightMovementsExcerpt: {
                    excerptId: 'aa33226f59ad48129aeccc70b8d9a114',
                    status: RightMovementsExcerptStatus.READY,
                    currentOwners: [
                        {
                            type: OwnerType.NATURAL_PERSON,
                            name: 'Валерия',
                            person: {
                                name: 'Валерия',
                                surname: 'Бебешко',
                                patronymic: 'Максимовна',
                            },
                        },
                    ],
                    number: '99/2021/427160124',
                    date: '2021-10-27T00:00:00Z',
                },
            },
            {
                initialTime: '2021-04-22T10:38:47.022Z',
                status: ExcerptStatus.EXCERPTS_READY,
                evaluatedObjectInfo: {
                    unifiedAddress: 'Россия, Санкт-Петербург, улица Грибалёвой, 7к1',
                    floor: '5',
                    area: 62.4,
                    cadastralNumber: '78:36:0005121:2439',
                    subjectFederationId: 10174,
                    rrAddress: 'Санкт-Петербург, ул Грибалёвой, д 7, корп 1, строен 1, кв 99',
                    unifiedRrAddress: 'Россия, Санкт-Петербург, улица Грибалёвой, 7к1',
                },
                rightMovementsExcerpt: {
                    excerptId: 'aa33226f59ad48129aeccc70b8d9a114',
                    status: RightMovementsExcerptStatus.READY,
                    currentOwners: [
                        {
                            type: OwnerType.NATURAL_PERSON,
                            name: 'Валерия',
                            person: {
                                name: 'Валерия',
                                surname: 'Бебешко',
                                patronymic: 'Максимовна',
                            },
                        },
                    ],
                    number: '99/2021/427160124',
                    date: '2021-10-27T00:00:00Z',
                },
            },
            {
                initialTime: '2021-04-23T09:06:23.148Z',
                status: ExcerptStatus.WAITING_FOR_EXCERPTS,
                evaluatedObjectInfo: {
                    unifiedAddress: 'Россия, Санкт-Петербург, улица Грибалёвой, 7к1',
                    floor: '5',
                    area: 62.4,
                    cadastralNumber: '78:36:0005121:2439',
                    subjectFederationId: 10174,
                    rrAddress: 'Санкт-Петербург, ул Грибалёвой, д 7, корп 1, строен 1, кв 99',
                    unifiedRrAddress: 'Россия, Санкт-Петербург, улица Грибалёвой, 7к1',
                },
                rightMovementsExcerpt: {
                    status: RightMovementsExcerptStatus.IN_PROGRESS,
                },
            },
        ],
    },
};

export const fewSuccessExcerptsWithDifferentAddressesStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    managerFlatExcerpts: {
        network: {
            excerptRequestStatus: RequestStatus.LOADED,
        },
    },
    managerFlat: {
        flat: {
            flatId: '5b95aaebd56a41748e493b616ff2ec31' as Flavor<string, 'FlatID'>,
            address: {
                address: 'г Москва, ул Фрязевская, д 11 к 2',
                flatNumber: '184',
            },
            status: FlatStatus.RENTED,
        },
        flatExcerptsRequests: [
            {
                initialTime: '2021-04-19T09:06:23.148Z',
                status: ExcerptStatus.EXCERPTS_READY,
                evaluatedObjectInfo: {
                    unifiedAddress: 'Россия, Санкт-Петербург, улица Грибалёвой, 7к1',
                    floor: '5',
                    area: 62.4,
                    cadastralNumber: '78:36:0005121:2439',
                    subjectFederationId: 10174,
                    rrAddress: 'Санкт-Петербург, ул Грибалёвой, д 7, корп 1, строен 1, кв 99',
                    unifiedRrAddress: 'Россия, Санкт-Петербург, улица Грибалёвой, 7к1',
                },
                rightMovementsExcerpt: {
                    excerptId: 'aa33226f59ad48129aeccc70b8d9a114',
                    status: RightMovementsExcerptStatus.READY,
                    currentOwners: [
                        {
                            type: OwnerType.NATURAL_PERSON,
                            name: 'Валерия',
                            person: {
                                name: 'Валерия',
                                surname: 'Бебешко',
                                patronymic: 'Максимовна',
                            },
                        },
                    ],
                    number: '99/2021/427160124',
                    date: '2021-10-27T00:00:00Z',
                },
            },
            {
                initialTime: '2021-04-20T09:06:23.148Z',
                status: ExcerptStatus.EXCERPTS_READY,
                evaluatedObjectInfo: {
                    unifiedAddress: 'Россия, Москва, Фрязевская улица, 11к2',
                    floor: '11',
                    area: 35.2,
                    cadastralNumber: '77:03:0006021:5342',
                    subjectFederationId: 1,
                    rrAddress: 'Москва, ул Фрязевская, д 11, корп 2, кв 184',
                    unifiedRrAddress: 'Россия, Москва, Фрязевская улица, 11к2',
                },
                rightMovementsExcerpt: {
                    excerptId: 'd8604ff54bac480a8433893705c2f628',
                    status: RightMovementsExcerptStatus.READY,
                    currentOwners: [
                        {
                            type: OwnerType.NATURAL_PERSON,
                            name: 'Елена',
                            person: {
                                name: 'Елена',
                                surname: 'Черемхина',
                                patronymic: 'Анатольевна',
                            },
                        },
                    ],
                    number: '99/2021/427160124',
                    date: '2021-10-27T00:00:00Z',
                },
            },
            {
                initialTime: '2021-04-22T10:38:47.022Z',
                status: ExcerptStatus.EXCERPTS_READY,
                evaluatedObjectInfo: {
                    unifiedAddress: 'Россия, Санкт-Петербург, улица Грибалёвой, 7к1',
                    floor: '5',
                    area: 62.4,
                    cadastralNumber: '78:36:0005121:2439',
                    subjectFederationId: 10174,
                    rrAddress: 'Санкт-Петербург, ул Грибалёвой, д 7, корп 1, строен 1, кв 99',
                    unifiedRrAddress: 'Россия, Санкт-Петербург, улица Грибалёвой, 7к1',
                },
                rightMovementsExcerpt: {
                    excerptId: 'aa33226f59ad48129aeccc70b8d9a114',
                    status: RightMovementsExcerptStatus.READY,
                    currentOwners: [
                        {
                            type: OwnerType.NATURAL_PERSON,
                            name: 'Валерия',
                            person: {
                                name: 'Валерия',
                                surname: 'Бебешко',
                                patronymic: 'Максимовна',
                            },
                        },
                    ],
                    number: '99/2021/427160124',
                    date: '2021-10-27T00:00:00Z',
                },
            },
        ],
    },
};

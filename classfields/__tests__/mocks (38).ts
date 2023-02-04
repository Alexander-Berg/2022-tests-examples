import { AddressInfoStatus } from 'realty-core/view/react/common/types/egrnPaidReport';

export const store = {
    config: {},
    user: {
        isAuth: false,
    },
    payment: {
        EGRNPaidReport: {
            popup: {
                isOpened: false,
            },
            stages: {
                init: {
                    isLoaded: false,
                },
                perform: {
                    isLoaded: false,
                },
                status: {
                    isLoaded: false,
                },
            },
            addressInfo: {},
            paidReport: {},
            currentStageName: 'init',
            isLoading: false,
            hasError: false,
        },
    },
    egrnAddressPurchase: {
        modalVisible: false,
        scrollToTopOnHide: false,
        isLoading: false,
        addressInfo: null,
        error: false,
        showSearchBtn: false,
    },
};

export const addressInfoMock = {
    addressInfoId: '05059047612545389dca7a0e25f44b31',
    userObjectInfo: { address: 'г Санкт-Петербург, ул Маршала Тухачевского, д 23, кв 364', flatNumber: '364' },
    evaluatedObjectInfo: {
        unifiedAddress: 'Россия, Санкт-Петербург, улица Маршала Тухачевского, 23',
        floor: '3',
        area: 51.7,
        cadastralNumber: '78:11:0006078:9239',
        subjectFederationId: 10174,
        rrAddress: 'Санкт-Петербург, ул Маршала Тухачевского, д 23, строен 1, кв 364',
        unifiedRrAddress: 'Россия, Санкт-Петербург, улица Маршала Тухачевского, 23',
    },
    status: AddressInfoStatus.DONE,
    price: {
        base: 410,
        effective: 123,
        isAvailable: true,
        reasons: [],
        modifiers: {},
    },
};

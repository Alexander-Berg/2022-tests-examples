export const storeMock = {
    page: {
        name: 'offer',
    },
    config: {
        cspNonce: 'cspNonce',
        yandexKassaV3ShopId: 123456789,
    },
    user: {
        isAuth: false,
        isJuridical: false,
        defaultEmail: '123@list.ru',
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
};

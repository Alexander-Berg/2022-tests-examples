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
        juridicalEGRNPaidReport: {
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

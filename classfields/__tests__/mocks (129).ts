import { secondaryOffer } from '../../__tests__/stubs/offer';

export { secondaryOffer as offer };

export const stateWithoutVas = {
    offerCard: {
        vas: { placement: {} },
        card: secondaryOffer,
        stats: { total: { offerShow: 1, cardShow: 1, phoneShow: 1, calls: 1 } },
    },
    renewalProblems: {},
    payment: {
        vas: {
            popup: {
                isOpened: false,
            },
            stages: {
                init: {
                    methods: [],
                    isLoaded: false,
                },
                perform: {
                    isLoaded: false,
                },
                status: {
                    isLoaded: false,
                },
            },
            currentStageName: 'init',
            isLoading: false,
            hasError: false,
        },
        wallet: {
            popup: {
                isOpened: false,
            },
            stages: {
                init: {
                    methods: [],
                    isLoaded: true,
                },
                perform: {
                    isLoaded: false,
                },
                status: {
                    isLoaded: false,
                },
            },
            currentStageName: 'init',
            isLoading: false,
            hasError: false,
        },
        bind: {
            popup: {
                isOpened: false,
            },
            stages: {
                init: {
                    methods: [],
                    isLoaded: false,
                },
                perform: {
                    isLoaded: false,
                },
                status: {
                    isLoaded: false,
                },
            },
            currentStageName: 'init',
            isLoading: false,
            hasError: false,
        },
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
            addressInfoId: '',
            paidReport: {},
            currentStageName: 'init',
            isLoading: false,
            hasError: false,
        },
        juridicalEGRNPaidReport: {
            popup: {
                isOpened: false,
                data: {},
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
            currentStageName: 'init',
            isLoading: false,
            hasError: false,
        },
    },
};

export const stateWithVas = {
    offerCard: {
        vas: {
            vasServices: {
                premium: {
                    priceContext: {
                        isAvailable: true,
                        effective: 1049,
                        base: 1049,
                        reasons: [],
                        modifiers: {},
                    },
                    description: {
                        duration: 7,
                        description: 'Описание',
                    },
                    isChangingStatus: false,
                    isChangingNotCancelable: false,
                    isWaitingForDeactivation: false,
                    status: 'active',
                    renewal: {
                        status: 'DISABLED_ACTIVE',
                    },
                    isAppliedFromFeed: false,
                    end: 1630399998664,
                },
                raising: {
                    priceContext: {
                        isAvailable: true,
                        effective: 97,
                        base: 97,
                        reasons: [],
                        modifiers: {},
                    },
                    description: {
                        duration: 1,
                        description: 'Ваше объявление 24 часа будет показываться выше других после блока «Премиум».',
                    },
                    isChangingStatus: false,
                    isChangingNotCancelable: false,
                    isWaitingForDeactivation: false,
                    status: 'active',
                    renewal: {
                        status: 'DISABLED_ACTIVE',
                    },
                    isAppliedFromFeed: false,
                    end: 1629873720000,
                },
                promotion: {
                    priceContext: {
                        isAvailable: true,
                        effective: 349,
                        base: 349,
                        reasons: [],
                        modifiers: {},
                    },
                    description: {
                        duration: 7,
                        description: 'Описание 2',
                    },
                    isChangingStatus: false,
                    isChangingNotCancelable: false,
                    isWaitingForDeactivation: false,
                    status: 'active',
                    renewal: {
                        status: 'DISABLED_ACTIVE',
                    },
                    isAppliedFromFeed: false,
                    end: 1630064051004,
                },
                turboSale: {
                    priceContext: {
                        isAvailable: true,
                        effective: 1499,
                        base: 1499,
                        reasons: [],
                        modifiers: {},
                    },
                    description: {
                        duration: 7,
                        description: 'Описание 3 ',
                    },
                    isChangingStatus: false,
                    isChangingNotCancelable: false,
                    isWaitingForDeactivation: false,
                    status: 'active',
                    renewal: {
                        status: 'ACTIVE',
                    },
                    isAppliedFromFeed: false,
                    end: 1629795198664,
                },
                placement: {
                    priceContext: {
                        isAvailable: true,
                        effective: 57,
                        base: 57,
                        reasons: [],
                        modifiers: {},
                    },
                    description: {
                        duration: 30,
                        description: 'Платное размещение',
                    },
                    isChangingStatus: false,
                    isChangingNotCancelable: false,
                    isWaitingForDeactivation: false,
                    status: 'active',
                    renewal: {
                        status: 'ACTIVE',
                    },
                    isAppliedFromFeed: false,
                    end: 1631361170671,
                },
            },
            placement: {
                paymentRequired: {
                    paid: true,
                },
            },
        },
        card: secondaryOffer,
        stats: { total: { offerShow: 1, cardShow: 1, phoneShow: 1, calls: 1 } },
    },
    renewalProblems: {},
    payment: {
        vas: {
            popup: {
                isOpened: false,
            },
            stages: {
                init: {
                    methods: [],
                    isLoaded: false,
                },
                perform: {
                    isLoaded: false,
                },
                status: {
                    isLoaded: false,
                },
            },
            currentStageName: 'init',
            isLoading: false,
            hasError: false,
        },
        wallet: {
            popup: {
                isOpened: false,
            },
            stages: {
                init: {
                    methods: [],
                    isLoaded: true,
                },
                perform: {
                    isLoaded: false,
                },
                status: {
                    isLoaded: false,
                },
            },
            currentStageName: 'init',
            isLoading: false,
            hasError: false,
        },
        bind: {
            popup: {
                isOpened: false,
            },
            stages: {
                init: {
                    methods: [],
                    isLoaded: false,
                },
                perform: {
                    isLoaded: false,
                },
                status: {
                    isLoaded: false,
                },
            },
            currentStageName: 'init',
            isLoading: false,
            hasError: false,
        },
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
            addressInfoId: '',
            paidReport: {},
            currentStageName: 'init',
            isLoading: false,
            hasError: false,
        },
        juridicalEGRNPaidReport: {
            popup: {
                isOpened: false,
                data: {},
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
            currentStageName: 'init',
            isLoading: false,
            hasError: false,
        },
    },
};

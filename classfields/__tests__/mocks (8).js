import cloneDeep from 'lodash/cloneDeep';

export const offerForm = {
    type: 'SELL',
    category: 'APARTMENT',
    newFlat: false,
    period: 'PER_MONTH',
    redirectPhones: true,
    _draftId: '1987542694763759361',
    currency: 'RUB',
    onlineShowPossible: false,
    description: '',
    balcony: [],
    windowView: [],
    location: {
        address: 'Свердловская набережная, 18АБ',
        coords: [
            59.961684673608914,
            30.39048436035155
        ],
        rgid: 741965,
        hasSites: true,
        isKnown: true,
        country: 225,
        sign: 'c004ea001ded328b76f4b2f8a27dbc97',
        combinedAddress: 'Россия, Санкт-Петербург, Свердловская набережная, 18АБ'
    },
    photo: [
        'https://avatars.mdst.yandex.net/get-realty/2935/add.1595086860749db2dadaff6/orig'
    ],
    videoReviewUrl: '',
    layoutImage: [],
    _draftsCount: 6,
    _startTime: 1595085564736,
    _needToSave: false,
    _errors: {}
};

export const productsWithDiscount = {
    individualCost: {
        total: {
            isAvailable: true,
            effective: 0,
            base: 0,
            reasons: [],
            modifiers: {}
        },
        raising: {
            priceContext: {
                isAvailable: true,
                effective: 87,
                base: 87,
                reasons: [],
                modifiers: {}
            },
            description: {
                duration: 1,
                description: 'Ваше объявление 24 часа будет показываться выше других после блока «Премиум».'
            }
        },
        premium: {
            priceContext: {
                isAvailable: true,
                effective: 0,
                base: 639,
                reasons: [],
                modifiers: {
                    bonusDiscount: {
                        percent: 30,
                        amount: 192
                    },
                    promocode: 'PREMIUM'
                }
            },
            description: {
                duration: 7,
                // eslint-disable-next-line max-len
                description: 'Премиум-объявления показываются\u2028 на первых трёх позициях каждой страницы выдачи и отмечаются специальным значком. Плюс показываются на главной странице.'
            }
        },
        promotion: {
            priceContext: {
                isAvailable: true,
                effective: 189,
                base: 189,
                reasons: [],
                modifiers: {
                    bonusDiscount: {
                        percent: 70,
                        amount: 133
                    }
                }
            },
            description: {
                duration: 7,
                // eslint-disable-next-line max-len
                description: 'Ваше объявление оказывается выше любых бесплатных на страницах поиска в течение 7‑ми дней.'
            }
        },
        turboSale: {
            priceContext: {
                isAvailable: true,
                effective: 1119,
                base: 1119,
                reasons: [],
                modifiers: {
                    bonusDiscount: {
                        percent: 30,
                        amount: 336
                    }
                }
            },
            description: {
                duration: 7,
                // eslint-disable-next-line max-len
                description: 'Включает в себя опции «Премиум», «Продвижение», ежедневное «Поднятие» \u2028в течение недели. Получите в 7 раз больше просмотров и в 3 раза больше звонков!'
            }
        },
        placement: {
            priceContext: {
                isAvailable: true,
                effective: 197,
                base: 197,
                reasons: [],
                modifiers: {}
            },
            description: {
                duration: 30,
                description: 'Платное размещение'
            },
            placement: {
                paymentRequired: {}
            }
        }
    }
};

export const defaultProducts = {
    individualCost: {
        total: {
            isAvailable: true,
            effective: 0,
            base: 0,
            reasons: [],
            modifiers: {}
        },
        raising: {
            priceContext: {
                isAvailable: true,
                effective: 87,
                base: 87,
                reasons: [],
                modifiers: {}
            },
            description: {
                duration: 1,
                description: 'Ваше объявление 24 часа будет показываться выше других после блока «Премиум».'
            }
        },
        premium: {
            priceContext: {
                isAvailable: true,
                effective: 639,
                base: 639,
                reasons: [],
                modifiers: {}
            },
            description: {
                duration: 7,
                // eslint-disable-next-line max-len
                description: 'Премиум-объявления показываются на первых трёх позициях каждой страницы выдачи и отмечаются специальным значком. Плюс показываются на главной странице.'
            }
        },
        promotion: {
            priceContext: {
                isAvailable: true,
                effective: 189,
                base: 189,
                reasons: [],
                modifiers: {}
            },
            description: {
                duration: 7,
                // eslint-disable-next-line max-len
                description: 'Ваше объявление оказывается выше любых бесплатных на страницах поиска в течение 7‑ми дней.'
            }
        },
        turboSale: {
            priceContext: {
                isAvailable: true,
                effective: 1119,
                base: 1119,
                reasons: [],
                modifiers: {}
            },
            description: {
                duration: 7,
                // eslint-disable-next-line max-len
                description: 'Включает в себя опции «Премиум», «Продвижение», ежедневное «Поднятие» в течение недели. Получите в 7 раз больше просмотров и в 3 раза больше звонков!'
            }
        },
        placement: {
            priceContext: {
                isAvailable: true,
                effective: 197,
                base: 197,
                reasons: [],
                modifiers: {
                    bonusDiscount: {
                        percent: 75,
                        amount: 112
                    }
                }
            },
            description: {
                duration: 30,
                description: 'Платное размещение'
            },
            placement: {
                paymentRequired: {}
            }
        }
    }
};

const store = {
    config: {
        timeDelta: 0
    },
    offerForm,
    user: {
        isJuridical: false,
        crc: ''
    },
    vosUserData: {
        ogrn: undefined,
        paymentType: 'NATURAL_PERSON',
        userType: 'ONWER'
    },
    accountForm: {
        userType: 'OWNER',
        ogrn: undefined,
        hasAccount: true
    },
    vasCosts: {
        individualCost: null,
        isFetching: false
    }
};

export const getState = () => cloneDeep(store);

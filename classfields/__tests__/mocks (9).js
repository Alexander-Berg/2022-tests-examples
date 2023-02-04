/**
 * @type {any}
 */
export const products = {
    premium: {
        priceContext: {
            isAvailable: true,
            effective: 359,
            base: 459,
            reasons: [],
            modifiers: {
                money: 100
            }
        },
        description: {
            duration: 7,
            // eslint-disable-next-line max-len
            description: 'Премиум-объявления показываются\u2028 на первых трёх позициях каждой страницы выдачи и отмечаются специальным значком. Плюс показываются на главной странице.'
        },
        status: 'active',
        renewal: {
            status: 'UNAVAILABLE'
        },
        isAppliedFromFeed: false,
        end: 1588582344147,
        renewalError: false
    },
    raising: {
        priceContext: {
            isAvailable: true,
            effective: 0,
            base: 30,
            reasons: [],
            modifiers: {
                money: 30
            }
        },
        description: {
            duration: 1,
            description: 'Ваше объявление 24 часа будет показываться выше других после блока «Премиум».'
        },
        status: 'active',
        renewal: {
            status: 'DISABLED_ACTIVE'
        },
        isAppliedFromFeed: false,
        end: 1588663080000,
        renewalError: false
    },
    promotion: {
        priceContext: {
            isAvailable: true,
            effective: 0,
            base: 139,
            reasons: [],
            modifiers: {
                money: 100,
                promocode: true
            }
        },
        description: {
            duration: 7,
            description: 'Ваше объявление оказывается выше любых бесплатных на страницах поиска в течение 7‑ми дней.'
        },
        status: 'active',
        renewal: {
            status: 'UNAVAILABLE'
        },
        isAppliedFromFeed: false,
        end: 1588582344147,
        renewalError: false
    },
    turboSale: {
        priceContext: {
            isAvailable: true,
            effective: 699,
            base: 799,
            reasons: [],
            modifiers: {
                money: 100
            }
        },
        description: {
            duration: 7,
            // eslint-disable-next-line max-len
            description: 'Включает в себя опции «Премиум», «Продвижение», ежедневное «Поднятие» \u2028в течение недели. Получите в 7 раз больше просмотров и в 3 раза больше звонков!'
        },
        status: 'active',
        renewal: {
            status: 'ACTIVE'
        },
        isAppliedFromFeed: false,
        end: 1588582344147,
        renewalError: false
    },
    placement: {
        priceContext: {
            isAvailable: true,
            effective: 0,
            base: 1,
            reasons: [],
            modifiers: {
                money: 1,
                discount: 80
            }
        },
        description: {
            duration: 30,
            description: 'Платное размещение'
        },
        status: 'active',
        renewal: {
            status: 'ACTIVE'
        },
        isAppliedFromFeed: false,
        end: 1587920739074,
        renewalError: false
    }
};

export const productsWithDiscount = {
    premium: {
        priceContext: {
            isAvailable: true,
            effective: 0,
            base: 329,
            reasons: [],
            modifiers: {
                bonusDiscount: {
                    percent: 30,
                    amount: 99
                },
                promocode: 'PREMIUM'
            }
        },
        description: {
            duration: 7,
            // eslint-disable-next-line max-len
            description: 'Премиум-объявления показываются\u2028 на первых трёх позициях каждой страницы выдачи и отмечаются специальным значком. Плюс показываются на главной странице.'
        },
        isChangingStatus: false,
        isChangingNotCancelable: false,
        isWaitingForDeactivation: false,
        status: 'inactive',
        renewal: {
            status: 'UNAVAILABLE'
        }
    },
    raising: {
        priceContext: {
            isAvailable: true,
            effective: 37,
            base: 37,
            reasons: [],
            modifiers: {}
        },
        description: {
            duration: 1,
            description: 'Ваше объявление 24 часа будет показываться выше других после блока «Премиум».'
        },
        isChangingStatus: false,
        isChangingNotCancelable: false,
        isWaitingForDeactivation: false,
        status: 'active',
        renewal: {
            status: 'ACTIVE'
        },
        isAppliedFromFeed: false,
        end: 1604585926135
    },
    promotion: {
        priceContext: {
            isAvailable: true,
            effective: 29,
            base: 99,
            reasons: [],
            modifiers: {
                bonusDiscount: {
                    percent: 70,
                    amount: 70
                }
            }
        },
        description: {
            duration: 7,
            description: 'Ваше объявление оказывается выше любых бесплатных на страницах поиска в течение 7‑ми дней.'
        },
        isChangingStatus: false,
        isChangingNotCancelable: false,
        isWaitingForDeactivation: false,
        status: 'inactive',
        renewal: {
            status: 'UNAVAILABLE'
        }
    },
    turboSale: {
        priceContext: {
            isAvailable: true,
            effective: 349,
            base: 499,
            reasons: [],
            modifiers: {
                bonusDiscount: {
                    percent: 30,
                    amount: 150
                }
            }
        },
        description: {
            duration: 7,
            // eslint-disable-next-line max-len
            description: 'Включает в себя опции «Премиум», «Продвижение», ежедневное «Поднятие» \u2028в течение недели. Получите в 7 раз больше просмотров и в 3 раза больше звонков!'
        },
        isChangingStatus: false,
        isChangingNotCancelable: false,
        isWaitingForDeactivation: false,
        status: 'inactive',
        renewal: {
            status: 'UNAVAILABLE'
        }
    },
    placement: {
        priceContext: {
            isAvailable: false,
            effective: null,
            base: null,
            reasons: [
                'VST_CALCULATION_ERROR'
            ],
            modifiers: {}
        },
        description: {
            duration: 30,
            description: 'Платное размещение'
        },
        isChangingStatus: false,
        isChangingNotCancelable: false,
        isWaitingForDeactivation: false,
        status: 'inactive',
        renewal: {
            status: 'UNAVAILABLE'
        }
    }
};

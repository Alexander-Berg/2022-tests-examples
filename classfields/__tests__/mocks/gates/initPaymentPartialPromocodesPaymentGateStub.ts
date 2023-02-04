export const initPaymentPartialPromocodesPaymentGateStub = {
    paidReport: {
        paidReportId: 'd406fb4a09314518a4f882092a664e50',
        addressInfoId: '5c554b179d9e4d85a2505e5c0a7c6578',
        address: {
            addressInfoId: '5c554b179d9e4d85a2505e5c0a7c6578',
            userObjectInfo: { offerId: '5437966522810970113' },
            evaluatedObjectInfo: {
                unifiedAddress: 'Россия, Краснодарский край, Анапа, улица Ленина, 180Ак1',
                floor: '8',
                area: 35.2,
                subjectFederationId: 10995,
            },
            status: 'DONE',
        },
        paymentStatus: 'READY_TO_PAY',
        reportStatus: 'NEW',
        reportDate: '2020-10-09T08:48:13.709Z',
        uid: '4029975986',
    },
    payment: {
        purchaseId: '8f6084fd566b4223ab9cf6b900548d36',
        methods: [
            {
                type: 'tiedCard',
                psId: 'YANDEXKASSA_V3',
                id: 'bank_card',
                name: 'Банковская карта',
                properties: {
                    card: { cddPanMask: '555555|4444', brand: 'MASTERCARD', expireYear: '2022', expireMonth: '12' },
                },
                needEmail: true,
            },
            {
                type: 'tiedCard',
                psId: 'YANDEXKASSA_V3',
                id: 'bank_card',
                name: 'Банковская карта',
                properties: {
                    card: { cddPanMask: '555555|4477', brand: 'MASTERCARD', expireYear: '2088', expireMonth: '9' },
                },
                needEmail: true,
            },
            {
                type: 'bankCard',
                psId: 'YANDEXKASSA_V3',
                id: 'bank_card',
                name: 'Банковская карта',
                needEmail: true,
            },
            { type: 'sberbank', psId: 'YANDEXKASSA_V3', id: 'sberbank', name: 'Сбербанк Онлайн', needEmail: true },
            {
                type: 'yooMoney',
                psId: 'YANDEXKASSA_V3',
                id: 'yoo_money',
                name: 'ЮMoney',
                needEmail: true,
            },
        ],
        price: {
            isAvailable: true,
            effective: 86,
            base: 310,
            reasons: [],
            modifiers: { money: 7 },
            availableMoneyFeaturesPrice: 7,
        },
        defaultEmail: 'zagent@yandex.ru',
        promocodes: {},
        preferWalletPayment: true,
    },
};

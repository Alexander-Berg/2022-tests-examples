export const initPaymentWithPreferWalletGateStub = {
    paidReport: {
        paidReportId: '484782b9d7db4453a5446a0abd5af99c',
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
        reportDate: '2020-10-09T09:02:02.883Z',
        uid: '4026826176',
    },
    payment: {
        purchaseId: '6cd7ce2e6f3d46ffab1c84d9f62589e2',
        methods: [
            {
                type: 'tiedCard',
                psId: 'YANDEXKASSA_V3',
                id: 'bank_card',
                name: 'Банковская карта',
                properties: {
                    card: { cddPanMask: '555555|4477', brand: 'MASTERCARD', expireYear: '2023', expireMonth: '3' },
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
            { type: 'wallet', balance: '1481900', needEmail: false },
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
            effective: 93,
            base: 310,
            reasons: [],
            modifiers: {},
            availableMoneyFeaturesPrice: null,
        },
        defaultEmail: 'gomer@yandex.ru',
        promocodes: {},
        preferWalletPayment: true,
    },
};

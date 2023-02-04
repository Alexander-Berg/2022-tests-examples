export const initPaymentWithManyMethodsGateStub = {
    paidReport: {
        paidReportId: 'a3807be5ab1046d8a2de29715ee98877',
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
        reportDate: '2020-10-10T09:47:54.247Z',
        uid: '4015472092',
    },
    payment: {
        purchaseId: '94122371880646f88aa39e643de2bb9d',
        methods: [
            {
                type: 'tiedCard',
                psId: 'YANDEXKASSA_V3',
                id: 'bank_card',
                name: 'Банковская карта',
                properties: {
                    card: {
                        cddPanMask: '370000|0002',
                        brand: 'AMERICAN_EXPRESS',
                        expireYear: '2044',
                        expireMonth: '9',
                    },
                },
                needEmail: true,
            },
            {
                type: 'tiedCard',
                psId: 'YANDEXKASSA_V3',
                id: 'bank_card',
                name: 'Банковская карта',
                properties: {
                    card: { cddPanMask: '411111|1111', brand: 'VISA', expireYear: '2044', expireMonth: '9' },
                },
                needEmail: true,
            },
            {
                type: 'tiedCard',
                psId: 'YANDEXKASSA_V3',
                id: 'bank_card',
                name: 'Банковская карта',
                properties: {
                    card: { cddPanMask: '352800|0000', brand: 'JCB', expireYear: '2099', expireMonth: '9' },
                },
                needEmail: true,
            },
            {
                type: 'tiedCard',
                psId: 'YANDEXKASSA_V3',
                id: 'bank_card',
                name: 'Банковская карта',
                properties: {
                    card: { cddPanMask: '675964|8453', brand: 'MAESTRO', expireYear: '2055', expireMonth: '5' },
                },
                needEmail: true,
            },
            {
                type: 'tiedCard',
                psId: 'YANDEXKASSA_V3',
                id: 'bank_card',
                name: 'Банковская карта',
                properties: {
                    card: { cddPanMask: '555555|4444', brand: 'MASTERCARD', expireYear: '2044', expireMonth: '4' },
                },
                needEmail: true,
            },
            {
                type: 'tiedCard',
                psId: 'YANDEXKASSA_V3',
                id: 'bank_card',
                name: 'Банковская карта',
                properties: {
                    card: { cddPanMask: '523555|4321', brand: 'MASTERCARD', expireYear: '2044', expireMonth: '4' },
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
            { type: 'wallet', balance: '226124', needEmail: false },
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
        defaultEmail: 'wwwwww@yandex.ru',
        promocodes: {},
        preferWalletPayment: true,
    },
};

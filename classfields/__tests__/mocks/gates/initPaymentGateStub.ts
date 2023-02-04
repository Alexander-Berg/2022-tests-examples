export const initPaymentGateStub = {
    paidReport: {
        paidReportId: 'a9b58d65ccc64ee99203ae07b8379505',
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
        reportDate: '2020-10-08T20:19:55.827Z',
        uid: '4026826176',
    },
    payment: {
        purchaseId: '6916ea771caa436a8a4b4fad979fa2aa',
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
            { type: 'wallet', balance: '1487600', needEmail: false },
            {
                type: 'yooMoney',
                psId: 'YANDEXKASSA_V3',
                id: 'yoo',
                name: 'ЮMoney',
                needEmail: true,
            },
        ],
        price: { isAvailable: true, effective: 93, base: 310, reasons: [], modifiers: {} },
        defaultEmail: 'gomer@yandex.ru',
        promocodes: {},
    },
};

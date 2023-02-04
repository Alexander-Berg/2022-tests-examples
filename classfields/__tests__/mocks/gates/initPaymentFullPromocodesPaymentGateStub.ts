export const initPaymentFullPromocodesPaymentGateStub = {
    paidReport: {
        paidReportId: '029a16483db640f99508b09c728e41bd',
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
        reportDate: '2020-10-09T08:43:50.499Z',
        uid: '4015472092',
    },
    payment: {
        purchaseId: '20a639004188447ab3e5b68d044ed8ff',
        methods: [{ type: 'promocodesOnly' }],
        price: {
            isAvailable: true,
            effective: 0,
            base: 310,
            reasons: [],
            modifiers: { money: 93 },
            availableMoneyFeaturesPrice: 147,
        },
        promocodes: {},
        preferWalletPayment: true,
    },
};

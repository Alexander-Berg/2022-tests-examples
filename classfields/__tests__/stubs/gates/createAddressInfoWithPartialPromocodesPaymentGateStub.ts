export const createAddressInfoWithPartialPromocodesPaymentGateStub = {
    addressInfoId: '875201527daa4db18aebc3d94a68af22',
    userObjectInfo: { offerId: '8207175651726858496' },
    evaluatedObjectInfo: {
        unifiedAddress: 'Россия, Москва, Чертановская улица, 48к2',
        floor: '10',
        area: 53.4,
        subjectFederationId: 1,
    },
    status: 'DONE',
    price: {
        isAvailable: true,
        effective: 100,
        base: 410,
        reasons: [],
        modifiers: { bonusDiscount: { percent: 70 }, money: 123 },
    },
    promocodes: { money: { discount: 23, count: 1 } },
};

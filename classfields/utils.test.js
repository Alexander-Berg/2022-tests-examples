const { isExternalPaymentMethod, PAYMENT_METHOD_IDS } = require('./utils');

describe('функция isExternalPaymentMethod', () => {
    it('относит "оплату кошельком" к внутренним платежным методам', () => {
        const result = isExternalPaymentMethod(PAYMENT_METHOD_IDS.wallet);
        expect(result).toBe(false);
    });

    it('относит "оплату яндекс.деньгами" к сторонним платежным методам', () => {
        const result = isExternalPaymentMethod(PAYMENT_METHOD_IDS.yandex_money);
        expect(result).toBe(true);
    });
});

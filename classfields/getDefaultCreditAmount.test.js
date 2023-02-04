const getDefaultCreditAmount = require('./getDefaultCreditAmount');
const creditConfig = {
    CREDIT_MIN_AMOUNT: 100000,
    CREDIT_MAX_AMOUNT: 1000000,
    CREDIT_AMOUNT_SLIDER_STEP: 25000,
    CREDIT_OFFER_INITIAL_PAYMENT_RATE: 0.2,
};

it('должен вернуть значение в границах конфига банка', () => {
    expect(getDefaultCreditAmount({ price: 1002000, creditConfig })).toEqual(800000);
    expect(getDefaultCreditAmount({ price: 970000, creditConfig })).toEqual(775000);
});
it('должен вернуть значение округленное по шагу слайдера', () => {
    expect(getDefaultCreditAmount({ price: 433000, creditConfig })).toEqual(325000);
});

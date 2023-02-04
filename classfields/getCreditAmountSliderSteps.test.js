const getCreditAmountSliderSteps = require('./getCreditAmountSliderSteps');

it('должен вернуть верный массив объектов', () => {
    expect(getCreditAmountSliderSteps({
        priceInRange: 267000,
        creditConfig: {
            CREDIT_MIN_AMOUNT: 225000,
            CREDIT_AMOUNT_SLIDER_STEP: 18000,
        },
    })).toEqual([ { value: 225000 }, { value: 243000 }, { value: 261000 }, { value: 267000 } ]);
});

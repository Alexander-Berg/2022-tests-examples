const onCreditGroup = require('./onCreditGroup');

it('преобразование параметров с бека на фронтовой фильтр', () => {
    const searchParams = {
        on_credit: true,
        credit_group: {
            payment_from: 1,
            payment_to: 2,
            loan_term: 3,
            initial_fee: 4,
        },
    };
    const result = onCreditGroup(searchParams);
    expect(result).toEqual({
        credit_payment_from: 1,
        credit_payment_to: 2,
        credit_loan_term: 3,
        credit_initial_fee: 4,
        on_credit: true,
    });
});

it('преобразования параметров с бека на фронтовой фильтр без on_credit', () => {
    const searchParams = {
        credit_group: {
            payment_from: 1,
            payment_to: 2,
            loan_term: 3,
            initial_fee: 4,
        },
    };
    const result = onCreditGroup(searchParams);
    expect(result).toEqual({
        credit_payment_from: 1,
        credit_payment_to: 2,
        credit_loan_term: 3,
        credit_initial_fee: 4,
        on_credit: true,
    });
});

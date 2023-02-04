const convertToCreditGroupString = require('./convertToCreditGroupString');

it('преобразование параметров к protobuf', () => {
    const result = {
        credit_payment_from: 1,
        credit_payment_to: 2,
        credit_loan_term: 3,
        credit_initial_fee: 4,
        on_credit: true,
    };
    convertToCreditGroupString(result);
    expect(result).toEqual({
        ...result,
        credit_group: 'payment_from=1,payment_to=2,loan_term=3,initial_fee=4',
        on_credit: true,
    });
});

it('преобразования параметров к protobuf без on_credit', () => {
    const result = {
        on_credit: false,
        credit_payment_to: 2,
    };
    convertToCreditGroupString(result);
    expect(result).toEqual({});
});

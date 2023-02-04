import prepareCreditProductCalculatorResponse from './prepareCreditProductCalculatorResponse';

it('должен правильно округлить', () => {
    const result = prepareCreditProductCalculatorResponse({
        result: { ok: {} },

        interest_rate_range: {
            from: 4.991231244,
            to: 4.991231244,
        },

        amount_range: {
            from: 0,
            to: 10,
        },

        term_months_range: {
            from: 10,
            to: 20,
        },

        min_initial_fee_rate: 0,
    });

    expect(result.interest_rate_range.from).toEqual(4.99);
    expect(result.interest_rate_range.to).toEqual(4.99);
});

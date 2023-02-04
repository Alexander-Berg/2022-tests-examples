import creditProductMock from 'auto-core/react/dataDomain/credit/mocks/creditProduct.mockchain';

import prepareCreditProductListResponse from './prepareCreditProductListResponse';

it('должен правильно округлить', () => {
    const result = prepareCreditProductListResponse({
        result: { ok: {} },

        credit_products: [
            creditProductMock()
                .withInterestRateRange({
                    from: 4.991231244,
                    to: 4.991231244,
                })
                .value(),
        ],
    });

    expect(result.credit_products?.[0].interest_rate_range.from).toEqual(4.99);
    expect(result.credit_products?.[0].interest_rate_range.to).toEqual(4.99);
});

import creditApplicationMock from 'auto-core/react/dataDomain/credit/mocks/creditApplication.mockchain';
import creditApplicationClaimMock from 'auto-core/react/dataDomain/credit/mocks/creditApplicationClaim.mockchain';

import prepareCreditApplicationResponse from './prepareCreditApplicationResponse';

it('должен правильно округлить', () => {
    const result = prepareCreditApplicationResponse({
        result: { ok: {} },

        credit_application:
            creditApplicationMock()
                .withClaim(
                    creditApplicationClaimMock()
                        .withSentSnapshot()
                        .value(),
                )
                .value(),
    });

    expect(result.credit_application?.claims?.[0]?.sent_snapshot?.credit_product_properties?.interest_rate_range.from).toEqual(4.99);
    expect(result.credit_application?.claims?.[0]?.sent_snapshot?.credit_product_properties?.interest_rate_range.to).toEqual(11.99);
});

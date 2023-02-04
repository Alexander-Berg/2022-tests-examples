import creditApplicationMock from 'auto-core/react/dataDomain/credit/mocks/creditApplication.mockchain';
import creditApplicationClaimMock from 'auto-core/react/dataDomain/credit/mocks/creditApplicationClaim.mockchain';
import creditProductMock from 'auto-core/react/dataDomain/credit/mocks/creditProduct.mockchain';

import { ClaimState, CreditApplicationState } from 'auto-core/types/TCreditBroker';

import areAllCreditClaimsRejeted from './areAllCreditClaimsRejeted';

const creditProduct = creditProductMock().value();
const rejectedCreditApplicationClaim = creditApplicationClaimMock().withState(ClaimState.REJECT).value();
const notSentCreditApplicationClaim = creditApplicationClaimMock().withState(ClaimState.NOT_SENT).value();

it('возвращает true, если все заявки в статусе REJECT', () => {
    const creditProducts = [ creditProduct, creditProduct, creditProduct ];

    const creditApplication = creditApplicationMock()
        .withState(CreditApplicationState.ACTIVE)
        .withClaim(rejectedCreditApplicationClaim)
        .withClaim(rejectedCreditApplicationClaim)
        .withClaim(rejectedCreditApplicationClaim)
        .value();

    expect(areAllCreditClaimsRejeted({ creditApplication, creditProducts }))
        .toEqual(true);
});

it('возвращает true, если все заявки в статусе REJECT или NOT_SENT', () => {
    const creditProducts = [ creditProduct, creditProduct, creditProduct ];

    const creditApplication = creditApplicationMock()
        .withState(CreditApplicationState.ACTIVE)
        .withClaim(rejectedCreditApplicationClaim)
        .withClaim(notSentCreditApplicationClaim)
        .withClaim(notSentCreditApplicationClaim)
        .value();

    expect(areAllCreditClaimsRejeted({ creditApplication, creditProducts }))
        .toEqual(true);
});

it('возвращает false, если не все заявки в статусе REJECT', () => {
    const creditProducts = [ creditProduct, creditProduct, creditProduct ];
    const newCreditApplicationClaim = creditApplicationClaimMock()
        .withState(ClaimState.NEW)
        .value();

    const creditApplication = creditApplicationMock()
        .withState(CreditApplicationState.ACTIVE)
        .withClaim(rejectedCreditApplicationClaim)
        .withClaim(newCreditApplicationClaim)
        .withClaim(rejectedCreditApplicationClaim)
        .value();

    expect(areAllCreditClaimsRejeted({ creditApplication, creditProducts }))
        .toEqual(false);
});

it('возвращает false, если все заявки в статусе REJECT, но есть доступные продукты', () => {
    const creditProducts = [ creditProduct, creditProduct, creditProduct, creditProduct ];

    const creditApplication = creditApplicationMock()
        .withState(CreditApplicationState.ACTIVE)
        .withClaim(rejectedCreditApplicationClaim)
        .withClaim(rejectedCreditApplicationClaim)
        .withClaim(rejectedCreditApplicationClaim)
        .value();

    expect(areAllCreditClaimsRejeted({ creditApplication, creditProducts }))
        .toEqual(false);
});

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import creditProductMock from 'auto-core/react/dataDomain/credit/mocks/creditProduct.mockchain';
import creditApplicationMock from 'auto-core/react/dataDomain/credit/mocks/creditApplication.mockchain';
import creditApplicationClaimMock from 'auto-core/react/dataDomain/credit/mocks/creditApplicationClaim.mockchain';
import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import { ClaimState } from 'auto-core/types/TCreditBroker';

import getUnsentCreditProductsCount from './getUnsentCreditProductsCount';

const claims = [
    creditApplicationClaimMock().withProductID('test-1').value(),
    creditApplicationClaimMock().withProductID('test-3').value(),
];
const creditProducts = [
    creditProductMock().withID('test-1').value(),
    creditProductMock().withID('test-2').value(),
    creditProductMock().withID('test-3').value(),
    creditProductMock().withID('test-4').value(),
];

const offerWithEcredit = cloneOfferWithHelpers(offerMock)
    .withEcreditPrecondition()
    .withIsOwner(false)
    .value();

it('возвращает верное количество неотправленных кредитных продуктов', () => {
    const creditApplication = creditApplicationMock().withClaims(claims).value();
    expect(getUnsentCreditProductsCount({ creditProducts, creditApplication })).toEqual(2);
});

it('возвращает верное количество неотправленных кредитных продуктов с оффером из флоу е-кредита', () => {
    const claimsWithCancelled = [
        ...claims,
        creditApplicationClaimMock().withProductID('test-4').withState(ClaimState.CANCELED_DRAFT).value(),
    ];
    const creditApplication = creditApplicationMock().withClaims(claimsWithCancelled).value();
    expect(getUnsentCreditProductsCount({ creditProducts, creditApplication, offer: offerWithEcredit })).toEqual(2);
});

it('возвращает верное количество неотправленных кредитных продуктов с отмененными заявками', () => {
    const claimsWithCancelled = [
        ...claims,
        creditApplicationClaimMock().withProductID('test-4').withState(ClaimState.CANCELED_DRAFT).value(),
    ];

    const creditApplication = creditApplicationMock().withClaims(claimsWithCancelled).value();

    expect(getUnsentCreditProductsCount({ creditProducts, creditApplication })).toEqual(2);
});

it('возвращает верное количество неотправленных кредитных продуктов, если нет отправленных', () => {
    const creditApplication = creditApplicationMock().value();
    expect(getUnsentCreditProductsCount({ creditProducts, creditApplication })).toEqual(4);
});

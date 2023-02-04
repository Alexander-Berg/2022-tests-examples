import bankMock from 'auto-core/react/dataDomain/credit/mocks/bank.mockchain';
import creditProductMock from 'auto-core/react/dataDomain/credit/mocks/creditProduct.mockchain';
import creditApplicationMock from 'auto-core/react/dataDomain/credit/mocks/creditApplication.mockchain';
import creditApplicationClaimMock from 'auto-core/react/dataDomain/credit/mocks/creditApplicationClaim.mockchain';

import { BankID, CreditApplicationState, ClaimState } from 'auto-core/types/TCreditBroker';

import getApprovedCreditClaimsInfo from './getApprovedCreditClaimsInfo';

const creditProducts = [
    creditProductMock()
        .withBankID(BankID.ALFABANK)
        .withID('test-1')
        .value(),
    creditProductMock()
        .withBankID(BankID.TINKOFF)
        .withID('test-2')
        .value(),
    creditProductMock()
        .withBankID(BankID.RAIFFEISEN)
        .withID('test-3')
        .value(),
];

const banks = [
    bankMock().withBankID(BankID.ALFABANK).value(),
    bankMock().withBankID(BankID.TINKOFF).value(),
    bankMock().withBankID(BankID.SBERBANK).value(),
    bankMock().withBankID(BankID.RAIFFEISEN).value(),
];

it('вернет null, если одобренных клеймов нет', () => {
    const creditApplication = creditApplicationMock()
        .withRequirements({ amount: 500000, term: 48, fee: 100000 })
        .withState(CreditApplicationState.ACTIVE)
        .value();

    expect(getApprovedCreditClaimsInfo({ banks, creditProducts, creditApplication })).toBeNull();
});

it('вернет информацию об одобренных клеймах', () => {
    const claims = [
        creditApplicationClaimMock()
            .withProductID('test-2')
            .withState(ClaimState.DRAFT)
            .value(),
        creditApplicationClaimMock()
            .withProductID('test-1')
            .withState(ClaimState.APPROVED)
            .value(),
        creditApplicationClaimMock()
            .withProductID('test-4')
            .withState(ClaimState.NEW)
            .value(),
        creditApplicationClaimMock()
            .withProductID('test-3')
            .withState(ClaimState.PREAPPROVED)
            .value(),
    ];
    const creditApplication = creditApplicationMock()
        .withRequirements({ amount: 500000, term: 48, fee: 100000 })
        .withState(CreditApplicationState.ACTIVE)
        .withClaims(claims)
        .value();

    const result = getApprovedCreditClaimsInfo({ banks, creditProducts, creditApplication });

    expect(result).toEqual({
        banks: [ banks[0], banks[3] ],
        creditProducts: [ creditProducts[0], creditProducts[2] ],
        claims: [ claims[1], claims[3] ],
    });
});

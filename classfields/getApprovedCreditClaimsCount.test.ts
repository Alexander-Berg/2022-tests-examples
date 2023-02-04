import creditApplicationMock from 'auto-core/react/dataDomain/credit/mocks/creditApplication.mockchain';
import creditApplicationClaimMock from 'auto-core/react/dataDomain/credit/mocks/creditApplicationClaim.mockchain';

import { ClaimState } from 'auto-core/types/TCreditBroker';

import getApprovedCreditClaimsCount from './getApprovedCreditClaimsCount';

describe('getApprovedCreditClaimsCount', () => {
    it('верно определяет количество одобренных заявок', () => {
        const approvedCreditApplicationClaim = creditApplicationClaimMock()
            .withState(ClaimState.APPROVED)
            .value();

        const creditApplication = creditApplicationMock()
            .withClaim(approvedCreditApplicationClaim)
            .withClaim(approvedCreditApplicationClaim)
            .withClaim(approvedCreditApplicationClaim)
            .value();

        expect(getApprovedCreditClaimsCount(creditApplication))
            .toEqual(3);
    });

    it('учитывает статус PREAPPROVED как одобреный', () => {
        const approvedCreditApplicationClaim = creditApplicationClaimMock()
            .withState(ClaimState.PREAPPROVED)
            .value();

        const creditApplication = creditApplicationMock()
            .withClaim(approvedCreditApplicationClaim)
            .withClaim(approvedCreditApplicationClaim)
            .value();

        expect(getApprovedCreditClaimsCount(creditApplication))
            .toEqual(2);
    });
});

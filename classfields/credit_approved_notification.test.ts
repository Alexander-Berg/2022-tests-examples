jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});
import contextMock from 'autoru-frontend/mocks/contextMock';

import gateApi from 'auto-core/react/lib/gateApi';
import creditApplicationMock from 'auto-core/react/dataDomain/credit/mocks/creditApplication.mockchain';
import creditApplicationClaimMock from 'auto-core/react/dataDomain/credit/mocks/creditApplicationClaim.mockchain';
import { AutoPopupNames } from 'auto-core/react/dataDomain/autoPopup/types';
import type { TStateCredit } from 'auto-core/react/dataDomain/credit/TStateCredit';

import { ClaimState, CreditApplicationState } from 'auto-core/types/TCreditBroker';

import type { DesktopAppState } from 'www-desktop/react/DesktopAppState';

const getResource = gateApi.getResource as jest.MockedFunction<typeof gateApi.getResource>;

import item from './credit_approved_notification';

describe('run()', () => {
    afterEach(() => {
        getResource.mockReset();
    });

    const creditApproveShowResult = {
        id: AutoPopupNames.CREDIT_APPROVED_NOTIFICATION,
    };
    const approvedCreditClaim = creditApplicationClaimMock()
        .withProductID('test-1')
        .withState(ClaimState.APPROVED)
        .value();
    const creditApplicationWithApprovedClaim = creditApplicationMock()
        .withRequirements({ amount: 500000, term: 48, fee: 100000 })
        .withState(CreditApplicationState.ACTIVE)
        .withClaim(approvedCreditClaim)
        .value();

    it('будет показана, если в сторе уже есть данные с одобренным кредитом', async() => {
        const state = {
            credit: {
                application: { data: { credit_application: creditApplicationWithApprovedClaim } },
            } as TStateCredit,
        } as DesktopAppState;

        const result = await item.run(state, jest.fn(), contextMock);
        expect(result).toMatchObject(creditApproveShowResult);
    });

    it('будет показана после запроса кредитной заявки с одобренным кредитом', async() => {
        const state = {
            credit: {
                application: { data: null, isError: false, isPending: false },
            } as TStateCredit,
        } as DesktopAppState;
        getResource.mockReturnValueOnce(Promise.resolve({
            credit_application: creditApplicationWithApprovedClaim,
        }));

        const result = await item.run(state, (fn: any) => fn(() => { }), contextMock);
        expect(result).toMatchObject(creditApproveShowResult);
    });

    it('не будет показана после запроса кредитной заявки без одобренного кредита', async() => {
        const state = {
            credit: {
                application: { data: null, isError: false, isPending: false },
            } as TStateCredit,
        } as DesktopAppState;
        getResource.mockReturnValueOnce(Promise.resolve({
            credit_application: creditApplicationMock()
                .withRequirements({ amount: 500000, term: 48, fee: 100000 })
                .withState(CreditApplicationState.ACTIVE)
                .value(),
        }));
        const result = await item.run(state, (fn: any) => fn(() => { }), contextMock);

        expect(result).toBeUndefined();
    });

    it('не будет показана, если в сторе уже есть данные с пустой заявкой', async() => {
        const state = {
            credit: {
                application: { data: { credit_application: {} } },
            } as TStateCredit,
        } as DesktopAppState;

        const result = await item.run(state, jest.fn(), contextMock);
        expect(result).toBeUndefined();
    });

});

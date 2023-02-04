import { C2bApplicationScreenLk } from 'auto-core/react/dataDomain/c2bApplications/types';

import type { C2bApplication } from 'auto-core/server/blocks/c2bAuction/types';
import { C2bApplicationStatus, BuyoutFlow } from 'auto-core/server/blocks/c2bAuction/types';

import mapApplicationStatusToLkScreen from './mapApplicationStatusToLkScreen';

describe('Маппинги статусов заявок на Выкуп', () => {
    it('Правильно мапит статусы для заявок по старому флоу', () => {
        expect(mapApplicationStatusToLkScreen(createMockApplication(C2bApplicationStatus.UNSET)))
            .toBe(C2bApplicationScreenLk.INSPECTION);
        expect(mapApplicationStatusToLkScreen(createMockApplication(C2bApplicationStatus.NEW)))
            .toBe(C2bApplicationScreenLk.INSPECTION);
        expect(mapApplicationStatusToLkScreen(createMockApplication(C2bApplicationStatus.WAITING_INSPECTION)))
            .toBe(C2bApplicationScreenLk.INSPECTION);
        expect(mapApplicationStatusToLkScreen(createMockApplication(C2bApplicationStatus.INSPECTED)))
            .toBe(C2bApplicationScreenLk.PROPASALS_FINAL);
        expect(mapApplicationStatusToLkScreen(createMockApplication(C2bApplicationStatus.AUCTION)))
            .toBe(C2bApplicationScreenLk.PROPASALS_FINAL);
        expect(mapApplicationStatusToLkScreen(createMockApplication(C2bApplicationStatus.DEAL)))
            .toBe(C2bApplicationScreenLk.DEAL_IN_PROCESS);
        expect(mapApplicationStatusToLkScreen(createMockApplication(C2bApplicationStatus.WAITING_DOCUMENTS)))
            .toBe(C2bApplicationScreenLk.INSPECTION);
        expect(mapApplicationStatusToLkScreen(createMockApplication(C2bApplicationStatus.PRE_OFFERS)))
            .toBe(C2bApplicationScreenLk.INSPECTION);
        expect(mapApplicationStatusToLkScreen(createMockApplication(C2bApplicationStatus.CONFIRM_PRE_OFFERS)))
            .toBe(C2bApplicationScreenLk.PROPOSALS_CONFIRMATION);
        expect(mapApplicationStatusToLkScreen(createMockApplication(C2bApplicationStatus.FINAL_OFFERS)))
            .toBe(C2bApplicationScreenLk.PROPASALS_FINAL);
        expect(mapApplicationStatusToLkScreen(createMockApplication(C2bApplicationStatus.REJECTED)))
            .toBe(C2bApplicationScreenLk.DEAL_REJECTED);
        expect(mapApplicationStatusToLkScreen(createMockApplication(C2bApplicationStatus.FINISHED)))
            .toBe(C2bApplicationScreenLk.DEAL_SUCCESSFUL);
    });

    it('Правильно мапит статусы для заявок по новому флоу', () => {
        expect(mapApplicationStatusToLkScreen(createMockApplication(C2bApplicationStatus.UNSET, true)))
            .toBe(C2bApplicationScreenLk.PROPASALS_PRELIMINARY);
        expect(mapApplicationStatusToLkScreen(createMockApplication(C2bApplicationStatus.NEW, true)))
            .toBe(C2bApplicationScreenLk.PROPASALS_PRELIMINARY);
        expect(mapApplicationStatusToLkScreen(createMockApplication(C2bApplicationStatus.WAITING_INSPECTION, true)))
            .toBe(C2bApplicationScreenLk.INSPECTION);
        expect(mapApplicationStatusToLkScreen(createMockApplication(C2bApplicationStatus.INSPECTED, true)))
            .toBe(C2bApplicationScreenLk.PROPASALS_FINAL);
        expect(mapApplicationStatusToLkScreen(createMockApplication(C2bApplicationStatus.AUCTION, true)))
            .toBe(C2bApplicationScreenLk.PROPASALS_FINAL);
        expect(mapApplicationStatusToLkScreen(createMockApplication(C2bApplicationStatus.DEAL, true)))
            .toBe(C2bApplicationScreenLk.DEAL_IN_PROCESS);
        expect(mapApplicationStatusToLkScreen(createMockApplication(C2bApplicationStatus.WAITING_DOCUMENTS, true)))
            .toBe(C2bApplicationScreenLk.PROPASALS_PRELIMINARY);
        expect(mapApplicationStatusToLkScreen(createMockApplication(C2bApplicationStatus.PRE_OFFERS, true)))
            .toBe(C2bApplicationScreenLk.PROPASALS_PRELIMINARY);
        expect(mapApplicationStatusToLkScreen(createMockApplication(C2bApplicationStatus.CONFIRM_PRE_OFFERS, true)))
            .toBe(C2bApplicationScreenLk.PROPOSALS_CONFIRMATION);
        expect(mapApplicationStatusToLkScreen(createMockApplication(C2bApplicationStatus.FINAL_OFFERS, true)))
            .toBe(C2bApplicationScreenLk.PROPASALS_FINAL);
        expect(mapApplicationStatusToLkScreen(createMockApplication(C2bApplicationStatus.REJECTED, true)))
            .toBe(C2bApplicationScreenLk.DEAL_REJECTED);
        expect(mapApplicationStatusToLkScreen(createMockApplication(C2bApplicationStatus.FINISHED, true)))
            .toBe(C2bApplicationScreenLk.DEAL_SUCCESSFUL);
    });
});

function createMockApplication(status: C2bApplicationStatus, isNewFlow?: boolean): C2bApplication {
    return {
        status,
        buy_out_alg: isNewFlow ? BuyoutFlow.WITH_PRE_OFFERS : BuyoutFlow.AUCTION,
    } as C2bApplication;
}

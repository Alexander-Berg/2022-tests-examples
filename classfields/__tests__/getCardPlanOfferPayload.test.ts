import MockDate from 'mockdate';

import { EventType, EventPlace } from 'realty-core/types/eventLog';
import analiticsData from 'realty-core/view/react/libs/analiticsData';

import { CORE_STORE, MOCK_DATE, CARD_PLANS_OFFER, ANALYTIC_DATA } from '../../__tests__/mocks';
import { getCommonStoreParams } from '../../libs/getCommonStoreParams';

import { getCardPlanOfferPayload } from '../getCardPlanOfferPayload';

jest.mock('realty-core/view/react/libs/analiticsData', () => ({
    get: jest.fn(),
}));

const commonStoreParams = getCommonStoreParams(CORE_STORE);

describe('getCardPlanOfferPayload', () => {
    beforeEach(() => {
        MockDate.set(MOCK_DATE);
    });

    it('Возвращает полный объект', () => {
        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-ignore
        analiticsData.get.mockImplementation(() => ANALYTIC_DATA);

        expect(
            getCardPlanOfferPayload({
                ...commonStoreParams,
                offer: CARD_PLANS_OFFER,
                position: 2,
                page: 0,
                eventType: EventType.OFFER_SHOW,
                eventPlace: EventPlace.NEWBUILDING_PLAN_POPUP,
            })
        ).toMatchSnapshot();
    });
});

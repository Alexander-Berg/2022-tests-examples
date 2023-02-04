import MockDate from 'mockdate';

import { EventType, EventPlace, CardViewType } from 'realty-core/types/eventLog';
import analiticsData from 'realty-core/view/react/libs/analiticsData';

import { CORE_STORE, MOCK_DATE, PHONE_NUMBER, ANALYTIC_DATA, CHAT } from '../../__tests__/mocks';
import { getCommonStoreParams } from '../../libs/getCommonStoreParams';

import { getSiteSnippetLogPayload } from '../getSiteSnippetLogPayload';

import { siteSnippet } from './mocks/siteSnippet';

jest.mock('realty-core/view/react/libs/analiticsData', () => ({
    get: jest.fn(),
}));

const commonStoreParams = getCommonStoreParams(CORE_STORE);

describe('getSiteSnippetLogPayload', () => {
    beforeEach(() => {
        MockDate.set(MOCK_DATE);
    });

    it('Возвращает полный объект', () => {
        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-ignore
        analiticsData.get.mockImplementation(() => ANALYTIC_DATA);

        expect(
            getSiteSnippetLogPayload({
                ...commonStoreParams,
                snippet: siteSnippet,
                phone: PHONE_NUMBER,
                redirectId: PHONE_NUMBER,
                chat: CHAT,
                position: 1,
                eventType: EventType.CARD_SHOW,
                eventPlace: EventPlace.NEWBUILDING_CARD_TOP,
                cardViewType: CardViewType.NB_CARD,
            })
        ).toMatchSnapshot();
    });
});

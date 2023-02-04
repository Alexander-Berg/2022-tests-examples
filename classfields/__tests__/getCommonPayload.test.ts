import MockDate from 'mockdate';

import analiticsData from 'realty-core/view/react/libs/analiticsData';
import { EventType, EventPlace, CardViewType, ClientType } from 'realty-core/types/eventLog';

import { QUERY_ID, QUERY_TEXT, REREFERER, WEB_REFERER, MOCK_DATE, ANALYTIC_DATA } from '../../__tests__/mocks';

import { getCommonPayload } from '../getCommonPayload';

jest.mock('realty-core/view/react/libs/analiticsData', () => ({
    get: jest.fn(),
}));

const params = {
    eventType: EventType.CARD_SHOW,
    clientType: ClientType.DESKTOP,
    eventPlace: EventPlace.CARD_TOP,
    cardViewType: CardViewType.NB_CARD,
    queryId: QUERY_ID,
    queryText: QUERY_TEXT,
    webReferer: WEB_REFERER,
};

describe('getCommonPayload', () => {
    beforeEach(() => {
        MockDate.set(MOCK_DATE);
    });

    it('Возвращает корректый объект', () => {
        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-ignore
        analiticsData.get.mockImplementation(() => ({}));

        expect(getCommonPayload(params)).toMatchSnapshot();
    });

    it('Возвращает полный объект', () => {
        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-ignore
        analiticsData.get.mockImplementation(() => ANALYTIC_DATA);

        expect(
            getCommonPayload({
                ...params,
                rereferer: REREFERER,
                page: 1,
                position: 1,
                totalResults: 10,
            })
        ).toMatchSnapshot();
    });
});

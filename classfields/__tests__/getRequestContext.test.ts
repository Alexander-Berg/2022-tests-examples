import { CardViewType, EventPlace } from 'realty-core/types/eventLog';

import { getRequestContext } from '../getRequestContext';

import { QUERY_ID, QUERY_TEXT, WEB_REFERER, REREFERER } from '../../__tests__/mocks';

const data = {
    eventPlace: EventPlace.NEWBUILDING_CARD_TOP,
    cardViewType: CardViewType.NB_CARD,
    queryText: QUERY_TEXT,
    queryId: QUERY_ID,
    webReferer: WEB_REFERER,
};

describe('getRequestContext', () => {
    it('Возвращает корректый объект (должен быть без дефолтов)', () => {
        expect(getRequestContext(data)).toMatchSnapshot();
    });

    it('Возвращает корректный объект со всеми полями', () => {
        expect(
            getRequestContext({
                ...data,
                rereferer: REREFERER,
                pageNumber: 1,
                absoluteOfferPosition: 1,
                totalResults: 10,
            })
        ).toMatchSnapshot();
    });
});

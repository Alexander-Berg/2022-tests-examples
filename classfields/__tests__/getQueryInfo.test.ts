import { getQueryInfo } from '../getQueryInfo';

import { QUERY_ID, QUERY_TEXT } from '../../__tests__/mocks';

describe('getQueryInfo', () => {
    it('Возвращает корректый объект', () => {
        expect(
            getQueryInfo({
                queryText: QUERY_TEXT,
                queryId: QUERY_ID,
            })
        ).toMatchSnapshot();
    });
});

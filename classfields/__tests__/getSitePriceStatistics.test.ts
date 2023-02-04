import { SITE_PRICE } from '../../__tests__/mocks';

import { getSitePriceStatistics } from '../getSitePriceStatistics';

describe('getSitePriceStatistics', () => {
    it('Возвращает ничего', () => {
        expect(getSitePriceStatistics({ price: undefined })).toMatchSnapshot();
    });

    it('Возвращает полный объект со всеми вариантами', () => {
        expect(getSitePriceStatistics({ price: SITE_PRICE })).toMatchSnapshot();
    });
});

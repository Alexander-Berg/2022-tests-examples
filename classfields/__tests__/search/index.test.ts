import { listingOffersCountSelector } from '../../search';

import { searchMock } from './mock';

describe('listingOffersCountSelector', function () {
    test('Выдача с офферами', () => {
        Object.values(searchMock).forEach((viewType) => {
            Object.values(viewType).forEach((reducer) => {
                expect(listingOffersCountSelector(reducer)).toBe(12);
            });
        });
    });
});

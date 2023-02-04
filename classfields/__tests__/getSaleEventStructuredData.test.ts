import MockDate from 'mockdate';

import { getSaleEventStructuredData } from '../getSaleEventStructuredData';

describe('sale event', () => {
    beforeEach(() => {
        MockDate.set('2021-12-01T21:00');
    });

    afterEach(() => {
        MockDate.reset();
    });

    it('search arenda', () => {
        getSaleEventStructuredData().forEach((saleEventStructuredData) => {
            expect(saleEventStructuredData).toMatchSnapshot();
        });
    });
});

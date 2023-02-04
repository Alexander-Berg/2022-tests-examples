import MockDate from 'mockdate';

import { getSaleEventTenantStructuredData } from '../getSaleEventTenantStructuredData';

describe('sale event', () => {
    beforeEach(() => {
        MockDate.set('2021-12-01T21:00');
    });

    afterEach(() => {
        MockDate.reset();
    });

    it('search to-tenant', () => {
        getSaleEventTenantStructuredData().forEach((saleEventTenantStructuredData) => {
            expect(saleEventTenantStructuredData).toMatchSnapshot();
        });
    });
});

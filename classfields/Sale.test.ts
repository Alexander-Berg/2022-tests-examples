import { PaidReason } from '@vertis/schema-registry/ts-types-snake/auto/api/common_model';
import { Sale } from 'app/server/tmpl/android/v1/helpers/sale/Sale';
import type { ResolutionBilling } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_resolution_model';

const billing = {
    service_prices: [
        {
            service: 'offers-history-reports',
            days: 365,
            price: 79,
            currency: 'RUR',
            original_price: 197,
            paid_reason: PaidReason.FREE_LIMIT,
            recommendation_priority: 0,
            aliases: [
                'offers-history-reports-1',
            ],
            need_confirm: false,
            counter: '1',
            purchase_forbidden: false,
        },
    ],
};

describe('sale', () => {
    it('должен округлить скидку до 60 %', () => {
        const sale = new Sale(billing as ResolutionBilling);

        expect(sale.getSaleDiscount()).toEqual(60);
    });
});

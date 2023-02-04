import _ from 'lodash';

import type { PaidServicePrice } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';
import { PaidReason } from '@vertis/schema-registry/ts-types-snake/auto/api/common_model';

import cardVinReportFree from 'auto-core/react/dataDomain/vinReport/mocks/freeReport-mercedes.mock';

import getLowestPricePerReportBillingParams from './getLowestPricePerReportBillingParams';

const COMMON_PROPS: Partial<PaidServicePrice> = {
    name: '',
    title: '',
    description: '',
    days: 0,
    recommendation_priority: 0,
    need_confirm: false,
    currency: 'RUR',
    service: 'offers-history-reports',
    paid_reason: PaidReason.FREE_LIMIT,
};

const SERVICE_PRICES: Array<PaidServicePrice> = [
    {
        price: 99,
        original_price: 197,
        counter: '1',
        ...COMMON_PROPS,
    } as PaidServicePrice,
    {
        price: 500,
        original_price: 999,
        counter: '10',
        ...COMMON_PROPS,
    } as PaidServicePrice,
    {
        price: 1999,
        original_price: 2999,
        counter: '50',
        ...COMMON_PROPS,
    } as PaidServicePrice,
];

it('возвращает сервис (большой пакет), у которого цена одного отчета ниже', () => {
    const report = _.cloneDeep(cardVinReportFree);

    report.billing && (report.billing.service_prices = SERVICE_PRICES);

    const result = getLowestPricePerReportBillingParams(report);
    expect(result).toEqual(SERVICE_PRICES[2]);
});

it('возвращает сервис одного отчета (потому что он дешевле)', () => {
    const report = _.cloneDeep(cardVinReportFree);
    const servicePrices = _.cloneDeep(SERVICE_PRICES);
    servicePrices[0].price = 10;

    report.billing && (report.billing.service_prices = servicePrices);

    const result = getLowestPricePerReportBillingParams(report);
    expect(result).toEqual(servicePrices[0]);
});

it('возвращает undefined, когда нет service prices', () => {
    const report = _.cloneDeep(cardVinReportFree);

    report.billing && (report.billing.service_prices = []);

    const result = getLowestPricePerReportBillingParams(report);
    expect(result).toBeUndefined();
});

import type { StateVinReportData } from 'auto-core/react/dataDomain/vinReport/types';

import getMaxBundleBillingParams from './getMaxBundleBillingParams';

it('если нет пакетов вернет undefined', () => {
    const vinReport = { billing: { } };
    const result = getMaxBundleBillingParams(vinReport as unknown as StateVinReportData);
    expect(result).toBeUndefined();
});

it('если доступен только один отчет вернет undefined', () => {
    const vinReport = { billing: {
        service_prices: [
            { counter: '1' },
        ],
    } };
    const result = getMaxBundleBillingParams(vinReport as unknown as StateVinReportData);
    expect(result).toBeUndefined();
});

it('из двух пакетов вернет наибольший', () => {
    const vinReport = {
        billing: {
            service_prices: [
                { counter: '1' },
                { counter: '10' },
                { counter: '100' },
            ],
        },
    };
    const result = getMaxBundleBillingParams(vinReport as unknown as StateVinReportData);
    expect(result).toEqual({ counter: '100' });
});

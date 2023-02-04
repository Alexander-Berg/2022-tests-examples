import type { StateVinReportData } from 'auto-core/react/dataDomain/vinReport/types';

import getSingleReportBillingParams from './getSingleReportBillingParams';

it('если нет пакетов вернет undefined', () => {
    const vinReport = { billing: { } };
    const result = getSingleReportBillingParams(vinReport as unknown as StateVinReportData);
    expect(result).toBeUndefined();
});

it('если доступен только один отчет вернет его', () => {
    const vinReport = { billing: {
        service_prices: [
            { counter: '1' },
        ],
    } };
    const result = getSingleReportBillingParams(vinReport as unknown as StateVinReportData);
    expect(result).toEqual({ counter: '1' });
});

it('из двух пакетов вернет с 1', () => {
    const vinReport = {
        billing: {
            service_prices: [
                { counter: '10' },
                { counter: '100' },
                { counter: '1' },
            ],
        },
    };
    const result = getSingleReportBillingParams(vinReport as unknown as StateVinReportData);
    expect(result).toEqual({ counter: '1' });
});

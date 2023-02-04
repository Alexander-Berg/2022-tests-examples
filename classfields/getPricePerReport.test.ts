import cardVinReportFree from 'auto-core/react/dataDomain/vinReport/mocks/freeReport-mercedes.mock';

import getPricePerReport from './getPricePerReport';

it('возвращает цену за 1 отчёт из расчет цена/количество в пакете', () => {
    const service1 = cardVinReportFree.billing!.service_prices[0];
    const service10 = cardVinReportFree.billing!.service_prices[1];
    const service50 = cardVinReportFree.billing!.service_prices[2];
    expect(getPricePerReport(service1)).toEqual(99);
    expect(getPricePerReport(service10)).toEqual(50);
    expect(getPricePerReport(service50)).toEqual(40);
});

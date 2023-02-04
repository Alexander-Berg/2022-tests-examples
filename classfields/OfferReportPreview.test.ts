import type { RawVinReport } from '@vertis/schema-registry/ts-types/auto/api/vin/vin_report_model';
import { createHelpers } from 'app/server/tmpl/ios/v1/Main';
import { toCamel } from 'snake-camel';
import { getFreeReport } from 'mocks/vinReport/freeReport';
import { getBillingWithDiscount } from 'mocks/billing/billing';
import type { ResolutionBilling } from '@vertis/schema-registry/ts-types/auto/api/vin/vin_resolution_model';
import { jpath } from 'nommon';
import { OfferReportPreview } from './OfferReportPreview';

function createNodes(report: RawVinReport, billing?: ResolutionBilling) {
    const helpers = createHelpers(report);
    helpers.billing = billing;
    return new OfferReportPreview().reportOfferNodes(report, helpers);
}

function getBillingWithDiscountMock(): ResolutionBilling {
    return toCamel(getBillingWithDiscount()) as unknown as ResolutionBilling;
}

function getFreeReportMock(): RawVinReport {
    return toCamel(getFreeReport()) as unknown as RawVinReport;
}

it('Есть баннер, если есть скидка на пакеты', () => {
    const report = getFreeReportMock();
    const nodes = createNodes(report, getBillingWithDiscountMock());
    const bannerNode = jpath('.[0]', nodes);
    expect(bannerNode).toMatchSnapshot();
});

it('Нет баннера, если нет скидки на пакеты', () => {
    const report = getFreeReportMock();
    const nodes = createNodes(report);
    const bannerNode = jpath('.[0]', nodes);
    expect(bannerNode).toMatchSnapshot();
});

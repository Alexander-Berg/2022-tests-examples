import type { RawVinReport } from '@vertis/schema-registry/ts-types/auto/api/vin/vin_report_model';
import { toCamel } from 'snake-camel';
import { yogaToXml } from 'app/server/tmpl/XMLBuilder';
import { getFreeReport } from 'mocks/vinReport/freeReport';
import { FreeReportPromo } from './Creator';
import { createHelpers } from '../../Main';

function getFreeReportMock(): RawVinReport {
    return toCamel(getFreeReport()) as unknown as RawVinReport;
}

function snapshot(report: RawVinReport): string {
    const helpers = createHelpers(report);
    const nodes = new FreeReportPromo().cellNodes(report, helpers);
    return yogaToXml(nodes);
}

it('FreeReportPromoCell', () => {
    const report = getFreeReportMock();
    const xml = snapshot(report);
    expect(xml).toMatchSnapshot('cell');
});

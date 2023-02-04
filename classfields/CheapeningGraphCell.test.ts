import type { RawVinReport } from '@vertis/schema-registry/ts-types/auto/api/vin/vin_report_model';
import { getPaidReport } from 'mocks/vinReport/paidReport';
import { toCamel } from 'snake-camel';
import { CheapeningGraph } from './Creator';
import { createHelpers } from '../../Main';
import { CheapeningGraphContentsNode } from './Contents';

function getPaidReportMock(): RawVinReport {
    return toCamel(getPaidReport()) as unknown as RawVinReport;
}

it('Нет ячейки, если процент падения стоимости не пришел', () => {
    const report = getPaidReportMock();
    report.cheapeningGraph!.cheapeningGraphData = undefined;

    const helpers = createHelpers(report);
    const nodes = new CheapeningGraph().cellNodes(report, helpers);
    expect(nodes).toEqual([]);
});

it('Нет ноды, если процент падения стоимости не пришел', () => {
    const report = getPaidReportMock();
    report.cheapeningGraph!.cheapeningGraphData = undefined;

    const nodes = new CheapeningGraphContentsNode();
    expect(nodes).toEqual({});
});

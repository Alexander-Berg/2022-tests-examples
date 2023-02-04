import type { RawVinReport } from '@vertis/schema-registry/ts-types/auto/api/vin/vin_report_model';
import { getPaidReport } from 'mocks/vinReport/paidReport';
import { toCamel } from 'snake-camel';
import { createHelpers } from '../../Main';
import { Score } from './Creator';
import { Status } from '@vertis/schema-registry/ts-types/auto/api/vin/vin_resolution_enums';

function getPaidReportMock(): RawVinReport {
    return toCamel(getPaidReport()) as unknown as RawVinReport;
}

it('Текст, если отчет не куплен', () => {
    const report = getPaidReportMock();
    report.healthScore!.status = Status.LOCKED;

    const helpers = createHelpers(report);
    const nodes = new Score().cellNodes(report, helpers);
    expect(nodes).toMatchSnapshot('ScoreCell_Locked');
});

it('Текст, если отчет куплен', () => {
    const report = getPaidReportMock();

    const helpers = createHelpers(report);
    const nodes = new Score().cellNodes(report, helpers);
    expect(nodes).toMatchSnapshot('ScoreCell_Unlocked');
});

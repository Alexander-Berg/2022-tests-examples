import type { RawVinReport } from '@vertis/schema-registry/ts-types/auto/api/vin/vin_report_model';
import { toCamel } from 'snake-camel';
import { getPaidReport } from 'mocks/vinReport/paidReport';
import { yogaToXml } from 'app/server/tmpl/XMLBuilder';
import { getRandomInt } from '../../helpers/SharedCode';
import { Programs } from './Creator';
import { createHelpers } from '../../Main';

function getPaidReportMock(): RawVinReport {
    return toCamel(getPaidReport()) as unknown as RawVinReport;
}

function snapshot(report: RawVinReport): string {
    const helpers = createHelpers(report);
    const nodes = new Programs().cellNodes(report, helpers);
    return yogaToXml(nodes);
}

it('Нет блока, если нет записей и блок не обновляется', () => {
    const report = getPaidReportMock();
    delete report.programs;
    const xml = snapshot(report);
    expect(xml).toMatchSnapshot('no block');
});

it('Заглушка обновления, если нет записей', () => {
    const report = getPaidReportMock();
    report.programs!.programRecords = [];
    report.programs!.header!.isUpdating = true;
    getRandomInt(0);
    const xml = snapshot(report);
    expect(xml).toMatchSnapshot('no records');
});

it('ProgramsCell', () => {
    const report = getPaidReportMock();
    const xml = snapshot(report);
    expect(xml).toMatchSnapshot('no records');
});

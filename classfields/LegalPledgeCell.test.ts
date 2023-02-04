import type { RawVinReport } from '@vertis/schema-registry/ts-types/auto/api/vin/vin_report_model';
import { getPaidReport } from 'mocks/vinReport/paidReport';
import { toCamel } from 'snake-camel';
import { jpath } from 'nommon';
import { createHelpers } from '../../Main';
import { Pledge } from './Creator';
import { ReportType, Status } from '@vertis/schema-registry/ts-types/auto/api/vin/vin_resolution_enums';

function getPaidReportMock(): RawVinReport {
    return toCamel(getPaidReport()) as unknown as RawVinReport;
}

it('Нет блока, если статус LOCKED', () => {
    const report = getPaidReportMock();
    report.pledge!.status = Status.LOCKED;

    const helpers = createHelpers(report);
    const nodes = new Pledge().cellNodes(report, helpers);
    expect(nodes).toEqual([]);
});

it('Есть НБКИ для бесплатного со статусом OK', () => {
    const report = getPaidReportMock();
    report.reportType = ReportType.FREE_REPORT;
    report.pledge!.status = Status.OK;

    const helpers = createHelpers(report);
    const nodes = new Pledge().cellNodes(report, helpers);
    const nbkiIconNode = jpath('.[3].children[0]', nodes);
    const nbkiTextNode = jpath('.[3].children[1].children[0]', nodes);
    expect(nbkiIconNode).toHaveProperty('url', 'https://yastatic.net/s3/vertis-frontend/autoru-mobile/carfax-icons/ios/locker-ios.png');
    expect(nbkiTextNode).toHaveProperty('text', 'Данные НБКИ');
});

it('Старая логика', () => {
    const report = getPaidReportMock();

    const helpers = createHelpers(report);
    const nodes = new Pledge().cellNodes(report, helpers);
    expect(nodes).toMatchSnapshot('LegalPledge_paid');
});

it('Находится в залоге', () => {
    const report = getPaidReportMock();
    report.pledge!.status = Status.ERROR;

    const helpers = createHelpers(report);
    const nodes = new Pledge().cellNodes(report, helpers);
    expect(nodes).toMatchSnapshot('LegalPledge_locked');
});

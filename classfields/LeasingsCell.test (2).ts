/* eslint-disable @typescript-eslint/no-use-before-define */
/* eslint-disable max-len */
import type { LeasingsBlock, RawVinReport } from '@vertis/schema-registry/ts-types/auto/api/vin/vin_report_model';
import { yogaToXml } from 'app/server/tmpl/XMLBuilder';
import type { YogaNode } from 'app/server/tmpl/YogaNode';
import { getPaidReport } from 'mocks/vinReport/paidReport';
import { toCamel } from 'snake-camel';
import { createHelpers } from '../../Main';
import { Leasings } from './Creator';

function cellXml(report: RawVinReport) {
    const helpers = createHelpers(report);
    const nodes = new Leasings().cellNodes(report, helpers);
    return yogaToXml(nodes);
}

function timelineNodeXml(report: RawVinReport) {
    const history = report.history;
    if (history === undefined) {
        return [];
    }

    const nodes: Array<YogaNode> = [];
    const helpers = createHelpers(report);
    history.owners.forEach((owner) => {
        owner.historyRecords?.forEach((record) => {
            nodes.push(...new Leasings().timelineNodes(record, helpers));
        });
    });

    return yogaToXml(nodes);
}

function getPaidReportMock(): RawVinReport {
    return toCamel(getPaidReport()) as unknown as RawVinReport;
}

function emptyStateXml(updating: boolean): string {
    const report = getPaidReportMock();
    report.leasings = { header: { isUpdating: updating } } as LeasingsBlock;
    return cellXml(report);
}

it('Пустой стейт (обновляется)', () => {
    expect(emptyStateXml(true)).toMatchSnapshot('LeasingsCell empty (updating)');
});

it('Пустой стейт (не обновляется)', () => {
    expect(emptyStateXml(false)).toMatchSnapshot('LeasingsCell empty (not updating)');
});

it('Блок', () => {
    const report = getPaidReportMock();
    const xml = cellXml(report);
    expect(xml).toMatchSnapshot('LeasingsCell');
});

it('Таймлайн', () => {
    const report = getPaidReportMock();
    const xml = timelineNodeXml(report);
    expect(xml).toMatchSnapshot('LeasingsCell (Timeline)');
});

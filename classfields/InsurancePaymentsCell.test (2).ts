import type { HistoryBlock_HistoryRecord, InsurancePaymentBlock, RawVinReport } from '@vertis/schema-registry/ts-types/auto/api/vin/vin_report_model';
import { InsuranceType } from '@vertis/schema-registry/ts-types/auto/api/vin/vin_report_model';
import { toCamel } from 'snake-camel';
import { getPaidReport } from 'mocks/vinReport/paidReport';
import { yogaToXml } from 'app/server/tmpl/XMLBuilder';
import { InsurancePayments } from './Creator';
import { createHelpers } from '../../Main';

function cellXml(report: RawVinReport) {
    const helpers = createHelpers(report);
    const nodes = new InsurancePayments().cellNodes(report, helpers);
    return yogaToXml(nodes);
}

function timelineNodeXml(report: RawVinReport) {
    const helpers = createHelpers(report);
    return report.history!.owners[0].historyRecords.map((record) => {
        const nodes = new InsurancePayments().timelineNodes(record, helpers);
        return yogaToXml(nodes);
    });
}

function getPaidReportMock(): RawVinReport {
    return toCamel(getPaidReport()) as unknown as RawVinReport;
}

it('Пустой стейт (обновляется)', () => {
    const report = getPaidReportMock();
    report.insurancePayments = { header: { isUpdating: true } } as InsurancePaymentBlock;

    const xml = cellXml(report);
    expect(xml).toMatchSnapshot('empty (updating)');
});

it('Пустой стейт (не обновляется)', () => {
    const report = getPaidReportMock();
    report.insurancePayments = { header: { isUpdating: false } } as InsurancePaymentBlock;

    const xml = cellXml(report);
    expect(xml).toMatchSnapshot('empty (not updating)');
});

it('Блок', () => {
    const report = getPaidReportMock();
    report.insurancePayments = {
        header: {
            title: 'Выплаты страховых',
            timestampUpdate: '1633557298',
            isUpdating: false,
        },
        payments: [
            {
                date: '1633557298',
                amount: 123456789,
            },
            {
                date: '1633557297',
                amount: 123456789,
                policyInfo: {
                    insurerName: 'insurerName',
                    insuranceType: InsuranceType.KASKO,
                    from: '1633557297',
                    to: '1633557298',
                },
            },
            {
                date: '1633557296',
                amount: 123456789,
                policyInfo: {
                    insurerName: 'insurerName',
                    insuranceType: InsuranceType.OSAGO,
                    from: '1633557296',
                    to: '1633557297',
                },
            },
        ],
    } as InsurancePaymentBlock;

    const xml = cellXml(report);
    expect(xml).toMatchSnapshot('InsurancePaymentsCell');
});

it('Таймлайн', () => {
    const report = getPaidReportMock();
    report.history!.owners[0].historyRecords = [
        {
            insurancePaymentRecord: {
                date: '1633557298',
                amount: 123456789,
            },
        },
        {
            insurancePaymentRecord: {
                date: '1633557297',
                amount: 123456789,
                policyInfo: {
                    insurerName: 'insurerName',
                    insuranceType: InsuranceType.KASKO,
                    from: '1633557297',
                    to: '1633557298',
                },
            },
        },
        {
            insurancePaymentRecord: {
                date: '1633557296',
                amount: 123456789,
                policyInfo: {
                    insurerName: 'insurerName',
                    insuranceType: InsuranceType.OSAGO,
                    from: '1633557296',
                    to: '1633557297',
                },
            },
        },
    ] as Array<HistoryBlock_HistoryRecord>;

    timelineNodeXml(report).forEach((xml) => {
        expect(xml).toMatchSnapshot('InsurancePaymentsCell (Timeline)');
    });
});

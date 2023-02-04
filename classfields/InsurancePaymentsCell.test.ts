import { createHelpers, makeFullReport } from 'app/server/tmpl/android/v1/Main';
import { AppConfig } from 'app/server/tmpl/android/v1/helpers/AppConfig';
import type { Request } from 'express';
import type { InsurancePaymentBlock, InsurancePaymentItem, RawVinReport } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { InsuranceType } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { response200ForVin } from 'app/server/resources/publicApi/methods/getCarfaxOfferReportRaw.fixtures';
import { yogaToXml } from 'app/server/tmpl/XMLBuilder';
import { get } from 'lodash';

let req: Request;
beforeEach(() => {
    req = {
        headers: {
            'x-android-app-version': '10.0.0',
        },
    } as unknown as Request;
    jest.spyOn(Math, 'random').mockReturnValue(0);
});

describe('insurance payment cell', () => {
    it('block not exists', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.insurance_payments = undefined;

        const result = render(report, req);
        const insurancePayments = result.find((node) => node.id === 'insurance_payments');

        expect(insurancePayments).toBeUndefined();
    });

    it('block not ready', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.insurance_payments = buildInsurancePaymentsItems(true, true);

        const result = render(report, req);
        const insurancePayments = result.find((node) => node.id === 'insurance_payments');

        expect(get(insurancePayments, 'children[1].children[0].url')).toContain('content-loading-1');
    });

    it('block empty', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.insurance_payments = buildInsurancePaymentsItems(false, true);

        const result = render(report, req);
        const insurancePayments = result.find((node) => node.id === 'insurance_payments');

        expect(get(insurancePayments, 'children[1].children[0].children[0].children[1].text'))
            .toContain('По данным Uremont страховых выплат не было');
    });

    it('block exist', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.insurance_payments = buildInsurancePaymentsItems(false, false);

        const result = render(report, req);
        const insurancePayments = result.find((node) => node.id === 'insurance_payments');
        const xml = insurancePayments ? yogaToXml([ insurancePayments ]) : '';

        expect(insurancePayments).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });
});

function render(report: RawVinReport, req: Request) {
    const appConfig = new AppConfig({}, req);
    const helpers = createHelpers(report, appConfig);
    return makeFullReport(report, helpers);
}

function buildInsurancePaymentsItems(isUpdating: boolean, isEmpty: boolean): InsurancePaymentBlock {
    let records: Array<InsurancePaymentItem> = [];
    if (!isEmpty) {
        records = [
            {
                date: '1519851600000',
                amount: 49524,
                policy_info: {
                    insurer_name: 'ИНГОССТРАХ',
                    insurance_type: InsuranceType.OSAGO,
                },
            } as InsurancePaymentItem,
            {

                date: '1519851600000',
                amount: 49524,
                policy_info: {
                    insurer_name: 'ИНГОССТРАХ',
                    insurance_type: InsuranceType.KASKO,
                },

            } as InsurancePaymentItem,
            {

                date: '1519851600000',
                amount: 49524,
                policy_info: {
                    insurer_name: 'ИНГОССТРАХ',
                    insurance_type: InsuranceType.OSAGO,
                },
            } as InsurancePaymentItem,
        ];
    }
    return {
        header: {
            title: 'Страховки',
            is_updating: isUpdating,
        },
        payments: records,
    } as InsurancePaymentBlock;
}

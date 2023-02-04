import { createHelpers, makeFullReport } from 'app/server/tmpl/android/v1/Main';
import { AppConfig } from 'app/server/tmpl/android/v1/helpers/AppConfig';
import type { Request } from 'express';
import type { InsurancesBlock, RawVinReport } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { InsuranceStatus, InsuranceType } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { response200ForVin } from 'app/server/resources/publicApi/methods/getCarfaxOfferReportRaw.fixtures';
import { Status } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_resolution_enums';
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

describe('insurance cell', () => {
    it('block not exists', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.insurances = undefined;

        const result = render(report, req);
        const insurance = result.find((node) => node.id === 'insurance');

        expect(insurance).toBeUndefined();
    });

    it('block not ready', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.insurances = buildInsuranceItems(true, true);

        const result = render(report, req);
        const insurance = result.find((node) => node.id === 'insurances');

        expect(get(insurance, 'children[1].children[0].url')).toContain('content-loading-1');
    });

    it('block exist', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.insurances = buildInsuranceItems(false, false);

        const result = render(report, req);
        const insurances = result.find((node) => node.id === 'insurances');
        const xml = insurances ? yogaToXml([ insurances ]) : '';

        expect(insurances).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });

    it('block empty', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.insurances = buildInsuranceItems(false, true);

        const result = render(report, req);
        const insurances = result.find((node) => node.id === 'insurances');

        expect(get(insurances, 'children[1].children[0].children[0].text'))
            .toContain('Данные о страховых полисах не найдены');
    });
});

function render(report: RawVinReport, req: Request) {
    const appConfig = new AppConfig({}, req);
    const helpers = createHelpers(report, appConfig);
    return makeFullReport(report, helpers);
}

function buildInsuranceItems(isUpdating: boolean, isEmpty: boolean): InsurancesBlock {
    const insurances = [];
    if (!isEmpty) {
        insurances.push(
            {
                date: '1617092491774',
                insurance_type: InsuranceType.KASKO,
                from: '1617192691774',
                to: '1617292691774',
                region_name: 'Саратов',
                partner_name: 'Яндекс',
                partner_url: '',
                mileage_status: Status.OK,
                serial: '12345',
                number: '54321',
                insurer_name: 'Я.Страхование',
                insurance_status: InsuranceStatus.EXPIRED,
                policy_status: 'Ждет одобрения',
                mileage: 100500,
            },
        );
    }
    return {
        header: {
            title: 'Страховые полисы',
            timestamp_update: '1617092491774',
            is_updating: isUpdating,
        },
        insurances: insurances,
        status: Status.OK,
        record_count: 2,
        comments_count: 0,
    };
}

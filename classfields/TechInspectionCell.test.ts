import { createHelpers, makeFullReport } from 'app/server/tmpl/android/v1/Main';
import { AppConfig } from 'app/server/tmpl/android/v1/helpers/AppConfig';
import type { Request } from 'express';
import type { RawVinReport, TechInspectionBlock, TechInspectionRecord } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
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

describe('tech inspection cell', () => {
    it('block not exists', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.tech_inspection_block = undefined;

        const result = render(report, req);
        const insurance = result.find((node) => node.id === 'tech_inspection');

        expect(insurance).toBeUndefined();
    });

    it('block not ready', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.tech_inspection_block = buildTechInspectionItems(true, true);

        const result = render(report, req);
        const insurance = result.find((node) => node.id === 'tech_inspection');

        expect(get(insurance, 'children[1].children[0].url')).toContain('content-loading-1');
    });

    it('block exist', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.tech_inspection_block = buildTechInspectionItems(false, false);

        const result = render(report, req);
        const techInspection = result.find((node) => node.id === 'tech_inspection');
        const xml = techInspection ? yogaToXml([ techInspection ]) : '';

        expect(techInspection).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });

    it('block empty', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.tech_inspection_block = buildTechInspectionItems(false, true);

        const result = render(report, req);
        const techInspection = result.find((node) => node.id === 'tech_inspection');

        expect(get(techInspection, 'children[1].children[0].children[1].text'))
            .toContain('Данные по\u00A0ТО не\u00A0найдены');
    });
});

function render(report: RawVinReport, req: Request) {
    const appConfig = new AppConfig({}, req);
    const helpers = createHelpers(report, appConfig);
    return makeFullReport(report, helpers);
}

function buildTechInspectionItems(isUpdating: boolean, isEmpty: boolean): TechInspectionBlock {
    let records: Array<TechInspectionRecord> = [];
    if (!isEmpty) {
        records = [
            {
                timestamp: '1554411600000',
                diagnostic_card_number: '084800041902754',
                mileage: 123,
                mileage_status: Status.OK,
                valid_until_timestamp: '1617570000000',
            },
            {
                timestamp: '1554930000000',
                diagnostic_card_number: '087560011906031',
                mileage_status: Status.OK,
                mileage: 148,
                valid_until_timestamp: '1618088400000',
            },
            {
                timestamp: '1595883600000',
                diagnostic_card_number: '086320032004607',
                mileage_status: Status.OK,
                mileage: 145,
                valid_until_timestamp: '1658955600000',
            },
        ];
    }
    return {
        header: {
            title: 'Техосмотры',
            timestamp_update: '1573629066575',
            is_updating: isUpdating,
        },
        records: records,
    } as unknown as TechInspectionBlock;
}

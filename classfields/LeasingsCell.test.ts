import { createHelpers, makeFullReport } from 'app/server/tmpl/android/v1/Main';
import { AppConfig } from 'app/server/tmpl/android/v1/helpers/AppConfig';
import type { Request } from 'express';
import type { HistoryBlock, LeasingItem, LeasingsBlock, RawVinReport } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';

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

describe('estimate_cell', () => {
    it('block not exists', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.leasings = undefined;

        const result = render(report, req);
        const leasingCell = result.find((node) => node.id === 'leasings');

        expect(leasingCell).toBeUndefined();
    });

    it('block not ready', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.leasings = buildLeasingsItems(true, false);

        const result = render(report, req);
        const leasingCell = result.find((node) => node.id === 'leasings');

        expect(get(leasingCell, 'children[1].children[0].url')).toContain('content-loading-1');
    });

    it('block empty', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.leasings = buildLeasingsItems(false, true);

        const result = render(report, req);
        const leasingCell = result.find((node) => node.id === 'leasings');

        expect(get(leasingCell, 'children[1].children[1].children[0].text')).toContain('Автомобиль не обнаружен в договорах лизинга');
    });

    it('block exist', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.leasings = buildLeasingsItems(false, false);

        const result = render(report, req);
        const leasingCell = result.find((node) => node.id === 'leasings');
        const xml = leasingCell ? yogaToXml([ leasingCell ]) : '';

        expect(leasingCell).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });

    it('check leasings block in timeline', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.history = timelineWithLeasingsRecord();

        const result = render(report, req);
        const timelineCell = result.find((node) => node.id === 'history');
        const xml = timelineCell ? yogaToXml([ timelineCell ]) : '';

        expect(timelineCell).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });
});

function render(report: RawVinReport, req: Request) {
    const appConfig = new AppConfig({}, req);
    const helpers = createHelpers(report, appConfig);
    return makeFullReport(report, helpers);
}

function buildLeasingsItems(isUpdating: boolean, isEmpty: boolean): LeasingsBlock {
    const records: Array<LeasingItem> = [];
    if (!isEmpty) {
        records.push(...[
            {
                date_from: '1436734800000',
                date_to: '1538341200000',
                lessees: [ 'ООО "М-Профи"' ],
            },
            {
                date_from: '1596229200000',
                date_to: '1685480400000',
                lessees: [ 'ООО "НПП "ИРТЭК"' ],
            },
        ]);
    }

    return {
        header: {
            title: 'Договор лизинга',
            timestamp_update: '1617092491774',
            is_updating: isUpdating,
        },
        leasings_records: records,
        status: Status.OK,
        record_count: 2,
        comments_count: 0,
    };
}


function timelineWithLeasingsRecord() {
    return {
        header: {
            title: 'История эксплуатации',
            timestamp_update: '1608560284675',
            is_updating: false,
        },
        owners: [
            {
                owner: {
                    index: 0,
                    owner_type: {
                        region: 'Эстония',
                    },
                    time_from: 1540155600000,
                    time_to: 1543611600000,
                    registration_status: 'NOT_REGISTERED',
                },
                history_records: [
                    {
                        leasing_record: {
                            date_from: '1436734800000',
                            date_to: '1538341200000',
                            lessees: [ 'ООО "М-Профи"' ],
                        },
                    },
                    {
                        leasing_record: {
                            date_from: '1596229200000',
                            date_to: '1685480400000',
                            lessees: [ 'ООО "НПП "ИРТЭК"' ],
                        },
                    },
                ],
            },
        ],
    } as unknown as HistoryBlock;
}

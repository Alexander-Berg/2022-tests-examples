import { createHelpers, makeFullReport } from 'app/server/tmpl/android/v1/Main';
import { AppConfig } from 'app/server/tmpl/android/v1/helpers/AppConfig';
import type { Request } from 'express';
import type { PledgeBlock, RawVinReport } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { response200ForVin } from 'app/server/resources/publicApi/methods/getCarfaxOfferReportRaw.fixtures';
import { yogaToXml } from 'app/server/tmpl/XMLBuilder';
import { get } from 'lodash';
import { ReportType, Status } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_resolution_enums';

let req: Request;
beforeEach(() => {
    req = {
        headers: {
            'x-android-app-version': '10.0.0',
        },
    } as unknown as Request;
    jest.spyOn(Math, 'random').mockReturnValue(0);
});

describe('pledge cell', () => {
    it('block not exists', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.pledge = undefined;

        const result = render(report, req);
        const pledge = result.find((node) => node.id === 'pledge');

        expect(pledge).toBeUndefined();
    });

    it('block not ready', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.pledge = buildPledgeItem(true);

        const result = render(report, req);
        const pledge = result.find((node) => node.id === 'pledge');

        expect(get(pledge, 'children[1].children[0].url')).toContain('content-loading-1');
    });

    it('NBKI error', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.pledge = buildPledgeItem(false, Status.ERROR);

        const result = render(report, req);
        const pledge = result.find((node) => node.id === 'pledge');
        const xml = pledge ? yogaToXml([ pledge ]) : '';

        expect(pledge).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });

    it('NBKI ok', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.pledge = buildPledgeItem(false, Status.OK);
        report.request_info = {
            is_dealer: false,
            is_active_offer_owner: true,
            is_moderator: false,
        };
        report.report_type = ReportType.FREE_REPORT;

        const result = render(report, req);
        const pledge = result.find((node) => node.id === 'pledge');
        const xml = pledge ? yogaToXml([ pledge ]) : '';

        expect(pledge).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });

    it('block not exists if status locked', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.pledge = buildPledgeItem(false, Status.LOCKED);
        report.request_info = {
            is_dealer: false,
            is_active_offer_owner: true,
            is_moderator: false,
        };
        report.report_type = ReportType.FREE_REPORT;

        const result = render(report, req);
        const pledge = result.find((node) => node.id === 'pledge');

        expect(pledge).toBeUndefined();
    });
});

function render(report: RawVinReport, req: Request) {
    const appConfig = new AppConfig({}, req);
    const helpers = createHelpers(report, appConfig);
    return makeFullReport(report, helpers);
}

function buildPledgeItem(
    isUpdating: boolean,
    status?: Status,
    nbkiStatus?: Status,
    fnpStatus?: Status,
): PledgeBlock {
    return {
        header: {
            title: 'Залоги',
            timestamp_update: '1617092491774',
            is_updating: isUpdating,
        },
        status: status,
        nbki_status: nbkiStatus,
        fnp_status: fnpStatus,
    } as PledgeBlock;
}

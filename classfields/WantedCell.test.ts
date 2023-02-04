import { createHelpers, makeFullReport } from 'app/server/tmpl/android/v1/Main';
import { AppConfig } from 'app/server/tmpl/android/v1/helpers/AppConfig';
import type { Request } from 'express';
import type { RawVinReport, WantedBlock } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { response200ForVin } from 'app/server/resources/publicApi/methods/getCarfaxOfferReportRaw.fixtures';
import { yogaToXml } from 'app/server/tmpl/XMLBuilder';
import { get } from 'lodash';
import { Status } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_resolution_enums';

let req: Request;
beforeEach(() => {
    req = {
        headers: {
            'x-android-app-version': '10.0.0',
        },
    } as unknown as Request;
    jest.spyOn(Math, 'random').mockReturnValue(0);
});

describe('wanted cell', () => {
    it('block not exists', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.wanted = undefined;

        const result = render(report, req);
        const wanted = result.find((node) => node.id === 'wanted');

        expect(wanted).toBeUndefined();
    });

    it('block not ready', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.wanted = buildWantedItem(true);

        const result = render(report, req);
        const wanted = result.find((node) => node.id === 'wanted');

        expect(get(wanted, 'children[1].children[0].url')).toContain('content-loading-1');
    });

    it('block error', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.wanted = buildWantedItem(false, Status.ERROR);

        const result = render(report, req);
        const wanted = result.find((node) => node.id === 'wanted');
        const xml = wanted ? yogaToXml([ wanted ]) : '';

        expect(wanted).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });

    it('block ok', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.wanted = buildWantedItem(false, Status.OK);

        const result = render(report, req);
        const wanted = result.find((node) => node.id === 'wanted');
        const xml = wanted ? yogaToXml([ wanted ]) : '';

        expect(wanted).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });

    it('block with comment', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.wanted = buildWantedItem(false, Status.OK);
        report.wanted.commentable = {
            block_id: 'wanted',
            add_comment: true,
        };

        const result = render(report, req);
        const wanted = result.find((node) => node.id === 'wanted');
        const xml = wanted ? yogaToXml([ wanted ]) : '';

        expect(wanted).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });
});

function render(report: RawVinReport, req: Request) {
    const appConfig = new AppConfig({}, req);
    const helpers = createHelpers(report, appConfig);
    return makeFullReport(report, helpers);
}

function buildWantedItem(
    isUpdating: boolean,
    status?: Status,
): WantedBlock {
    return {
        header: {
            title: 'Угоны',
            timestamp_update: '1577916410649',
            is_updating: isUpdating,
        },
        status: status,
    } as WantedBlock;
}

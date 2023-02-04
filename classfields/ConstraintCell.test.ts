import { createHelpers, makeFullReport } from 'app/server/tmpl/android/v1/Main';
import { AppConfig } from 'app/server/tmpl/android/v1/helpers/AppConfig';
import type { Request } from 'express';
import type { ConstraintBlock, RawVinReport } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
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

describe('constraints cell', () => {
    it('block not exists', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.constraints = undefined;

        const result = render(report, req);
        const constraints = result.find((node) => node.id === 'constraints');

        expect(constraints).toBeUndefined();
    });

    it('block not ready', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.constraints = buildConstraintItem(true);

        const result = render(report, req);
        const constraints = result.find((node) => node.id === 'constraints');

        expect(get(constraints, 'children[1].children[0].url')).toContain('content-loading-1');
    });

    it('block error', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.constraints = buildConstraintItem(false, Status.ERROR);

        const result = render(report, req);
        const constraints = result.find((node) => node.id === 'constraints');
        const xml = constraints ? yogaToXml([ constraints ]) : '';

        expect(constraints).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });

    it('block ok', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.constraints = buildConstraintItem(false, Status.OK);

        const result = render(report, req);
        const constraints = result.find((node) => node.id === 'constraints');
        const xml = constraints ? yogaToXml([ constraints ]) : '';

        expect(constraints).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });
});

function render(report: RawVinReport, req: Request) {
    const appConfig = new AppConfig({}, req);
    const helpers = createHelpers(report, appConfig);
    return makeFullReport(report, helpers);
}

function buildConstraintItem(
    isUpdating: boolean,
    status?: Status,
): ConstraintBlock {
    return {
        header: {
            title: 'Ограничения',
            timestamp_update: '1577916410649',
            is_updating: isUpdating,
        },
        status: status,
    } as ConstraintBlock;
}

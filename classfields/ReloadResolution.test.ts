import { createHelpers, makeFullReport } from 'app/server/tmpl/android/v1/Main';
import { AppConfig } from 'app/server/tmpl/android/v1/helpers/AppConfig';
import type { Request } from 'express';
import type { RawVinReport } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { response200ForVin } from 'app/server/resources/publicApi/methods/getCarfaxOfferReportRaw.fixtures';
import { yogaToXml } from 'app/server/tmpl/XMLBuilder';
import { ReloadResolutionModel } from 'app/server/tmpl/android/v1/cells/reload_resolution/Model';
import type { AdditionalYogaLayoutData_ReloadData } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_api_model';

let req: Request;
beforeEach(() => {
    req = {
        headers: {
            'x-android-app-version': '10.0.0',
        },
    } as unknown as Request;
    jest.spyOn(Math, 'random').mockReturnValue(0);
});

describe('reload resolution cell', () => {
    const id = new ReloadResolutionModel().identifier;

    it('block not exists', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.reload_data = undefined;

        const result = render(report, req);
        const sources = result.find((node) => node.id === id);

        expect(sources).toBeUndefined();
    });

    it('block exists', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.reload_data = {
            id: '123',
            reload_params: {
                allow_reload: true,
                remaining_time_till_reload: '10000',
            },
        } as AdditionalYogaLayoutData_ReloadData;

        const result = render(report, req);
        const sources = result.find((node) => node.id === id);
        const xml = sources ? yogaToXml([ sources ]) : '';

        expect(sources).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });

    it('block with one hour await', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.reload_data = {
            id: '123',
            reload_params: {
                allow_reload: true,
                remaining_time_till_reload: '10000',
            },
        } as AdditionalYogaLayoutData_ReloadData;

        const result = render(report, req);
        const sources = result.find((node) => node.id === 'vReloadResolutionRequestedBlock');
        const xml = sources ? yogaToXml([ sources ]) : '';

        expect(sources).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });

});

function render(report: RawVinReport, req: Request) {
    const appConfig = new AppConfig({}, req);
    const helpers = createHelpers(report, appConfig);
    return makeFullReport(report, helpers);
}

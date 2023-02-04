import { createHelpers, makeFullReport } from 'app/server/tmpl/android/v1/Main';
import { AppConfig } from 'app/server/tmpl/android/v1/helpers/AppConfig';
import type { Request } from 'express';
import type { RawVinReport, ReportOfferInfo, RequestInfo } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { response200ForVin } from 'app/server/resources/publicApi/methods/getCarfaxOfferReportRaw.fixtures';
import { yogaToXml } from 'app/server/tmpl/XMLBuilder';
import { VinNotFoundModel } from 'app/server/tmpl/android/v1/cells/vin_not_found/Model';

let req: Request;
beforeEach(() => {
    req = {
        headers: {
            'x-android-app-version': '10.0.0',
        },
    } as unknown as Request;
    jest.spyOn(Math, 'random').mockReturnValue(0);
});

describe('vin not found cell', () => {
    const id = new VinNotFoundModel().identifier;

    it('block not exists', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.request_info = undefined;

        const result = render(report, req);
        const vinNotFound = result.find((node) => node.id === id);

        expect(vinNotFound).toBeUndefined();
    });

    it('block exists', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.request_info = { is_dealer: false, is_active_offer_owner: true } as RequestInfo;
        report.report_offer_info = { no_license_plate_photo: true } as ReportOfferInfo;

        const result = render(report, req);
        const vinNotFound = result.find((node) => node.id === id);
        const xml = vinNotFound ? yogaToXml([ vinNotFound ]) : '';

        expect(vinNotFound).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });

});

function render(report: RawVinReport, req: Request) {
    const appConfig = new AppConfig({}, req);
    const helpers = createHelpers(report, appConfig);
    return makeFullReport(report, helpers);
}

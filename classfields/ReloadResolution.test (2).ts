import { createHelpers } from 'app/server/tmpl/android/v1/Main';
import { AppConfig } from 'app/server/tmpl/android/v1/helpers/AppConfig';
import type { Request } from 'express';
import type { RawVinReport } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { response200ForVin } from 'app/server/resources/publicApi/methods/getCarfaxOfferReportRaw.fixtures';
import { OfferReportPreview } from 'app/server/tmpl/android/v1/OfferReportPreview';
import { ReportType } from 'app/server/tmpl/android/v1/models/Models';
import type { ReloadParams } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_api_model';
import { searchTree } from 'app/server/tmpl/android/v1/helpers/SharedCode';

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
    it('block not exists', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        const helpers = createHelpers(report, new AppConfig({}, req));
        const creator = new OfferReportPreview(report, true, ReportType.CARD_PREVIEW, helpers);
        const result = creator.getPaidCardPreviewBlock();
        const reloadResolution = searchTree(result, 'vReloadResolutionAllowed');

        expect(reloadResolution).toBeUndefined();
    });

    it('reload allowed', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        const reloadParams: ReloadParams = {
            allow_reload: true,
            remaining_time_till_reload: '',
        };
        const helpers = createHelpers(report, new AppConfig({}, req), undefined, undefined, reloadParams);
        const creator = new OfferReportPreview(report, true, ReportType.CARD_PREVIEW, helpers);
        const result = creator.getPaidCardPreviewBlock();
        const reloadResolution = searchTree(result, 'vReloadResolutionAllowed');

        expect(reloadResolution).toMatchSnapshot();
    });

    it('reload not allowed for many hours', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        const reloadParams: ReloadParams = {
            allow_reload: false,
            remaining_time_till_reload: '123123123123',
        };
        const helpers = createHelpers(report, new AppConfig({}, req), undefined, undefined, reloadParams);
        const creator = new OfferReportPreview(report, true, ReportType.CARD_PREVIEW, helpers);
        const result = creator.getPaidCardPreviewBlock();
        const reloadResolution = searchTree(result, 'tvRequestedInfo');

        expect(reloadResolution).toMatchSnapshot();
    });

    it('reload not allowed for 24 hours', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        const reloadParams: ReloadParams = { allow_reload: true } as ReloadParams;
        const helpers = createHelpers(report, new AppConfig({}, req), undefined, undefined, reloadParams);
        const creator = new OfferReportPreview(report, true, ReportType.CARD_PREVIEW, helpers);
        const result = creator.getPaidCardPreviewBlock();
        const reloadResolution = searchTree(result, 'tvRequestedInfo');

        expect(reloadResolution).toMatchSnapshot();
    });

});

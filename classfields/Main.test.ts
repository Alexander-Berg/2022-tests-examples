import type { RawVinReport } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import type { ResolutionBilling } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_resolution_model';
import type { ReloadParams } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_api_model';
import type { VinReportEnrichData } from 'app/server/blocks/getEnrichDataForVinReport';
import { response200ForVin } from 'app/server/resources/publicApi/methods/getCarfaxOfferReportRaw.fixtures';
import { AppConfig } from 'app/server/tmpl/android/v1/helpers/AppConfig';
import { createHelpers } from 'app/server/tmpl/android/v1/Main';
import type { Request } from 'express';
import type { Helpers } from './models/Models';

let req: Request;
beforeEach(() => {
    req = {
        headers: {
            'x-android-app-version': '10.0.0',
        },
    } as unknown as Request;
    jest.spyOn(Math, 'random').mockReturnValue(0);
});
describe('creating helpers', () => {
    describe('creating additional data', () => {
        describe('creating report offer info', () => {
            it('should not create report offer info', () => {
                const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
                report.report_offer_info = undefined;
                const helpers = createDefaultHelpers(report);

                expect(helpers.additionalData).not.toBeUndefined();
                expect(helpers.additionalData.offerInfo).toBeUndefined();
            });

            it('should copy report offer info', () => {
                const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
                const offerInfo = {
                    offer_id: 'SOME_ID',
                    no_license_plate_photo: false,
                    is_favorite: true,
                };
                report.report_offer_info = offerInfo;
                const helpers = createDefaultHelpers(report);

                expect(helpers.additionalData).not.toBeUndefined();
                expect(helpers.additionalData.offerInfo).toMatchObject(offerInfo);
            });

        });
    });
});

function createDefaultHelpers(
    report?: RawVinReport,
    appConfig?: AppConfig,
    billing?: ResolutionBilling,
    enrichData?: VinReportEnrichData,
    reloadParams?: ReloadParams,
): Helpers {
    const defaultReport = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
    const defaultConfig = new AppConfig({}, req);
    return createHelpers(report ?? defaultReport, appConfig ?? defaultConfig, billing, enrichData, reloadParams);
}

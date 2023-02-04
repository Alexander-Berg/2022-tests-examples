import { createHelpers, makeFullReport } from 'app/server/tmpl/android/v1/Main';
import { AppConfig } from 'app/server/tmpl/android/v1/helpers/AppConfig';
import type { Request } from 'express';
import type { RawVinReport, VehiclePhotoItem, VehiclePhotos } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
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

describe('vehicle photos cell', () => {
    it('block not exists', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.vehicle_photos = undefined;

        const result = render(report, req);
        const vehiclePhotos = result.find((node) => node.id === 'vehicle_photos');

        expect(vehiclePhotos).toBeUndefined();
    });

    it('block not ready', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.vehicle_photos = getVehiclePhotos(true);

        const result = render(report, req);
        const vehiclePhotos = result.find((node) => node.id === 'vehicle_photos');

        expect(get(vehiclePhotos, 'children[1].children[0].url')).toContain('content-loading-1');
    });

    it('block exist', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.vehicle_photos = getVehiclePhotos(false);

        const result = render(report, req);
        const vehiclePhotos = result.find((node) => node.id === 'vehicle_photos');
        const xml = vehiclePhotos ? yogaToXml([ vehiclePhotos ]) : '';

        expect(vehiclePhotos).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });
});

function render(report: RawVinReport, req: Request) {
    const appConfig = new AppConfig({}, req);
    const helpers = createHelpers(report, appConfig);
    return makeFullReport(report, helpers);
}

function getVehiclePhotos(isUpdating: boolean): VehiclePhotos {
    let records: Array<VehiclePhotoItem> = [];
    if (!isUpdating) {
        records = [
            {
                date: '1430341200000',
                gallery: [
                    {
                        name: 'autoru-carfax:2926091-Fy4IoqKAXEuQsw2QU4AKFvwmZLbRNpbxz',
                        sizes: {
                            thumb_m: '//images.mds-proxy.test.avto.ru/get-autoru-carfax/2926091/Fy4IoqKAXEuQsw2QU4AKFvwmZLbRNpbxz/thumb_m',
                        },
                    },
                ],
            } as unknown as VehiclePhotoItem,
            {
                date: '1560632400000',
                gallery: [
                    {
                        name: 'autoru-carfax:2926091-qcVc4e1hyd45mvmpsdvQHYYsr2p84FLqr',
                        sizes: {
                            thumb_m: '//images.mds-proxy.test.avto.ru/get-autoru-carfax/2926091/qcVc4e1hyd45mvmpsdvQHYYsr2p84FLqr/thumb_m',
                        },
                    },
                ],
            } as unknown as VehiclePhotoItem,
        ];
    }
    return {
        comments_count: 0,
        header: {
            title: 'Фотографии автолюбителей',
            timestamp_update: '1643897998077',
            is_updating: isUpdating,
        },
        records: records,
        status: Status.OK,
        record_count: 2,
    } as unknown as VehiclePhotos;
}

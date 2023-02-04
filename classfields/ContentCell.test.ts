import { createHelpers, makeFullReport } from 'app/server/tmpl/android/v1/Main';
import { AppConfig } from 'app/server/tmpl/android/v1/helpers/AppConfig';
import type { Request } from 'express';
import type { PhotoBlock, RawVinReport } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { response200ForVin } from 'app/server/resources/publicApi/methods/getCarfaxOfferReportRaw.fixtures';
import { yogaToXml } from 'app/server/tmpl/XMLBuilder';
import type { ResolutionBilling } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_resolution_model';
import type { Photo } from '@vertis/schema-registry/ts-types-snake/auto/api/common_model';
import { PaidReason } from '@vertis/schema-registry/ts-types-snake/auto/api/common_model';
import type { PaidServicePrice } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';
import { buildEquipmentItem } from 'app/server/tmpl/android/v1/cells/equipment/EquipmentCell.test';

let req: Request;
beforeEach(() => {
    req = {
        headers: {
            'x-android-app-version': '10.0.0',
        },
    } as unknown as Request;
    jest.spyOn(Math, 'random').mockReturnValue(0);
});

describe('content cell', () => {

    it('block exist', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;

        report.photo_block = buildPhotoBlock();
        report.car_info = buildEquipmentItem(false);

        const result = render(report, req);
        const content = result.find((node) => node.id === 'content_block');
        const xml = content ? yogaToXml([ content ]) : '';

        expect(content).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });

});

function render(report: RawVinReport, req: Request) {
    const appConfig = new AppConfig({}, req);
    const billing = getBillingWithDiscount();
    const helpers = createHelpers(report, appConfig, billing);
    return makeFullReport(report, helpers);
}

function getBillingWithDiscount(): ResolutionBilling {
    return {
        quota_left: 0,
        service_prices: [
            {
                service: 'offers-history-reports',
                days: 365,
                price: 147,
                currency: 'RUR',
                paid_reason: PaidReason.FREE_LIMIT,
                recommendation_priority: 0,
                aliases: [ 'offers-history-reports-1' ],
                need_confirm: false,
                counter: '1',
                purchase_forbidden: false,
            } as PaidServicePrice,
            {
                service: 'offers-history-reports',
                days: 365,
                price: 599,
                currency: 'RUR',
                paid_reason: PaidReason.FREE_LIMIT,
                recommendation_priority: 0,
                aliases: [ 'offers-history-reports-5' ],
                need_confirm: false,
                counter: '5',
                purchase_forbidden: false,
            } as PaidServicePrice,
            {
                service: 'offers-history-reports',
                days: 365,
                price: 693,
                original_price: 990,
                currency: 'RUR',
                paid_reason: PaidReason.FREE_LIMIT,
                recommendation_priority: 0,
                aliases: [ 'offers-history-reports-10' ],
                need_confirm: false,
                counter: '10',
                purchase_forbidden: false,
            } as PaidServicePrice,
        ],
    };
}

function buildPhotoBlock(): PhotoBlock {
    return {
        photos: [
            {
                name: 'autoru-vos:3909203-67e8975bda8066d0e608b05b0be4367f',
                sizes: {
                    thumb_m: '//avatars.mds.yandex.net/get-autoru-vos/3909203/67e8975bda8066d0e608b05b0be4367f/thumb_m',
                },
                transform: {
                    angle: 0,
                    blur: true,
                },
                preview: {
                    version: 2,
                    width: 24,
                    height: 18,
                    // eslint-disable-next-line max-len
                    data: 'AAwDAQACEQMRAD8A7BPD6XUyw2+qX+pO8gYWN5eAQHoGY/Iei5I/L6em2/ildEsY7W10Z4ooR5YjiuIlDD1GH5zz1xXyR4j/AGg9B8DRf8TxNUsrq+ik+xXenlHWNhj5ZVb3KE4PQEVxfhn9onxFq9rJPcvaSRMcwMDKPMHPU+WQPTjNd0HFavcqpFy0ufU/jbQbfxFq9peRW76dYlf3qefGrJIMFSFUPnPf5hjHfNFfNPh39pfSlmu7XxZc3qagXjS0sdD2vvJJBV2lVTn7uMDjPfoCoqQg3dDg5pWPJfj/ABpJp53KrbRuXIzg+o968l8PfE/xlo2nR2Fh4t1yxsbZCsFtbalNHHECSSFUMAoySeO5NFFcszrj8S9DoPg/qF14n+LdrqGsXM2rX8svmyXV9IZpXcDhizZJPA5ooopmC2P/2Q==',
                },
                namespace: 'autoru-vos',
            } as unknown as Photo,
        ],
    } as PhotoBlock;
}

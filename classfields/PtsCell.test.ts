import { createHelpers, makeFullReport } from 'app/server/tmpl/android/v1/Main';
import { AppConfig } from 'app/server/tmpl/android/v1/helpers/AppConfig';
import type { Request } from 'express';
import type { PtsBlock, RawVinReport } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { PtsBlock_PtsType } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { response200ForVin } from 'app/server/resources/publicApi/methods/getCarfaxOfferReportRaw.fixtures';
import { yogaToXml } from 'app/server/tmpl/XMLBuilder';
import { Status } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_resolution_enums';
import type { Photo } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/common';
import type { VinReportComment } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/comments/comment_api_model';

let req: Request;
beforeEach(() => {
    req = {
        headers: {
            'x-android-app-version': '10.0.0',
        },
    } as unknown as Request;
    jest.spyOn(Math, 'random').mockReturnValue(0);
});

describe('pts cell', () => {
    it('block not exists', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.pts_info = undefined;

        const result = render(report, req);
        const pts = result.find((node) => node.id === 'pts');

        expect(pts).toBeUndefined();
    });

    it('block exists', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.pts_info = buildPtsItem(false);
        report.pts_info.pts_type = PtsBlock_PtsType.DIGITAL;
        report.comments = [
            {
                id: '65a70a4521cd41a2b4cc126f627c3116',
                block_id: 'pts',
                text: 'Это комментарий с тремя фотографиями и текстом, который не помещается в одну строку.',
                photos: [
                    {
                        mds_photo_info: {
                            namespace: 'autoru-reviews',
                            group_id: 69673,
                            name: 'd228e9ddda7df5c6de38ad38e1ed4102',
                        },
                        sizes: {
                            thumb_m: '//images.mds-proxy.test.avto.ru/get-autoru-reviews/69673/d228e9ddda7df5c6de38ad38e1ed4102/thumb_m',
                        },
                        is_deleted: false,
                    } as Photo,
                ],
                user: {
                    name: 'Продавец',
                    current_user_is_owner: true,
                },
                create_time: '1594240743610',
                update_time: '1594240751710',
                actions: {
                    edit: true,
                    'delete': true,
                },
                is_deleted: false,
            } as VinReportComment,
        ];

        const result = render(report, req);
        const pts = result.find((node) => node.id === 'pts');
        const xml = pts ? yogaToXml([ pts ]) : '';

        expect(pts).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });

});

function render(report: RawVinReport, req: Request) {
    const appConfig = new AppConfig({}, req);
    const helpers = createHelpers(report, appConfig);
    return makeFullReport(report, helpers);
}

function buildPtsItem(
    isUpdating: boolean,
): PtsBlock {
    return {
        header: {
            title: 'Данные из ПТС',
            is_updating: isUpdating,
            timestamp_update: '1595405273648',
        },
        vin: 'WBADE210X0BM83317',
        mark: {
            key: 'Марка',
            value: 'BMW',
            value_text: 'BMW',
            status: Status.OK,
        },
        color: {
            key: 'Цвет',
            value: 'Зеленый',
            value_text: 'Зеленый',
            status: Status.OK,
        },
        year: {
            key: 'Год выпуска',
            value: 1997,
            value_text: '1997 г.',
            status: Status.OK,
        },
        horse_power: {
            key: 'Мощность двигателя',
            value: 234,
            value_text: '234 л.с.',
            status: Status.OK,
        },
        displacement: {
            key: 'Объём двигателя',
            value: 3500,
            value_text: '3,5 л',
            status: Status.OK,
        },
        mark_logo: {
            name: 'standalone_preview_mark_icon',
            sizes: {
                orig: '//avatars.mds.yandex.net/get-verba/997355/2a00000170a99124c3a521a92b8cd4846660/dealer_logo',
            },
        },
        commentable: {
            block_id: 'pts',
            add_comment: false,
        },
        status: Status.OK,
    } as unknown as PtsBlock;
}

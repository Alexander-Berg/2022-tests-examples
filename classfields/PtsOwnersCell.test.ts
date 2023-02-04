import { createHelpers, makeFullReport } from 'app/server/tmpl/android/v1/Main';
import { AppConfig } from 'app/server/tmpl/android/v1/helpers/AppConfig';
import type { Request } from 'express';
import type { OwnerItem, OwnerType, PtsOwnersBlock, RawVinReport } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { OwnerItem_RegistrationStatus, OwnerType_Type } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { response200ForVin } from 'app/server/resources/publicApi/methods/getCarfaxOfferReportRaw.fixtures';
import { yogaToXml } from 'app/server/tmpl/XMLBuilder';
import { get } from 'lodash';
import { Status } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_resolution_enums';
import type { User, VinReportComment } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/comments/comment_api_model';
import type { Photo } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/common';

let req: Request;
beforeEach(() => {
    req = {
        headers: {
            'x-android-app-version': '10.0.0',
        },
    } as unknown as Request;
    jest.spyOn(Math, 'random').mockReturnValue(0);
});

describe('pts owners cell', () => {
    it('block not exists', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.pts_owners = undefined;

        const result = render(report, req);
        const owners = result.find((node) => node.id === 'owners_block');

        expect(owners).toBeUndefined();
    });

    it('block not ready', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.pts_owners = buildPtsOwnersItems(true, true);

        const result = render(report, req);
        const owners = result.find((node) => node.id === 'owners_block');

        expect(get(owners, 'children[1].children[0].url')).toContain('content-loading-1');
    });

    it('block empty', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.pts_owners = buildPtsOwnersItems(false, true);

        const result = render(report, req);
        const owners = result.find((node) => node.id === 'owners_block');

        expect(owners).toBeUndefined();
    });

    it('block exist', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.pts_owners = buildPtsOwnersItems(false, false);

        const result = render(report, req);
        const owners = result.find((node) => node.id === 'owners_block');
        const xml = owners ? yogaToXml([ owners ]) : '';

        expect(owners).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });

    it('block with comments', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.comments = [
            {
                id: '65a70a4521cd41a2b4cc126f627c3117',
                block_id: 'owners',
                text: 'коммент с одним фото',
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
                } as User,
                create_time: '1595405273648',
                update_time: '1595405273648',
                actions: {
                    edit: true,
                    'delete': true,
                },
                is_deleted: false,
            } as VinReportComment,
        ];
        report.pts_owners = buildPtsOwnersItems(false, false);
        report.pts_owners.commentable = {
            block_id: 'owners',
            add_comment: true,
        };

        const result = render(report, req);
        const owners = result.find((node) => node.id === 'owners_block');
        const xml = owners ? yogaToXml([ owners ]) : '';

        expect(owners).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });
});

function render(report: RawVinReport, req: Request) {
    const appConfig = new AppConfig({}, req);
    const helpers = createHelpers(report, appConfig);
    return makeFullReport(report, helpers);
}

function buildPtsOwnersItems(isUpdating: boolean, isEmpty: boolean): PtsOwnersBlock {
    let owners: Array<OwnerItem> = [];

    if (!isEmpty) {
        owners = [
            {
                index: 0,
                owner_type: {
                    type: OwnerType_Type.PERSON,
                    name: 'Физическое лицо',
                    region: 'Москва',
                } as OwnerType,
                time_from: new Date(2013, 8, 19).getTime().toString(),
                time_to: new Date(2013, 9, 25).getTime().toString(),
                registration_status: OwnerItem_RegistrationStatus.NOT_REGISTERED,
                reg_actions: [
                    {
                        date: '1374264000000',
                        region: 'РФ',
                        operation: 'Снятие с учета в связи с кражей или угоном',
                    },
                    {
                        date: '1374264000000',
                        region: 'РСФСР',
                        operation: 'Снятие с учета в связи с кражей или угоном',
                    },
                ],
            } as OwnerItem,
            {
                index: 0,
                owner_type: {
                    type: OwnerType_Type.LEGAL,
                    name: 'ООО Очень Юредическое лицо',
                    region: 'Москва',
                } as OwnerType,
                time_from: '1374264000000',
                time_to: '1412366400000',
                registration_status: OwnerItem_RegistrationStatus.REGISTERED,
                reg_actions: [
                    {
                        date: '1374264000000',
                        region: 'РФ',
                        operation: 'Снятие с учета в связи с кражей или угоном',
                    },
                    {
                        date: '1412366400000',
                        region: 'РСФСР',
                        operation: 'Снятие с учета в связи с кражей или угоном',
                    },
                ],
            } as OwnerItem,
            {
                index: 0,
                owner_type: {
                    type: OwnerType_Type.UNKNOWN_OWNER_TYPE,
                    name: 'Странное лицо',
                    region: 'Москва',
                } as OwnerType,
                time_from: '1374864000000',
                time_to: '1374864000000',
                registration_status: OwnerItem_RegistrationStatus.UNKNOWN_REGISTRATION_STATUS,
                reg_actions: [
                    {
                        date: '1412366400000',
                        region: 'РФ',
                        operation: 'Снятие с учета в связи с кражей или угоном',
                    },
                    {
                        date: '1412366400000',
                        region: 'РФ',
                        operation: 'Снятие с учета в связи с кражей или угоном',
                    },
                    {
                        date: '1412366400000',
                        region: 'РСФСР',
                        operation: 'Снятие с учета в связи с кражей или угоном',
                    },
                    {
                        date: '1374264000000',
                        region: 'РФ',
                        operation: 'Снятие с учета в связи с кражей или угоном',
                    },
                ],
            } as OwnerItem,
        ];
    }
    return {
        header: {
            title: isEmpty ? 'Владельцы' : '4 владельца по ПТС',
            timestamp_update: '1621252899016',
            is_updating: isUpdating,
        },
        owners: isEmpty ? undefined : owners,
        owners_count_status: Status.OK,
        owners_count_report: isEmpty ? undefined : 4,
    } as PtsOwnersBlock;
}

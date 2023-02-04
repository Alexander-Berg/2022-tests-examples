import { createHelpers, makeFullReport } from 'app/server/tmpl/android/v1/Main';
import { AppConfig } from 'app/server/tmpl/android/v1/helpers/AppConfig';
import type { Request } from 'express';
import type { CommentableItem, DtpBlock, DtpItem, RawVinReport } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { response200ForVin } from 'app/server/resources/publicApi/methods/getCarfaxOfferReportRaw.fixtures';
import { yogaToXml } from 'app/server/tmpl/XMLBuilder';
import { get } from 'lodash';
import { Status } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_resolution_enums';
import { GibbDamage_DamageType } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_resolution_model';
import { Damage_CarPart, Damage_DamageType } from '@vertis/schema-registry/ts-types-snake/auto/api/common_model';

let req: Request;
beforeEach(() => {
    req = {
        headers: {
            'x-android-app-version': '10.0.0',
        },
    } as unknown as Request;
    jest.spyOn(Math, 'random').mockReturnValue(0);
});

describe('dtp cell', () => {
    it('block not exists', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.dtp = undefined;

        const result = render(report, req);
        const dtp = result.find((node) => node.id === 'dtp');

        expect(dtp).toBeUndefined();
    });

    it('block not ready', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.dtp = buildDtpItems(true, true, Status.OK);

        const result = render(report, req);
        const dtp = result.find((node) => node.id === 'dtp');

        expect(get(dtp, 'children[1].children[0].url')).toContain('content-loading-1');
    });

    it('block empty', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.dtp = buildDtpItems(false, true, Status.OK);

        const result = render(report, req);
        const dtp = result.find((node) => node.id === 'dtp');

        expect(get(dtp, 'children[2].children[0].text'))
            .toContain('В ГИБДД нет данных об авариях');
    });

    it('block exist', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.dtp = buildDtpItems(false, false, Status.ERROR);

        const result = render(report, req);
        const dtp = result.find((node) => node.id === 'dtp');
        const xml = dtp ? yogaToXml([ dtp ]) : '';

        expect(dtp).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });
});

function render(report: RawVinReport, req: Request) {
    const appConfig = new AppConfig({}, req);
    const helpers = createHelpers(report, appConfig);
    return makeFullReport(report, helpers);
}

function buildDtpItems(
    isUpdating: boolean,
    isEmpty: boolean,
    status: Status,
): DtpBlock {
    let items: Array<DtpItem> = [];
    if (!isEmpty) {
        items = [
            {
                body_type: 'LIFTBACK',
                title: 'Наезд на стоящее ТС',
                timestamp: '1516705200000',
                place: 'Пермский Край',
                damages: [
                    {
                        damage_type: GibbDamage_DamageType.DAMAGE_RIGHT_REAR_WHEEL,
                        message: 'повреждения правой части заднего бампера',
                        damage_code: '214',
                        damage: {
                            car_part: Damage_CarPart.REAR_BUMPER,
                            type: [ Damage_DamageType.DENT ],
                            description: 'повреждения правой части заднего бампера',
                        },
                    },
                    {
                        damage_type: GibbDamage_DamageType.DAMAGE_LEFT_REAR_WHEEL,
                        message: 'повреждения заднего левого крыла или колеса',
                        damage_code: '216',
                        damage: {
                            car_part: Damage_CarPart.REAR_LEFT_FENDER,
                            type: [ Damage_DamageType.DENT ],
                            description: 'повреждения заднего левого крыла или колеса',
                        },
                    },
                    {
                        damage_type: GibbDamage_DamageType.DAMAGE_ROOF,
                        message: 'повреждения двери багажника',
                        damage_code: '224',
                        damage: {
                            car_part: Damage_CarPart.TRUNK_DOOR,
                            type: [ Damage_DamageType.DENT ],
                            description: 'повреждения двери багажника',
                        },
                    },
                ],
                commentable: { block_id: 'dtp:570001825', add_comment: false },
                comments_info: {
                    commentable: { block_id: 'dtp:570001825', add_comment: false },
                } as CommentableItem,
            } as DtpItem,
        ];
    }
    return {
        header: {
            title: 'Обнаружено 1 ДТП с 2015 года',
            is_updating: isUpdating,
            timestamp_update: '1582053749713',
        },
        items: items,
        status: status,
        record_count: 1,
        available_for_free: true,
    } as DtpBlock;
}

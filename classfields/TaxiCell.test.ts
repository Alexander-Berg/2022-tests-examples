import { createHelpers, makeFullReport } from 'app/server/tmpl/android/v1/Main';
import { AppConfig } from 'app/server/tmpl/android/v1/helpers/AppConfig';
import type { Request } from 'express';
import type { RawVinReport, TaxiBlock, CommentableItem, TaxiItem } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
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

describe('taxi cell', () => {
    it('block not exists', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.taxi = undefined;

        const result = render(report, req);
        const carSharing = result.find((node) => node.id === 'taxi_block');

        expect(carSharing).toBeUndefined();
    });

    it('block not ready', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.taxi = buildTaxiItems(true, true);

        const result = render(report, req);
        const taxi = result.find((node) => node.id === 'taxi_block');

        expect(get(taxi, 'children[1].children[0].url')).toContain('content-loading-1');
    });

    it('block empty', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.taxi = buildTaxiItems(false, true);

        const result = render(report, req);
        const taxi = result.find((node) => node.id === 'taxi_block');

        expect(get(taxi, 'children[1].children[1].children[0].text'))
            .toContain('Автомобиль не\u00A0регистрировался для\u00A0работы в\u00A0такси');
    });

    it('block exist', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.taxi = buildTaxiItems(false, false);

        const result = render(report, req);
        const taxi = result.find((node) => node.id === 'taxi_block');
        const xml = taxi ? yogaToXml([ taxi ]) : '';

        expect(taxi).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });
});

function render(report: RawVinReport, req: Request) {
    const appConfig = new AppConfig({}, req);
    const helpers = createHelpers(report, appConfig);
    return makeFullReport(report, helpers);
}

function buildTaxiItems(isUpdating: boolean, isEmpty: boolean): TaxiBlock {
    let records: Array<TaxiItem> = [];
    if (!isEmpty) {
        records = [
            {
                license_from: '1349726400000',
                license_to: '1507410000000',
                license: '0025065',
                company: 'Бурмистров Николай Вячеславович',
                city: 'Московская область',
                license_status: 'Завершено',
                license_plate_type: 'Обычный',
                is_yellow: false,
                comments_info: {
                    commentable: {
                        block_id: 'taxi:Московская область-1349726400000-1507410000000',
                        add_comment: false,
                    },
                } as CommentableItem,
            },
        ];
    }
    return {
        header: {
            title: 'Работа в такси',
            timestamp_update: '1571856640707',
            is_updating: isUpdating,
        },
        taxi_records: records,
        status: Status.ERROR,
        record_count: 1,
    } as TaxiBlock;
}

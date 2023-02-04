import { createHelpers, makeFullReport } from 'app/server/tmpl/android/v1/Main';
import { AppConfig } from 'app/server/tmpl/android/v1/helpers/AppConfig';
import type { Request } from 'express';
import type { FinesBlock, RawVinReport, Fine } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { FineStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
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

describe('fines cell', () => {
    it('block not exists', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.fines = undefined;

        const result = render(report, req);
        const fines = result.find((node) => node.id === 'fines');

        expect(fines).toBeUndefined();
    });

    it('block not ready', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.fines = buildFinesItems(0, false, true);

        const result = render(report, req);
        const fines = result.find((node) => node.id === 'fines');

        expect(get(fines, 'children[1].children[0].url')).toContain('content-loading-1');
    });

    it('vin not found', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.fines = buildFinesItems(0, true, false);

        const result = render(report, req);
        const fines = result.find((node) => node.id === 'fines');

        expect(get(fines, 'children[2].children[0].children[1].text'))
            .toContain('Невозможно проверить данные о штрафах, для автомобиля не найден номер СТС.');
    });

    it('block exist', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.fines = buildFinesItems(5, false, true);

        const result = render(report, req);
        const fines = result.find((node) => node.id === 'fines');
        const xml = fines ? yogaToXml([ fines ]) : '';

        expect(fines).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });
});

function render(report: RawVinReport, req: Request) {
    const appConfig = new AppConfig({}, req);
    const helpers = createHelpers(report, appConfig);
    return makeFullReport(report, helpers);
}

function buildFinesItems(count: number, isStsKnown: boolean, isUpdating: boolean): FinesBlock {
    const records: Array<Fine> = [];
    if (count > 0) {
        for (let i = 0; i < count; i++) {
            let state: FineStatus = FineStatus.NO_NEED_PAYMENT;
            if (i % 2 === 0) {
                state = FineStatus.PAID;
            } else if (i % 3 === 0) {
                state = FineStatus.NEED_PAYMENT;
            }
            let region = '';
            if (i % 2 === 0) {
                region = 'Москва и Московская область';
            }
            const fine: Fine = {
                date: (1617092490000 + 1000 * i).toString(),
                cost: 123.5 * (i + 1),
                description: i + ' Превышение установленной скорости движения транспортного средства на величину более 20, но не более 40',
                uin: '322470332000' + i + '3834000',
                vendor_name: i + ' УФК по МО (УГИБДД ГУ МВД России по Московской области) (УГИБДД ГУ МВД России по Московской области)',
                status: state,
                region: region,
            };
            records.push(fine);
        }
    }
    return {
        header: {
            title: 'Штрафы',
            timestamp_update: '1617092491774',
            is_updating: isUpdating,
        },
        is_sts_known: isStsKnown,
        records: records,
        status: Status.OK,
        record_count: count,
    };
}

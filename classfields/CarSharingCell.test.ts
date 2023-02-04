import { createHelpers, makeFullReport } from 'app/server/tmpl/android/v1/Main';
import { AppConfig } from 'app/server/tmpl/android/v1/helpers/AppConfig';
import type { Request } from 'express';
import type { RawVinReport, CarSharingBlock, CarSharingItem } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { response200ForVin } from 'app/server/resources/publicApi/methods/getCarfaxOfferReportRaw.fixtures';
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

describe('car sharing cell', () => {
    it('block not exists', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.car_sharing = undefined;

        const result = render(report, req);
        const carSharing = result.find((node) => node.id === 'carsharing');

        expect(carSharing).toBeUndefined();
    });

    it('block not ready', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.car_sharing = buildCarSharingItems(true, true);

        const result = render(report, req);
        const carSharing = result.find((node) => node.id === 'carsharing');

        expect(get(carSharing, 'children[1].children[0].url')).toContain('content-loading-1');
    });

    it('block empty', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.car_sharing = buildCarSharingItems(false, true);

        const result = render(report, req);
        const carSharing = result.find((node) => node.id === 'carsharing');

        expect(get(carSharing, 'children[1].children[1].children[0].children[1].text'))
            .toContain('Автомобиль мог использоваться в каршеринге');
    });

    it('block exist', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.car_sharing = buildCarSharingItems(false, false);

        const result = render(report, req);
        const carSharing = result.find((node) => node.id === 'carsharing');
        const xml = carSharing ? yogaToXml([ carSharing ]) : '';

        expect(carSharing).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });
});

function render(report: RawVinReport, req: Request) {
    const appConfig = new AppConfig({}, req);
    const helpers = createHelpers(report, appConfig);
    return makeFullReport(report, helpers);
}

function buildCarSharingItems(isUpdating: boolean, isEmpty: boolean): CarSharingBlock {
    let records: Array<CarSharingItem> = [];
    if (!isEmpty) {
        records = [
            {
                from: '1531688400000',
                to: '1565384400000',
                company: 'Каршеринг Yandex (ООО МЭЙДЖОР ПРОФИ)',
            },
        ];
    }
    return {
        header: {
            title: isEmpty ? 'Не использовался в каршеринге' : 'Использовался в каршеринге',
            timestamp_update: '1581757890954',
            is_updating: isUpdating,
        },
        could_be_used_in_car_sharing: true,
        car_sharing_records: records,
    } as CarSharingBlock;
}

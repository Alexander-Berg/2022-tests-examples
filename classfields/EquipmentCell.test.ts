import { createHelpers, makeFullReport } from 'app/server/tmpl/android/v1/Main';
import { AppConfig } from 'app/server/tmpl/android/v1/helpers/AppConfig';
import type { Request } from 'express';
import type { RawVinReport } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { response200ForVin } from 'app/server/resources/publicApi/methods/getCarfaxOfferReportRaw.fixtures';
import { yogaToXml } from 'app/server/tmpl/XMLBuilder';
import type { CarInfo } from '@vertis/schema-registry/ts-types-snake/auto/api/cars_model';

let req: Request;
beforeEach(() => {
    req = {
        headers: {
            'x-android-app-version': '10.0.0',
        },
    } as unknown as Request;
    jest.spyOn(Math, 'random').mockReturnValue(0);
});

describe('equipment cell', () => {

    it('block not exists', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.car_info = undefined;

        const result = render(report, req);
        const equipment = result.find((node) => node.id === 'car_info');

        expect(equipment).toBeUndefined();
    });

    it('block exist', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.car_info = buildEquipmentItem(false);

        const result = render(report, req);
        const equipment = result.find((node) => node.id === 'car_info');
        const xml = equipment ? yogaToXml([ equipment ]) : '';

        expect(equipment).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });

});

function render(report: RawVinReport, req: Request) {
    const appConfig = new AppConfig({}, req);
    const helpers = createHelpers(report, appConfig);
    return makeFullReport(report, helpers);
}

// eslint-disable-next-line jest/no-export
export function buildEquipmentItem(isEmpty: boolean): CarInfo {
    let equipment: { [key: string]: boolean } = {};
    if (!isEmpty) {
        equipment = {
            usb: true,
            aux: true,
            'led-lights': true,
            '16-inch-wheels': true,
            '12-inch-wheels': true,
        };
    }
    return {
        body_type: 'SEDAN',
        engine_type: 'GASOLINE',
        transmission: 'MECHANICAL',
        drive: 'REAR_DRIVE',
        mark: 'NISSAN',
        model: 'SKYLINE',
        super_gen_id: '8298646',
        configuration_id: '8298659',
        tech_param_id: '21219762',
        horse_power: 125,
        mark_info: {
            code: 'NISSAN',
            name: 'Nissan',
        },
        model_info: {
            code: 'SKYLINE',
            name: 'Skyline',
        },
        super_gen: {
            id: '8298646',
        },
        configuration: {
            id: '8298659',
            doors_count: 4,
        },
        tech_param: {
            id: '21219762',
            displacement: 1998,
        },
        equipment: equipment,
        steering_wheel: 'RIGHT',
    } as unknown as CarInfo;
}

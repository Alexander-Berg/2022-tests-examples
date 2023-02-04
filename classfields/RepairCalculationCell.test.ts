import { createHelpers, makeFullReport } from 'app/server/tmpl/android/v1/Main';
import { AppConfig } from 'app/server/tmpl/android/v1/helpers/AppConfig';
import type { Request } from 'express';
import type {
    RawVinReport,
    RepairCalculationBlock,
    RepairCalculationItem,
} from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
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

describe('repair calculation cell', () => {
    it('block not exists', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.repair_calculations = undefined;

        const result = render(report, req);
        const repairCalculations = result.find((node) => node.id === 'repair_calculations');

        expect(repairCalculations).toBeUndefined();
    });

    it('block not ready', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.repair_calculations = buildRepairCalculationItems(true, true, Status.OK);

        const result = render(report, req);
        const repairCalculations = result.find((node) => node.id === 'repair_calculations');

        expect(get(repairCalculations, 'children[1].children[0].url')).toContain('content-loading-1');
    });

    it('block empty', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.repair_calculations = buildRepairCalculationItems(false, true, Status.OK);

        const result = render(report, req);
        const repairCalculations = result.find((node) => node.id === 'repair_calculations');

        expect(get(repairCalculations, 'children[1].children[0].children[1].children[1].text'))
            .toContain('Расчёт не проводился');
    });

    it('block locked', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.repair_calculations = buildRepairCalculationItems(false, false, Status.LOCKED);

        const result = render(report, req);
        const repairCalculations = result.find((node) => node.id === 'repair_calculations');
        const xml = repairCalculations ? yogaToXml([ repairCalculations ]) : '';

        expect(repairCalculations).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });

    it('block exist', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.repair_calculations = buildRepairCalculationItems(false, false, Status.ERROR);

        const result = render(report, req);
        const repairCalculations = result.find((node) => node.id === 'repair_calculations');
        const xml = repairCalculations ? yogaToXml([ repairCalculations ]) : '';

        expect(repairCalculations).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });
});

function render(report: RawVinReport, req: Request) {
    const appConfig = new AppConfig({}, req);
    const helpers = createHelpers(report, appConfig);
    return makeFullReport(report, helpers);
}

function buildRepairCalculationItems(
    isUpdating: boolean,
    isEmpty: boolean,
    status: Status,
): RepairCalculationBlock {
    let records: Array<RepairCalculationItem> = [];
    if (!isEmpty) {
        records = [
            {
                data_source: 'Аудатекс',
                assessor: 'Оценщик',
                timestamp: 1311102000000,
                works: [
                    {
                        work_type: 'Замена',
                        parts: [ 'Кузов неукомплектованный замена' ],
                    },
                    {
                        work_type: 'Вспомогательные работы',
                        parts: [ 'Поперечн рычаг пр в сб замен' ],
                    },
                ],
                total_cost: 100500,
            } as unknown as RepairCalculationItem,
        ];
    }
    return {
        header: {
            title: '1 расчёт стоимости ремонта',
            is_updating: isUpdating,
            timestamp_update: '1617092491774',
        },
        calculation_records: records,
        status: status,
    } as RepairCalculationBlock;
}

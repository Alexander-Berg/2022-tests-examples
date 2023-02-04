/* eslint-disable @typescript-eslint/no-use-before-define */
import { toCamel } from 'snake-camel';
import { Status } from '@vertis/schema-registry/ts-types/auto/api/vin/vin_resolution_enums';
import { yogaToXml } from 'app/server/tmpl/XMLBuilder';
import { response200Paid } from 'app/server/resources/publicApi/methods/getCarfaxOfferReportRaw.fixtures';
import type { RawVinReport, RepairCalculationBlock } from '@vertis/schema-registry/ts-types/auto/api/vin/vin_report_model';
import type { RawVinReportResponse } from '@vertis/schema-registry/ts-types/auto/api/response_model';
import { Repair } from './Creator';
import { createHelpers } from '../../Main';

// Step("Ремонт")
it('test_repairs()', () => {
    const report = reportMock();
    if (!report) {
        return;
    }

    const xml = snapshot(report);
    expect(xml).toMatchSnapshot();
});

// Step("Ремонт (блок не готов)")
it('test_notReady()', () => {
    const report = reportMock();
    if (!report) {
        return;
    }

    if (report.repairCalculations?.isReady) {
        report.repairCalculations.isReady = false;
    }

    const xml = snapshot(report);
    expect(xml).toMatchSnapshot();
});

// Step("Ремонт (пустой)")
it('test_repairsEmpty()', () => {
    const report = reportMock();
    if (!report) {
        return;
    }

    if (report.repairCalculations?.isReady) {
        report.repairCalculations.isReady = true;
    }
    if (report.repairCalculations?.calculationRecords) {
        report.repairCalculations.calculationRecords = [];
    }

    const xml = snapshot(report);
    expect(xml).toMatchSnapshot();
});

// Step("Ремонт (пустой, не готов)")
it('test_repairsEmptyNotReady()', () => {
    const report = reportMock();
    if (!report) {
        return;
    }

    if (report.repairCalculations?.isReady) {
        report.repairCalculations.isReady = false;
    }
    if (report.repairCalculations?.calculationRecords) {
        report.repairCalculations.calculationRecords = [];
    }

    const xml = snapshot(report);
    expect(xml).toMatchSnapshot();
});

// Step("Ремонт (пустой, обновляется)")
it('test_repairsEmptyUpdating()', () => {
    const report = reportMock();
    if (!report) {
        return;
    }

    if (report.repairCalculations?.header?.isUpdating) {
        report.repairCalculations.header.isUpdating = true;
    }
    if (report.repairCalculations?.calculationRecords) {
        report.repairCalculations.calculationRecords = [];
    }

    const xml = snapshot(report);
    expect(xml).toMatchSnapshot();
});

// Step("Ремонт (locked)")
it('test_lockedItem()', () => {
    const report = reportMock();
    if (!report) {
        return;
    }

    if (report.repairCalculations?.status) {
        report.repairCalculations.status = Status.LOCKED;
    }

    const xml = snapshot(report);
    expect(xml).toMatchSnapshot();
});

function reportMock(): RawVinReport | undefined {
    const response = response200Paid();
    const camelResponse = toCamel(response) as RawVinReportResponse;
    const report = camelResponse.report;
    if (!report) {
        return;
    }
    report.repairCalculations = calculations;
    // report.repairCalculations?.calculationRecords.push(...records);

    return report;
}

function snapshot(report: RawVinReport): string {
    const helpers = createHelpers(report);
    const nodes = new Repair().cellNodes(report, helpers);
    return yogaToXml(nodes);
}

const calculations: RepairCalculationBlock = {
    header: {
        title: '3 расчёта стоимости ремонта',
        timestampUpdate: '1615793350000',
        isUpdating: false,
    },
    calculationRecords: [
        {
            timestamp: '1519916361051',
            insuranceType: 'ОСАГО',
            dataSource: 'Audatex',
            totalCost: 10000,
            coloringCost: 10000,
            partsCost: 10000,
            works: [
                {
                    workType: 'Окраска ремонтная (площадь повреждения детали < 20 - 50 %)',
                    parts: [ 'ОБЛИЦОВКА БАМПЕР ПЕРРЕМОНТНАЯ ОКРАСКА K3' ],
                },
                {
                    workType: 'Окраска новой детали',
                    parts: [ 'МОЛД ВОЗД РЕШЕТ П ЛОКРАСКА НОВ.ДЕТ. K1R' ],
                },
                {
                    workType: 'Замена',
                    parts: [
                        'ВОЗД РЕШЕТКА Л БАМП51 11 8 064 963',
                        'МОЛД ВОЗД РЕШЕТ П Л51 11 8 064 979',
                        'НАДБАВКА ВРЕМЕНИ К ОСНОВНОЙ РАБОТЕ',
                        'ОБЛИЦОВКА БАМПЕРА П - С/У',
                        'МОЛДИНГ Л БАМПЕР П - С/У',
                    ],
                },
                {
                    workType: 'Вспомогательные работы',
                    parts: [
                        'ДОП РАБОТЫ ДЛЯ СИСТЕМ ПАРКОВКИ',
                        'РЕШЕТКА ВОЗДУХОВОДА Н С/У',
                        'МОЛДИНГ ПР БАМПЕР П - С/У',
                        'НОМЕРН ЗНАК П - С/У',
                        'ФАРА ПРОТИВОТУМАННАЯ Л - С/У',
                        'ФАРА ПРОТИВОТУМАННАЯ ПР - С/У',
                    ],
                },
            ],
            mileage: 31325,
            totalLoss: false,
            mileageStatus: Status.OK,
            totalCostRange: {
                from: 10000,
                to: 25000,
            },
            coloringCostRange: {
                from: 0,
                to: 10000,
            },
            worksCostRange: {
                from: 0,
                to: 10000,
            },
            partsCostRange: {
                from: 0,
                to: 10000,
            },
            worksCost: 1000,
            assessor: 'Оценщик',
            status: Status.OK,
            recordCount: 0,
        },
        {
            timestamp: '1524486865035',
            insuranceType: 'КАСКО',
            dataSource: 'Audatex',
            totalCost: 100000,
            coloringCost: 10000,
            partsCost: 50000,
            works: [
                {
                    workType: 'Окраска ремонтная (площадь повреждения детали < 20 - 50 %)',
                    parts: [ 'ОБЛИЦОВКА БАМПЕР ПЕРОКРАСИТЬ ЭТАП 3' ],
                },
                {
                    workType: 'Замена',
                    parts: [
                        'ОБЛИЦОВКА БАМПЕР ПЕР51 11 8 069 072',
                        'НАДБАВКА ВРЕМЕНИ К ОСНОВНОЙ РАБОТЕ',
                        'ОБЛИЦОВКА БАМПЕРА П - С/У',
                        'ОБЛИЦОВКА БАМПЕРА П - ЗАМЕНИТЬ',
                        'ДОП РАБОТЫ ДЛЯ СИСТЕМ ПАРКОВКИ',
                    ],
                },
                {
                    workType: 'Замена',
                    parts: [ 'НАДБАВКА ВРЕМЕНИ К ОСНОВНОЙ РАБОТЕ' ],
                },
            ],
            totalLoss: false,
            mileageStatus: Status.OK,
            totalCostRange: {
                from: 50000,
                to: 100000,
            },
            coloringCostRange: {
                from: 0,
                to: 10000,
            },
            worksCostRange: {
                from: 0,
                to: 10000,
            },
            partsCostRange: {
                from: 50000,
                to: 100000,
            },
            mileage: 1000,
            worksCost: 1000,
            assessor: 'Оценщик',
            status: Status.OK,
            recordCount: 0,
        },
        {
            timestamp: '1531482256048',
            insuranceType: 'ОСАГО',
            dataSource: 'Audatex',
            totalCost: 100000,
            coloringCost: 10000,
            partsCost: 50000,
            works: [
                {
                    workType: 'Замена',
                    parts: [
                        'ОБЛИЦОВКА БАМПЕРА З51 12 8 073 725',
                        'ПЛЕНКА ЗД БАМП (ДОП)ПБДОП25575',
                        'ПЛЕНКА СПОЙЛЕРА (ДОППБДОП11550',
                        'ОБЛИЦОВКА БАМПЕРА З - С/У',
                        'ОБЛИЦОВКА БАМПЕРА З - ЗАМЕНИТЬ',
                        'ДОП РАБОТЫ ДЛЯ СИСТЕМ ПАРКОВКИ',
                    ],
                },
                {
                    workType: 'Окраска ремонтная (площадь повреждения детали < 20 - 50 %)',
                    parts: [ 'НАКЛАДКА БАМПЕРА ЗРЕМОНТНАЯ ОКРАСКА K3' ],
                },
                {
                    workType: 'Вспомогательные работы',
                    parts: [ 'НАДБАВКА ВРЕМЕНИ К ОСНОВНОЙ РАБОТЕ', 'ПЛЕНКУ СПОЙЛЕРАРАЗОБРАТЬ/СОБРАТЬ' ],
                },
                {
                    workType: 'Ремонт',
                    parts: [ 'КРЫШКА БАГАЖНИКАРЕМОНТИРОВАТЬ' ],
                },
                {
                    workType: 'Окраска новой детали',
                    parts: [ 'ОБЛИЦОВКА БАМПЕРА ЗОКРАСКА НОВ.ДЕТ. K1R' ],
                },
            ],
            mileage: 40616,
            totalLoss: false,
            mileageStatus: Status.OK,
            totalCostRange: {
                from: 50000,
                to: 100000,
            },
            coloringCostRange: {
                from: 0,
                to: 10000,
            },
            worksCostRange: {
                from: 0,
                to: 10000,
            },
            partsCostRange: {
                from: 50000,
                to: 100000,
            },
            worksCost: 1000,
            assessor: 'Оценщик',
            status: Status.OK,
            recordCount: 0,
        },
    ],
    status: Status.OK,
    recordCount: 0,
    isReady: true,
} as unknown as RepairCalculationBlock;

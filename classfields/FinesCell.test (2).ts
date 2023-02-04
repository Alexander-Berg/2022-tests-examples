/* eslint-disable @typescript-eslint/no-use-before-define */
/* eslint-disable max-len */
import type {
    RawVinReport,
    FinesBlock,
    Fine,
} from '@vertis/schema-registry/ts-types/auto/api/vin/vin_report_model';
import {
    FineStatus,
} from '@vertis/schema-registry/ts-types/auto/api/vin/vin_report_model';

import { toCamel } from 'snake-camel';

import { yogaToXml } from 'app/server/tmpl/XMLBuilder';
import { response200Paid } from 'app/server/resources/publicApi/methods/getCarfaxOfferReportRaw.fixtures';
import type { RawVinReportResponse } from '@vertis/schema-registry/ts-types/auto/api/response_model';
import { Status } from '@vertis/schema-registry/ts-types/auto/api/vin/vin_resolution_enums';
import { Fines } from './Creator';
import { createHelpers } from '../../Main';

// Step("Штрафы")
it('FinesCellTests_test_hasRecords()', () => {
    const report = reportMock();
    if (!report) {
        return;
    }

    const xml = snapshot(report);
    expect(xml).toMatchSnapshot();
});

// Step("Штрафы")
it('FinesCellTests_test_hasThree()', () => {
    const report = reportMock();
    if (!report) {
        return;
    }

    if (report.fines) {
        report.fines.records = report.fines.records?.slice(0, 3);
    }

    const xml = snapshot(report);
    expect(xml).toMatchSnapshot();
});

// Step("Штрафы (нет)")
it('FinesCellTests_test_noRecords()', () => {
    const report = reportMock();
    if (!report) {
        return;
    }

    if (report.fines) {
        report.fines.records = [];
    }

    const xml = snapshot(report);
    expect(xml).toMatchSnapshot();
});

// Step("Штрафы (не знаем СТС)")
it('FinesCellTests_test_noSTSKnown', () => {
    const report = reportMock();
    if (!report) {
        return;
    }

    if (report.fines) {
        report.fines.records = [];
        report.fines.isStsKnown = false;
    }

    const xml = snapshot(report);
    expect(xml).toMatchSnapshot();
});

// Step("Штрафы (обновляются)")
it('FinesCellTests_test_isUpdating()', () => {
    const report = reportMock();
    if (!report) {
        return;
    }

    if (report.fines?.header) {
        report.fines.header.isUpdating = true;
    }

    const xml = snapshot(report);
    expect(xml).toMatchSnapshot();
});

// Step("Штрафы (есть неоплаченные)")
it('FinesCellTests_test_notPaid()', () => {
    const report = reportMock();
    if (!report) {
        return;
    }

    if (report.fines?.records) {
        report.fines.records[0].status = FineStatus.NEED_PAYMENT;
    }

    const xml = snapshot(report);
    expect(xml).toMatchSnapshot();
});

// utils
function reportMock(): RawVinReport | undefined {
    const response = response200Paid();
    const camelResponse = toCamel(response) as RawVinReportResponse;
    const report = camelResponse.report;
    if (!report) {
        return;
    }
    report.fines = finesMock;
    report.fines?.records?.push(...fines);

    return report;
}

function snapshot(report: RawVinReport): string {
    const helpers = createHelpers(report);
    const nodes = new Fines().cellNodes(report, helpers);
    return yogaToXml(nodes);
}

const finesMock: FinesBlock = {
    header: {
        title: 'Штрафы',
        isUpdating: false,
        timestampUpdate: '1619191628767',
    },
    isStsKnown: true,
    records: [],
    status: Status.OK,
    recordCount: 0,
};

const fines: Array<Fine> = [
    {
        date: '1523300400000',
        cost: 500,
        uin: '18810136180410018821',
        vendorName: 'Центр видеофиксации ГИБДД ГУ МВД России по Воронежской области',
        status: FineStatus.PAID,
        description: 'Превышение установленной скорости движения транспортного средства на величину более 20, но не более 40 километров в час',
        region: '',
    },
    {
        date: '1523386800000',
        cost: 500,
        uin: '18810136180411043595',
        vendorName: 'Центр видеофиксации ГИБДД ГУ МВД России по Воронежской области',
        status: FineStatus.PAID,
        description: 'Превышение установленной скорости движения транспортного средства на величину более 20, но не более 40 километров в час',
        region: '',
    },
    {
        date: '1523818800000',
        cost: 500,
        uin: '18810161180416035712',
        vendorName: 'ЦАФАП ГИБДД ГУ МВД России по Ростовской области',
        status: FineStatus.PAID,
        description: 'Превышение установленной скорости движения транспортного средства на величину более 20, но не более 40 километров в час',
        region: '',
    },
    {
        date: '1529002800000',
        cost: 500,
        uin: '',
        vendorName: 'ЦАФАП в ОДД ГИБДД ГУ МВД России по г.СПб и ЛО',
        status: FineStatus.PAID,
        description: 'Превышение установленной скорости движения транспортного средства на величину более 20, но не более 40 километров в час',
        region: '',
    },
    {
        date: '1531249200000',
        cost: 500,
        description: 'Превышение установленной скорости движения транспортного средства на величину более 20, но не более 40 километров в час',
        uin: '18810150180711145333',
        vendorName: 'Центр видеофиксации ГИБДД ГУ МВД России по Московской области',
        status: FineStatus.PAID,
        region: '',
    },
    {
        date: '1532199600000',
        cost: 500,
        description: 'Превышение установленной скорости движения транспортного средства на величину более 20, но не более 40 километров в час',
        uin: '18810136180722051492',
        vendorName: 'Центр видеофиксации ГИБДД ГУ МВД России по Воронежской области',
        status: FineStatus.PAID,
        region: '',
    },
    {
        date: '1533841200000',
        cost: 500,
        description: 'Превышение установленной скорости движения транспортного средства на величину более 20, но не более 40 километров в час',
        uin: '18810147180810007146',
        vendorName: 'ЦАФАП в ОДД ГИБДД ГУ МВД России по Ленинградской области',
        status: FineStatus.PAID,
        region: '',
    },
    {
        date: '1534964400000',
        cost: 500,
        uin: '18810178180823078712',
        vendorName: 'УФК по г.Санкт-Петербургу (УГИБДД ГУ МВД России по г. Санкт-Петербургу и Ленинградской области)(ЦАФАП в ОДД ГИБДД ГУ МВД России по г.СПб и ЛО)',
        description: 'Превышение установленной скорости движения транспортного средства на величину более 20, но не более 40 километров в час',
        status: FineStatus.PAID,
        region: '',
    },
    {
        date: '1539543600000',
        cost: 500,
        uin: '18810178181015076199',
        vendorName: 'УФК по г.Санкт-Петербургу (УГИБДД ГУ МВД России по г. Санкт-Петербургу и Ленинградской области)(ЦАФАП в ОДД ГИБДД ГУ МВД России по г.СПб и ЛО)',
        description: 'Превышение установленной скорости движения транспортного средства на величину более 20, но не более 40 километров в час',
        status: FineStatus.PAID,
        region: '',
    },
    {
        date: '1542826800000',
        cost: 500,
        description: 'Превышение установленной скорости движения транспортного средства на величину более 20, но не более 40 километров в час',
        uin: '18810178181122004796',
        vendorName: 'ЦАФАП в ОДД ГИБДД ГУ МВД России по г.СПб и ЛО',
        status: FineStatus.PAID,
        region: '',
    },
    {
        date: '1543863600000',
        cost: 500,
        description: 'Превышение установленной скорости движения транспортного средства на величину более 20, но не более 40 километров в час',
        uin: '18810169181204001376',
        vendorName: 'ЦАФАП ГИБДД УМВД России по Тверской области',
        status: FineStatus.PAID,
        region: '',
    },
    {
        date: '1545332400000',
        cost: 500,
        description: 'Превышение установленной скорости движения транспортного средства на величину более 20, но не более 40 километров в час',
        uin: '18810169181221004728',
        vendorName: 'ЦАФАП ГИБДД УМВД России по Тверской области',
        status: FineStatus.PAID,
        region: '',
    },
    {
        date: '1545505200000',
        cost: 500,
        description: 'Превышение установленной скорости движения транспортного средства на величину более 20, но не более 40 километров в час',
        uin: '18810150181223025798',
        vendorName: 'Центр видеофиксации ГИБДД ГУ МВД России по Московской области',
        status: FineStatus.PAID,
        region: '',
    },
    {
        date: '1550430000000',
        cost: 500,
        uin: '',
        description: 'Превышение установленной скорости движения транспортного средства на величину более 20, но не более 40 километров в час',
        vendorName: 'Центр видеофиксации ГИБДД ГУ МВД России по Московской области',
        status: FineStatus.PAID,
        region: '',
    },
    {
        date: '1557946800000',
        cost: 500,
        uin: '',
        description: 'Превышение установленной скорости движения транспортного средства на величину более 20, но не более 40 километров в час',
        vendorName: 'Центр видеофиксации ГИБДД ГУ МВД России по Московской области',
        status: FineStatus.PAID,
        region: '',
    },
    {
        date: '1558119600000',
        cost: 500,
        uin: '',
        description: 'Превышение установленной скорости движения транспортного средства на величину более 20, но не более 40 километров в час',
        vendorName: 'УГИБДД ГУ МВД России по г. Москве',
        status: FineStatus.PAID,
        region: '',
    },
    {
        date: '1558983600000',
        cost: 1000,
        description: 'Превышение установленной скорости движения транспортного средства на величину более 40, но не более 60 километров в час',
        uin: '18810178190528024778',
        vendorName: 'ЦАФАП в ОДД ГИБДД ГУ МВД России по г.СПб и ЛО',
        status: FineStatus.PAID,
        region: '',
    },
    {
        date: '1559070000000',
        cost: 500,
        description: 'Превышение установленной скорости движения транспортного средства на величину более 20, но не более 40 километров в час',
        uin: '18810178190529005661',
        vendorName: 'ЦАФАП в ОДД ГИБДД ГУ МВД России по г.СПб и ЛО',
        status: FineStatus.PAID,
        region: '',
    },
    {
        date: '1585076400000',
        cost: 500,
        uin: '32247033200008342003',
        vendorName: 'УФК ПО ЛЕНИНГРАДСКОЙ ОБЛАСТИ (СОСНОВОБОРСКИЙ РОСП УФССП РОССИИ ПО ЛЕНИНГРАДСКОЙ ОБЛАСТИ Л/С 05451844500)',
        description: 'Превышение установленной скорости движения транспортного средства на величину более 20, но не более 40 километров в час',
        status: FineStatus.PAID,
        region: '',
    },
    {
        date: '1585249200000',
        cost: 500,
        description: 'Превышение установленной скорости движения транспортного средства на величину более 20, но не более 40 километров в час',
        uin: '18810148200327000976',
        vendorName: 'УФК по Липецкой области (УМВД России по Липецкой области л/с 04461060350)(ЦАФАП ОДД ГИБДД УМВД России по Липецкой области)',
        status: FineStatus.PAID,
        region: '',
    },
    {
        date: '1586113200000',
        cost: 500,
        description: 'Превышение установленной скорости движения транспортного средства на величину более 20, но не более 40 километров в час',
        uin: '18810169200406032412',
        vendorName: 'УФК по Тверской области (Управление МВД Российской Федерации по Тверской области)(ЦАФАП ГИБДД УМВД России по Тверской области)',
        status: FineStatus.PAID,
        region: '',
    },
    {
        date: '1586286000000',
        cost: 800,
        description: 'Невыполнение требования Правил дорожного движения об остановке перед стоп-линией, обозначенной дорожными знаками или разметкой проезжей части дороги, при запрещающем сигнале светофора или запрещающем жесте регулировщика',
        uin: '18810150200408017942',
        vendorName: 'УФК по МО (УГИБДД ГУ МВД России по Московской области)(Центр видеофиксации ГИБДД ГУ МВД России по Московской области)',
        status: FineStatus.PAID,
        region: '',
    },
    {
        date: '1586977200000',
        cost: 500,
        description: 'Превышение установленной скорости движения транспортного средства на величину более 20, но не более 40 километров в час',
        uin: '18810150200416709235',
        vendorName: 'УФК по МО (УГИБДД ГУ МВД России по Московской области)(Центр видеофиксации ГИБДД ГУ МВД России по Московской области)',
        status: FineStatus.PAID,
        region: '',
    },
    {
        date: '1586977200000',
        cost: 500,
        description: 'Превышение установленной скорости движения транспортного средства на величину более 20, но не более 40 километров в час',
        uin: '18810153200416003687',
        vendorName: 'УФК по Новгородской области (УМВД России по Новгородской области)(ЦАФАП ОБДД ГИБДД УМВД РФ по Новгородской области)',
        status: FineStatus.PAID,
        region: '',
    },
];

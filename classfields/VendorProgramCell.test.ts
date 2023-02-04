import { createHelpers, makeFullReport } from 'app/server/tmpl/android/v1/Main';
import { AppConfig } from 'app/server/tmpl/android/v1/helpers/AppConfig';
import type { Request } from 'express';
import type { RawVinReport, ProgramItem, ProgramsBlock } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
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

describe('vendor program cell', () => {
    it('block not exists', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.programs = undefined;

        const result = render(report, req);
        const programs = result.find((node) => node.id === 'programs');

        expect(programs).toBeUndefined();
    });

    it('block not ready', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.programs = buildVendorProgramItems(true, true);

        const result = render(report, req);
        const programs = result.find((node) => node.id === 'programs');

        expect(get(programs, 'children[1].children[0].url')).toContain('content-loading-1');
    });

    it('block empty', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.programs = buildVendorProgramItems(false, true);

        const result = render(report, req);
        const programs = result.find((node) => node.id === 'programs');

        expect(programs).toBeUndefined();
    });

    it('block exist', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.programs = buildVendorProgramItems(false, false);

        const result = render(report, req);
        const programs = result.find((node) => node.id === 'programs');
        const xml = programs ? yogaToXml([ programs ]) : '';

        expect(programs).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });
});

function render(report: RawVinReport, req: Request) {
    const appConfig = new AppConfig({}, req);
    const helpers = createHelpers(report, appConfig);
    return makeFullReport(report, helpers);
}

function buildVendorProgramItems(isUpdating: boolean, isEmpty: boolean): ProgramsBlock {
    let records: Array<ProgramItem> = [];
    if (!isEmpty) {
        records = [
            {
                program_name: 'Контракт NS3+ 12 месяцев или 125000 км.',
                start_date: '1645650000000',
                finish_date: '1677099600000',
                uri: 'https://www.nissan.ru/ownership/nissan-service3plus.html',
                description: 'Продленная гарантия от Nissan',
            } as ProgramItem,
            {
                program_name: 'Контракт NS3+ 12 месяцев или 250000 км.',
                start_date: '1677186000000',
                finish_date: '1708635600000',
                uri: 'https://www.nissan.ru/ownership/nissan-service3plus.html',
                description: 'Продленная гарантия от Nissan',
            } as ProgramItem,
        ];
    }
    return {
        header: {
            title: 'Программы производителей',
            timestamp_update: '1574264050000',
            is_updating: isUpdating,
        },
        program_records: records,
        status: Status.OK,
        record_count: 2,
        description: 'Программы дают преимущества в обслуживании',
    } as unknown as ProgramsBlock;
}

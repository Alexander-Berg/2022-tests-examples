import { createHelpers, makeFullReport } from 'app/server/tmpl/android/v1/Main';
import { AppConfig } from 'app/server/tmpl/android/v1/helpers/AppConfig';
import type { Request } from 'express';
import type { RawVinReport, CustomsBlock } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
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

describe('customs cell', () => {
    it('block not exists', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.customs = undefined;

        const result = render(report, req);
        const customs = result.find((node) => node.id === 'customs');

        expect(customs).toBeUndefined();
    });

    it('block not ready', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.customs = buildCustomsItems(true, true);

        const result = render(report, req);
        const customs = result.find((node) => node.id === 'customs');

        expect(get(customs, 'children[1].children[0].url')).toContain('content-loading-1');
    });

    it('block exist', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.customs = buildCustomsItems(false, false);

        const result = render(report, req);
        const customs = result.find((node) => node.id === 'customs');
        const xml = customs ? yogaToXml([ customs ]) : '';

        expect(customs).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });

    it('block empty', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.customs = buildCustomsItems(false, true);

        const result = render(report, req);
        const customs = result.find((node) => node.id === 'customs');

        expect(get(customs, 'children[1].children[0].children[0].text'))
            .toContain('Автомобиль не ввозили в РФ');
    });
});

function render(report: RawVinReport, req: Request) {
    const appConfig = new AppConfig({}, req);
    const helpers = createHelpers(report, appConfig);
    return makeFullReport(report, helpers);
}

function buildCustomsItems(isUpdating: boolean, isEmpty: boolean): CustomsBlock {
    const customs = [];
    if (!isEmpty) {
        customs.push(
            {
                date: '1540155600000',
                country_from_name: 'Занзибар',
                country_to_name: 'Эстония',
            },
        );
    }
    return {
        header: {
            title: 'Таможня',
            timestamp_update: '1617092491774',
            is_updating: isUpdating,
        },
        customs_records: customs,
        status: Status.OK,
        record_count: 2,
    };
}

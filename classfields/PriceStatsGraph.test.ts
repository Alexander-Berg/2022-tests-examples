import { createHelpers, makeFullReport } from 'app/server/tmpl/android/v1/Main';
import { AppConfig } from 'app/server/tmpl/android/v1/helpers/AppConfig';
import type { Request } from 'express';
import * as fixtures from 'app/server/resources/publicApi/methods/getCarfaxOfferReportRaw.fixtures';
import type { RawVinReport } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { get } from 'lodash';
import { yogaToXml } from 'app/server/tmpl/XMLBuilder';


let req: Request;
beforeEach(() => {
    req = {
        headers: {
            'x-android-app-version': '9.8.0',
        },
    } as unknown as Request;
});

describe('price_stats', () => {
    it('should not render price_stats_graph if it not exists', () => {
        const report = fixtures.response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;

        const result = render(report, req);
        const priceStates = result.find((node) => node.id === 'price_stats');

        expect(priceStates).toBeUndefined();
    });

    describe('block exists', () => {
        it('common snapshot test', () => {
            const report = fixtures.response200ForVin('Z94K241BAKR092916').report as RawVinReport;
            const priceStates = report.price_stats_graph;
            if (priceStates) {
                priceStates.mark = 'Hyundai';
                priceStates.model = 'Solaris';
                priceStates.year = 2018;
            }
            report.price_stats_graph = priceStates;

            const result = render(report, req);
            const priceStateBlock = result.find((node) => node.id === 'price_stats');
            const xml = priceStateBlock ? yogaToXml([ priceStateBlock ]) : '';

            expect(priceStateBlock).toMatchSnapshot();
            expect(xml).toMatchSnapshot();
        });

        it('should render header with average price', () => {
            const report = fixtures.response200ForVin('Z94K241BAKR092916').report as RawVinReport;

            const result = render(report, req);
            const priceStates = result.find((node) => node.id === 'price_stats');

            expect(get(priceStates, 'children[0].children[0].text')).toEqual('~765 500 ₽');
        });
    });
});

function render(report: RawVinReport, req: Request) {
    const appConfig = new AppConfig({}, req);
    const helpers = createHelpers(report, appConfig);
    return makeFullReport(report, helpers);
}

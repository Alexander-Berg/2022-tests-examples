import { createHelpers, makeFullReport } from 'app/server/tmpl/android/v1/Main';
import { AppConfig } from 'app/server/tmpl/android/v1/helpers/AppConfig';
import type { Request } from 'express';
import type { RawVinReport, SourcesBlock } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { response200ForVin } from 'app/server/resources/publicApi/methods/getCarfaxOfferReportRaw.fixtures';
import { yogaToXml } from 'app/server/tmpl/XMLBuilder';
import { SourcesModel } from 'app/server/tmpl/android/v1/cells/sources/Model';

let req: Request;
beforeEach(() => {
    req = {
        headers: {
            'x-android-app-version': '10.0.0',
        },
    } as unknown as Request;
    jest.spyOn(Math, 'random').mockReturnValue(0);
});

describe('sources cell', () => {
    const id = new SourcesModel().identifier;

    it('block not exists', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.sources = undefined;

        const result = render(report, req);
        const sources = result.find((node) => node.id === id);

        expect(sources).toBeUndefined();
    });

    it('hide block if source == reade', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.sources = {
            sources_count: 10,
            ready_count: 10,
        } as SourcesBlock;

        const result = render(report, req);
        const sources = result.find((node) => node.id === id);

        expect(sources).toBeUndefined();
    });

    it('block exists', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.sources = {
            sources_count: 10,
            ready_count: 5,
        } as SourcesBlock;

        const result = render(report, req);
        const sources = result.find((node) => node.id === id);
        const xml = sources ? yogaToXml([ sources ]) : '';

        expect(sources).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });

});

function render(report: RawVinReport, req: Request) {
    const appConfig = new AppConfig({}, req);
    const helpers = createHelpers(report, appConfig);
    return makeFullReport(report, helpers);
}

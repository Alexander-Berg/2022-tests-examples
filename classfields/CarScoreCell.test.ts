import { createHelpers, makeFullReport } from 'app/server/tmpl/android/v1/Main';
import { AppConfig } from 'app/server/tmpl/android/v1/helpers/AppConfig';
import type { Request } from 'express';
import type { HealthScore, RawVinReport } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { response200ForVin } from 'app/server/resources/publicApi/methods/getCarfaxOfferReportRaw.fixtures';
import { yogaToXml } from 'app/server/tmpl/XMLBuilder';
import { ReportType } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_resolution_enums';
import { searchTree } from 'app/server/tmpl/android/v1/helpers/SharedCode';

let req: Request;
beforeEach(() => {
    req = {
        headers: {
            'x-android-app-version': '10.0.0',
        },
    } as unknown as Request;
    jest.spyOn(Math, 'random').mockReturnValue(0);
});

describe('car score cell', () => {
    it('block not exists', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.health_score = undefined;

        const result = render(report, req);
        const carScore = result.find((node) => node.id === 'car_score');

        expect(carScore).toBeUndefined();
    });

    it('score for not bought report', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.health_score = buildScoreItem();
        report.report_type = ReportType.FREE_REPORT;

        const result = render(report, req);
        const carScore = result.find((node) => node.id === 'car_score');
        const xml = carScore ? yogaToXml([ carScore ]) : '';

        expect(carScore).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });

    it('loading state', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.health_score = buildScoreItem(true);

        const result = render(report, req);
        const carScore = result.find((node) => node.id === 'car_score');
        const xml = carScore ? yogaToXml([ carScore ]) : '';

        expect(carScore).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });

    it('ready state', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.health_score = buildScoreItem(false);

        const result = render(report, req);
        const carScore = result.find((node) => node.id === 'car_score');
        const xml = carScore ? yogaToXml([ carScore ]) : '';

        expect(carScore).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });

    it('score in content block', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.health_score = buildScoreItem(false);

        const result = render(report, req);
        const carScoreInContentBlock = searchTree(result[0], 'car_score_item_id');

        expect(carScoreInContentBlock).toMatchSnapshot();
    });

    it('loading score in content block', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.health_score = buildScoreItem(true);

        const result = render(report, req);
        const carScoreInContentBlock = searchTree(result[1], 'car_score_item_id');

        expect(carScoreInContentBlock).toMatchSnapshot();
    });

});

function render(report: RawVinReport, req: Request) {
    const appConfig = new AppConfig({}, req);
    const helpers = createHelpers(report, appConfig);
    return makeFullReport(report, helpers);
}

function buildScoreItem(isUpdating?: boolean): HealthScore {
    return {
        score: 43.8,
        header: {
            title: 'Оценка автомобиля',
            is_updating: isUpdating,
            timestamp_update: '1613458320319',
        },
        score_distribution: {
            offer_count: 8,
            offers_min_score: 43,
            offers_max_score: 66,
        },
    } as HealthScore;
}

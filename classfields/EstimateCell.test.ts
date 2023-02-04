import { createHelpers, makeFullReport } from 'app/server/tmpl/android/v1/Main';
import { AppConfig } from 'app/server/tmpl/android/v1/helpers/AppConfig';
import type { Request } from 'express';
import type { EstimateBlock, RawVinReport } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
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

describe('estimate_cell', () => {
    it('block not exists', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.estimates = undefined;

        const result = render(report, req);
        const estimateCell = result.find((node) => node.id === 'estimates');

        expect(estimateCell).toBeUndefined();
    });

    it('not show block if empty', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        // eslint-disable-next-line @typescript-eslint/no-use-before-define
        report.estimates = estimateEmpty;

        const result = render(report, req);
        const estimateCell = result.find((node) => node.id === 'estimates');

        expect(estimateCell).toBeUndefined();
    });

    it('block not ready', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        // eslint-disable-next-line @typescript-eslint/no-use-before-define
        report.estimates = estimateNotReady;

        const result = render(report, req);
        const estimateCell = result.find((node) => node.id === 'estimates');

        expect(get(estimateCell, 'children[1].children[0].url')).toContain('content-loading-1');
    });

    it('block exist', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        // eslint-disable-next-line @typescript-eslint/no-use-before-define
        report.estimates = estimate as unknown as EstimateBlock;

        const result = render(report, req);
        const estimateCell = result.find((node) => node.id === 'estimates');
        const xml = estimateCell ? yogaToXml([ estimateCell ]) : '';

        expect(estimateCell).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });
});

function render(report: RawVinReport, req: Request) {
    const appConfig = new AppConfig({}, req);
    const helpers = createHelpers(report, appConfig);
    return makeFullReport(report, helpers);
}

const estimateEmpty = {
    header: {
        title: 'Нет оценки от партнеров',
        timestamp_update: '1584444558871',
        is_updating: false,
    },
    estimate_records: [],
    status: Status.OK,
    record_count: 0,
    comments_count: 0,
};

const estimateNotReady = {
    header: {
        title: 'Нет объявлений на Авто.ру',
        is_updating: true,
        timestamp_update: '1584390758494',
    },
    estimate_records: [],
    record_count: 0,
    comments_count: 0,
    status: Status.IN_PROGRESS,
};

const estimate = {
    header: {
        title: 'Оценка от партнёров',
        timestamp_update: '1617092491774',
        is_updating: false,
    },
    estimate_records: [
        {
            date: '1617092491000',
            region_name: 'Махачкала',
            mileage: 123456,
            partner_name: 'Торги России',
            partner_url: 'https://торги-россии.рф/',
            mileage_status: Status.OK,
            results: {
                report_url: 'https://торги-россии.рф/',
                summary: 'Автомобиль реализовывался на торгах по банкротству юридических или физических лиц',
                price_from: 9999,
                price_to: 99999,
            },
            images: [
                {
                    name: 'autoru-carfax:1397989-Ogz6uRIR6NfSfZILolqihdnN3wSbSxV4r',
                    sizes: {
                        thumb_m: '//images.mds-proxy.test.avto.ru/get-autoru-carfax/1397989/Ogz6uRIR6NfSfZILolqihdnN3wSbSxV4r/thumb_m',
                    },
                },
            ],
        },
    ],
    record_count: 1,
    status: Status.OK,
};

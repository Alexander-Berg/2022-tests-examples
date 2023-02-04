import { createHelpers, makeFullReport } from 'app/server/tmpl/android/v1/Main';
import { AppConfig } from 'app/server/tmpl/android/v1/helpers/AppConfig';
import type { Request } from 'express';
import type { RawVinReport, BrandCertificationBlock, BrandCertificationItem } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { response200ForVin } from 'app/server/resources/publicApi/methods/getCarfaxOfferReportRaw.fixtures';
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

describe('vendor certification cell', () => {
    it('block not exists', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.brand_certification = undefined;

        const result = render(report, req);
        const certification = result.find((node) => node.id === 'certifications');

        expect(certification).toBeUndefined();
    });

    it('block not ready', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.brand_certification = buildVendorCertificationItems(true, true);

        const result = render(report, req);
        const certification = result.find((node) => node.id === 'certifications');

        expect(get(certification, 'children[1].children[0].url')).toContain('content-loading-1');
    });

    it('block empty', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.brand_certification = buildVendorCertificationItems(false, true);

        const result = render(report, req);
        const certification = result.find((node) => node.id === 'certifications');

        expect(certification).toBeUndefined();
    });

    it('block exist', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.brand_certification = buildVendorCertificationItems(false, false);

        const result = render(report, req);
        const certification = result.find((node) => node.id === 'certifications');
        const xml = certification ? yogaToXml([ certification ]) : '';

        expect(certification).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });
});

function render(report: RawVinReport, req: Request) {
    const appConfig = new AppConfig({}, req);
    const helpers = createHelpers(report, appConfig);
    return makeFullReport(report, helpers);
}

function buildVendorCertificationItems(isUpdating: boolean, isEmpty: boolean): BrandCertificationBlock {
    let records: Array<BrandCertificationItem> = [];
    if (!isEmpty) {
        records = [
            {
                program_name: 'MercedesBenz',
                create_timestamp: '1589541522854',
                update_timestamp: '1590420003476',
                view: {
                    name: 'Mercedes-Benz Certified',
                    advantages_html: [
                        'Постгарантийная поддержка Mercedes-Benz Certified&nbsp;&mdash; 12&nbsp;месяцев без ограничения пробега',
                        '24-часовая помощь на&nbsp;дорогах',
                        '75&nbsp;критериев тщательной технической проверки',
                    ],
                    description_html: 'В&nbsp;рамках специальной программы Mercedes-Benz Certified официальные дилеры &laquo;Мерседес-Бенц&raquo;' +
                        ' предлагают вам приобрести сертифицированные автомобили с&nbsp;пробегом. Каждый сертифицированный автомобиль включает' +
                        ' в&nbsp;себя: <ul><li>постгарантийную поддержку&nbsp;&mdash; 12&nbsp;месяцев без ограничения пробега;</li><li>поддержку' +
                        ' на&nbsp;дорогах 24/7;</li><li>тщательную юридическую и&nbsp;техническую проверку.</li></ul>',
                    logo: [ {
                        sizes: {
                            full: '//yastatic.net/s3/vertis-frontend/autoru-frontend/manufacturer-cert-logo/MercedesBenz/logo.png',
                        },
                    } ],
                    description_url: 'https://used.mercedes-benz.ru/Certified/',
                },
            } as unknown as BrandCertificationItem,
        ];
    }
    return {
        header: {
            title: 'Сертификация производителем',
            timestamp_update: '1590420003476',
            is_updating: isUpdating,
        },
        brand_certification_records: records,
    } as unknown as BrandCertificationBlock;
}

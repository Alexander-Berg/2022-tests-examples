/* eslint-disable @typescript-eslint/no-use-before-define */
import type { BrandCertificationBlock, BrandCertificationItem, RawVinReport } from '@vertis/schema-registry/ts-types/auto/api/vin/vin_report_model';
import { Status } from '@vertis/schema-registry/ts-types/auto/api/vin/vin_resolution_enums';
import { toCamel } from 'snake-camel';
import { yogaToXml } from 'app/server/tmpl/XMLBuilder';
import { response200Paid } from 'app/server/resources/publicApi/methods/getCarfaxOfferReportRaw.fixtures';
import type { RawVinReportResponse } from '@vertis/schema-registry/ts-types/auto/api/response_model';
import { PhotoClass, PhotoType } from '@vertis/schema-registry/ts-types/auto/api/common_model';
import { BrandCertification } from './Creator';
import { createHelpers } from '../../Main';

// Step("Сертификация производителем (нет записей), хедер обновляется")
it('BrandCertificationCellTests_test_emptyStateUpdating()', () => {
    const report = reportMock();
    if (!report) {
        return;
    }

    if (report.brandCertification?.header) {
        report.brandCertification.header.isUpdating = true;
    }

    const xml = snapshot(report);
    expect(xml).toMatchSnapshot();
});

// Step("Сертификация производителем (нет записей), хедер не обновляется")
it('BrandCertificationCellTests_test_noRecords()', () => {
    const report = reportMock();
    if (!report) {
        return;
    }

    if (report.brandCertification?.brandCertificationRecords) {
        report.brandCertification.brandCertificationRecords = [];
    }

    const xml = snapshot(report);
    expect(xml).toMatchSnapshot();
});

// Step("Сертификация производителем 1 запись")
it('BrandCertificationCellTests_test_hasRecord()', () => {
    const report = reportMock();
    if (!report) {
        return;
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
    report.brandCertification = blockMock;
    report.brandCertification?.brandCertificationRecords.push(...records);

    return report;
}

function snapshot(report: RawVinReport): string {
    const helpers = createHelpers(report);
    const nodes = new BrandCertification().cellNodes(report, helpers);
    return yogaToXml(nodes);
}

const blockMock: BrandCertificationBlock = {
    header: {
        title: '',
        isUpdating: false,
        timestampUpdate: '',
    },
    brandCertificationRecords: [],
    status: Status.OK,
    recordCount: 0,
};

const records: Array<BrandCertificationItem> = [
    {
        programName: 'PorscheApproved',
        createTimestamp: '1_574_701_386_211',
        updateTimestamp: '1_577_725_422_514,',
        isActive: true,
        view: {
            name: 'PorscheApproved',
            advantagesHtml: [ 'Техническая проверка по&nbsp;111 пунктам' ],
            descriptionHtml: 'Автомобили, сертифицированные производителем по&nbsp;программе Porsche Approved*',
            descriptionUrl: 'https://www.porsche.com/russia/approvedused/porscheapproved/',
            logo: [
                {
                    name: '',
                    namespace: '',
                    isDeleted: false,
                    isInternal: false,
                    photoType: PhotoType.UNRECOGNIZED,
                    photoClass: PhotoClass.UNRECOGNIZED,
                    sizes: {
                        full: '//yastatic.net/s3/vertis-frontend/autoru-frontend/manufacturer-cert-logo/PorscheApproved/logo.png',
                    },
                },
            ],
        },
    } as unknown as BrandCertificationItem,
];

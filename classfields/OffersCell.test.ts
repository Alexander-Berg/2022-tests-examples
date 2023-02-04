import { createHelpers, makeFullReport } from 'app/server/tmpl/android/v1/Main';
import { AppConfig } from 'app/server/tmpl/android/v1/helpers/AppConfig';
import type { Request } from 'express';
import type {
    AutoruOffersBlock,
    RawVinReport,
} from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
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
    jest.spyOn(Math, 'random').mockReturnValue(0.99);
});

describe('offers_cell', () => {
    it('should not render offers_cell if it not exists', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.autoru_offers = undefined;

        const result = render(report, req);
        const offersCell = result.find((node) => node.id === 'offers');

        expect(offersCell).toBeUndefined();
    });

    it('block empty', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        // eslint-disable-next-line @typescript-eslint/no-use-before-define
        report.autoru_offers = offersEmpty;

        const result = render(report, req);
        const offersCell = result.find((node) => node.id === 'offers');
        const xml = offersCell ? yogaToXml([ offersCell ]) : '';

        expect(offersCell).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });

    it('block not ready', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        // eslint-disable-next-line @typescript-eslint/no-use-before-define
        report.autoru_offers = offersNotReady;

        const result = render(report, req);
        const offersCell = result.find((node) => node.id === 'offers');

        expect(get(offersCell, 'children[1].children[0].url')).toContain('content-loading-3');
    });

    it('block exist', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        // eslint-disable-next-line @typescript-eslint/no-use-before-define
        report.autoru_offers = offers as unknown as AutoruOffersBlock;

        const result = render(report, req);
        const offersCell = result.find((node) => node.id === 'offers');
        const xml = offersCell ? yogaToXml([ offersCell ]) : '';

        expect(offersCell).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });
});

function render(report: RawVinReport, req: Request) {
    const appConfig = new AppConfig({}, req);
    const helpers = createHelpers(report, appConfig);
    return makeFullReport(report, helpers);
}

const offersEmpty: AutoruOffersBlock = {
    header: {
        title: 'Нет объявлений на Авто.ру',
        timestamp_update: '1584444558871',
        is_updating: false,
    },
    offers: [],
    status: Status.OK,
    record_count: 0,
    comments_count: 0,
};

const offersNotReady: AutoruOffersBlock = {
    comments_count: 0,
    offers: [],
    record_count: 0,
    status: Status.IN_PROGRESS,
    header: {
        title: 'Нет объявлений на Авто.ру',
        is_updating: true,
        timestamp_update: '1584390758494',
    },
};

const offers = {
    header: {
        title: '3 объявления на Авто.ру',
        timestamp_update: '1591369895167',
    },
    offers: [
        {
            offer_id: '1091684546-ce539e5c',
            time_of_placement: '1568284097000',
            region_name: 'Сочи',
            offer_link: 'https://auto.ru/cars/used/sale/1091684546-ce539e5c/',
            mileage: 9511,
            seller_type: 'COMMERCIAL',
            mileage_status: 'OK',
        },
        {
            offer_id: '1096920492-7ee34a72',
            photo: {
                name: 'autoru-vos:65698-d4af6c0c718cd04b4a2bf55ea729ab48',
                sizes: {
                    thumb_m: '//images.mds-proxy.test.avto.ru/get-autoru-vos/65698/d4af6c0c718cd04b4a2bf55ea729ab48/thumb_m',
                    '832x624': '//images.mds-proxy.test.avto.ru/get-autoru-vos/65698/d4af6c0c718cd04b4a2bf55ea729ab48/832x624',
                    full: '//images.mds-proxy.test.avto.ru/get-autoru-vos/65698/d4af6c0c718cd04b4a2bf55ea729ab48/full',
                    '320x240': '//images.mds-proxy.test.avto.ru/get-autoru-vos/65698/d4af6c0c718cd04b4a2bf55ea729ab48/320x240',
                    '1200x900': '//images.mds-proxy.test.avto.ru/get-autoru-vos/65698/d4af6c0c718cd04b4a2bf55ea729ab48/1200x900',
                    small: '//images.mds-proxy.test.avto.ru/get-autoru-vos/65698/d4af6c0c718cd04b4a2bf55ea729ab48/small',
                    '120x90': '//images.mds-proxy.test.avto.ru/get-autoru-vos/65698/d4af6c0c718cd04b4a2bf55ea729ab48/120x90',
                    '92x69': '//images.mds-proxy.test.avto.ru/get-autoru-vos/65698/d4af6c0c718cd04b4a2bf55ea729ab48/92x69',
                    '456x342': '//images.mds-proxy.test.avto.ru/get-autoru-vos/65698/d4af6c0c718cd04b4a2bf55ea729ab48/456x342',
                    '1200x900n': '//images.mds-proxy.test.avto.ru/get-autoru-vos/65698/d4af6c0c718cd04b4a2bf55ea729ab48/1200x900n',
                },
                transform: {
                    angle: 0,
                    blur: true,
                },
                namespace: 'autoru-vos',
            },
            time_of_placement: '1584626414000',
            price: 5954000,
            region_name: 'Москва',
            offer_link: 'https://auto.ru/cars/used/sale/1096920492-7ee34a72/',
            mileage: 9511,
            seller_type: 'COMMERCIAL',
            mileage_status: 'OK',
        },
        {
            offer_id: '1098329048-59d2bd69',
            photo: {
                name: 'autoru-vos:2181224-a83e24c1440bd968cd892b56490e5829',
                sizes: {
                    thumb_m: '//images.mds-proxy.test.avto.ru/get-autoru-vos/2181224/a83e24c1440bd968cd892b56490e5829/thumb_m',
                    '832x624': '//images.mds-proxy.test.avto.ru/get-autoru-vos/2181224/a83e24c1440bd968cd892b56490e5829/832x624',
                    full: '//images.mds-proxy.test.avto.ru/get-autoru-vos/2181224/a83e24c1440bd968cd892b56490e5829/full',
                    '320x240': '//images.mds-proxy.test.avto.ru/get-autoru-vos/2181224/a83e24c1440bd968cd892b56490e5829/320x240',
                    '1200x900': '//images.mds-proxy.test.avto.ru/get-autoru-vos/2181224/a83e24c1440bd968cd892b56490e5829/1200x900',
                    small: '//images.mds-proxy.test.avto.ru/get-autoru-vos/2181224/a83e24c1440bd968cd892b56490e5829/small',
                    '120x90': '//images.mds-proxy.test.avto.ru/get-autoru-vos/2181224/a83e24c1440bd968cd892b56490e5829/120x90',
                    '92x69': '//images.mds-proxy.test.avto.ru/get-autoru-vos/2181224/a83e24c1440bd968cd892b56490e5829/92x69',
                    '456x342': '//images.mds-proxy.test.avto.ru/get-autoru-vos/2181224/a83e24c1440bd968cd892b56490e5829/456x342',
                    '1200x900n': '//images.mds-proxy.test.avto.ru/get-autoru-vos/2181224/a83e24c1440bd968cd892b56490e5829/1200x900n',
                },
                transform: {
                    angle: 0,
                    blur: false,
                },
                preview: {
                    version: 2,
                    width: 24,
                    height: 18,
                    // eslint-disable-next-line max-len
                    data: 'AAwDAQACEQMRAD8A6P4xftYfDT4lfCXxHpunSapb6hfaastut/BtXzvkZol+fcSAeW27eDgnqfX3+K3wt1Tw7o2p33i/S9O+1xRRD7Q4kdZPKBO4jdt54OehODXw/wCNfAfh3wbq2mxX9tLJY3bNDNc3VyY44QMcfKvUgnGf7tYfivRtBaG00zQvGM11qrW6mFHs1gto4lQuUM4K7zweVUZJ9yaiNJraT0Lnfax936R8X/hlHpyyzeKIbVQhRA6CMSFcfvAe6kcg9wRRXwN8Jfh94i8UWcia7fahHYsi+VGlyV2jBBG3BXoR7jB9TRWyU19pkKkpK9j3nxhbxXv2NLiNLhDuysqhgenrWR4J8LaKl9PIukWCyIzBWFsmR16HFFFT9o7uh2mnxrFF8ihOB90YooorZFI//9k=',
                },
                namespace: 'autoru-vos',
            },
            time_of_placement: '1590477156000',
            price: 5954000,
            region_name: 'Сочи',
            offer_link: 'https://auto.ru/cars/used/sale/1098329048-59d2bd69/',
            mileage: 9511,
            seller_type: 'COMMERCIAL',
            mileage_status: 'OK',
        },
    ],
};

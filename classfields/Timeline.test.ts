import { createHelpers, makeFullReport } from 'app/server/tmpl/android/v1/Main';
import { AppConfig } from 'app/server/tmpl/android/v1/helpers/AppConfig';
import type { Request } from 'express';
import type { HistoryBlock, RawVinReport } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { response200ForVin } from 'app/server/resources/publicApi/methods/getCarfaxOfferReportRaw.fixtures';
import { yogaToXml } from 'app/server/tmpl/XMLBuilder';

let req: Request;
beforeEach(() => {
    req = {
        headers: {
            'x-android-app-version': '10.0.0',
        },
    } as unknown as Request;
});

describe('timeline', () => {

    it('check timeline customs block', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        // eslint-disable-next-line @typescript-eslint/no-use-before-define
        report.history = timelineWithCustomsRecord as unknown as HistoryBlock;

        const result = render(report, req);
        const timelineCell = result.find((node) => node.id === 'history');
        const xml = timelineCell ? yogaToXml([ timelineCell ]) : '';

        expect(timelineCell).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });

    it('check timeline estimate block', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        // eslint-disable-next-line @typescript-eslint/no-use-before-define
        report.history = timelineWithEstimateRecord as unknown as HistoryBlock;

        const result = render(report, req);
        const timelineCell = result.find((node) => node.id === 'history');
        const xml = timelineCell ? yogaToXml([ timelineCell ]) : '';

        expect(timelineCell).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });

    it('check timeline fines block', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        // eslint-disable-next-line @typescript-eslint/no-use-before-define
        report.history = timelineWithFinesRecord as unknown as HistoryBlock;

        const result = render(report, req);
        const timelineCell = result.find((node) => node.id === 'history');
        const xml = timelineCell ? yogaToXml([ timelineCell ]) : '';

        expect(timelineCell).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });

    it('check timeline insurance payment block', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        // eslint-disable-next-line @typescript-eslint/no-use-before-define
        report.history = timelineWithInsurancePaymentRecord as unknown as HistoryBlock;

        const result = render(report, req);
        const timelineCell = result.find((node) => node.id === 'history');
        const xml = timelineCell ? yogaToXml([ timelineCell ]) : '';

        expect(timelineCell).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });

    it('check timeline insurance block', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        // eslint-disable-next-line @typescript-eslint/no-use-before-define
        report.history = timelineWithInsuranceRecord as unknown as HistoryBlock;

        const result = render(report, req);
        const timelineCell = result.find((node) => node.id === 'history');
        const xml = timelineCell ? yogaToXml([ timelineCell ]) : '';

        expect(timelineCell).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });

    it('check timeline brand certification block', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        // eslint-disable-next-line @typescript-eslint/no-use-before-define
        report.history = timelineWithBrandCertificationRecord as unknown as HistoryBlock;

        const result = render(report, req);
        const timelineCell = result.find((node) => node.id === 'history');
        const xml = timelineCell ? yogaToXml([ timelineCell ]) : '';

        expect(timelineCell).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });

    it('check timeline recall block', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        // eslint-disable-next-line @typescript-eslint/no-use-before-define
        report.history = timelineWithRecallRecord as unknown as HistoryBlock;

        const result = render(report, req);
        const timelineCell = result.find((node) => node.id === 'history');
        const xml = timelineCell ? yogaToXml([ timelineCell ]) : '';

        expect(timelineCell).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });

    it('check timeline offers block', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        // eslint-disable-next-line @typescript-eslint/no-use-before-define
        report.history = timelineWithOffersRecord as unknown as HistoryBlock;

        const result = render(report, req);
        const timelineCell = result.find((node) => node.id === 'history');
        const xml = timelineCell ? yogaToXml([ timelineCell ]) : '';

        expect(timelineCell).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });

    it('check timeline vehicle photo block', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        // eslint-disable-next-line @typescript-eslint/no-use-before-define
        report.history = timelineWithVehiclePhotoRecord as unknown as HistoryBlock;

        const result = render(report, req);
        const timelineCell = result.find((node) => node.id === 'history');
        const xml = timelineCell ? yogaToXml([ timelineCell ]) : '';

        expect(timelineCell).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });

    it('check timeline vehicle review block', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        // eslint-disable-next-line @typescript-eslint/no-use-before-define
        report.history = timelineWithVehicleReviewsRecord as unknown as HistoryBlock;

        const result = render(report, req);
        const timelineCell = result.find((node) => node.id === 'history');
        const xml = timelineCell ? yogaToXml([ timelineCell ]) : '';

        expect(timelineCell).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });

    it('check timeline partner block', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        // eslint-disable-next-line @typescript-eslint/no-use-before-define
        report.history = timelineWithPartnerRecord as unknown as HistoryBlock;

        const result = render(report, req);
        const timelineCell = result.find((node) => node.id === 'history');
        const xml = timelineCell ? yogaToXml([ timelineCell ]) : '';

        expect(timelineCell).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });
});

function render(report: RawVinReport, req: Request) {
    const appConfig = new AppConfig({}, req);
    const helpers = createHelpers(report, appConfig);
    return makeFullReport(report, helpers);
}

const timelineWithCustomsRecord = {
    header: {
        title: 'История эксплуатации',
        timestamp_update: '1608560284675',
        is_updating: false,
    },
    owners: [
        {
            owner: {
                index: 0,
                owner_type: {
                    region: 'Эстония',
                },
                time_from: 1540155600000,
                time_to: 1543611600000,
                registration_status: 'UNKNOWN_REGISTRATION_STATUS',
            },
            history_records: [
                {
                    title: 'Таможня',
                    customs_record: {
                        date: 1617092491774,
                        country_from_name: 'Занзибар',
                        country_to_name: 'Эстония',
                    },
                },
            ],
        },
    ],
};

const timelineWithEstimateRecord = {
    header: {
        title: 'История эксплуатации',
        timestamp_update: '1608560284675',
        is_updating: false,
    },
    owners: [
        {
            owner: {
                index: 0,
                registration_status: 'UNKNOWN_REGISTRATION_STATUS',
            },
        },
        {
            owner: {
                index: 0,
                owner_type: {
                    region: 'Эстония',
                },
                time_from: 1540155600000,
                time_to: 1543611600000,
                registration_status: 'NOT_REGISTERED',
            },
            history_records: [
                {
                    estimate_record: {
                        date: 1617092491000,
                        region_name: 'Махачкала',
                        mileage: 123456,
                        partner_name: 'Торги России',
                        partner_url: 'https://торги-россии.рф/',
                        images: [
                            {
                                name: 'autoru-carfax:1397989-Ogz6uRIR6NfSfZILolqihdnN3wSbSxV4r',
                                sizes: {
                                    thumb_m: '//images.mds-proxy.test.avto.ru/get-autoru-carfax/1397989/Ogz6uRIR6NfSfZILolqihdnN3wSbSxV4r/thumb_m',
                                },
                            },
                        ],
                        results: {
                            report_url: 'https://торги-россии.рф/',
                            summary: 'Автомобиль реализовывался на торгах по банкротству юридических или физических лиц',
                            price_from: 9999,
                            price_to: 99999,
                            inspection_details: [],
                            repair: undefined,
                        },
                    },
                },
            ],
        },
    ],
};

const timelineWithFinesRecord = {
    header: {
        title: 'История эксплуатации',
        timestamp_update: '1608560284675',
        is_updating: false,
    },
    owners: [
        {
            owner: {
                index: 0,
                owner_type: {
                    region: 'Эстония',
                },
                time_from: 1540155600000,
                time_to: 1543611600000,
                registration_status: 'NOT_REGISTERED',
            },
            history_records: [
                {
                    fine_record: {
                        date_from: 1540155600000,
                        date_to: 1543611900000,
                        count: 3,
                        paid_count: 1,
                        cost: 12445.5,
                        paid_cost: 12345.5,
                    },
                },
                {
                    fine_record: {
                        date_from: 1540155600000,
                        date_to: 1543611900000,
                        count: 1,
                        paid_count: 1,
                        cost: 12445.5,
                        paid_cost: 12345.5,
                    },
                },
                {
                    fine_record: {
                        date_from: 1540155600000,
                        date_to: 1543611900000,
                        count: 1,
                        paid_count: 0,
                        need_payment_count: 1,
                        cost: 12445.5,
                        paid_cost: 12345.5,
                    },
                },
                {
                    fine_record: {
                        date_from: 1540155600000,
                        date_to: 1543611900000,
                        count: 1,
                        paid_count: 0,
                        need_payment_count: 0,
                        cost: 12445.5,
                        paid_cost: 12345.5,
                    },
                },
            ],
        },
    ],
};

const timelineWithInsurancePaymentRecord = {
    header: {
        title: 'История эксплуатации',
        timestamp_update: '1608560284675',
        is_updating: false,
    },
    owners: [
        {
            owner: {
                index: 0,
                owner_type: {
                    region: 'Эстония',
                },
                time_from: 1540155600000,
                time_to: 1543611600000,
                registration_status: 'NOT_REGISTERED',
            },
            history_records: [
                {
                    insurance_payment_record: {
                        date: 1519851600000,
                        amount: 49524,
                        policy_info: {
                            insurer_name: 'ИНГОССТРАХ',
                            insurance_type: 'OSAGO',
                        },
                    },
                },
                {
                    insurance_payment_record: {
                        date: 1519851600000,
                        amount: 49524,
                        policy_info: {
                            insurer_name: 'ИНГОССТРАХ',
                            insurance_type: 'KASKO',
                        },
                    },
                },
            ],
        },
    ],
};

const timelineWithInsuranceRecord = {
    header: {
        title: 'История эксплуатации',
        timestamp_update: '1608560284675',
        is_updating: false,
    },
    owners: [
        {
            owner: {
                index: 0,
                owner_type: {
                    region: 'Эстония',
                },
                time_from: 1540155600000,
                time_to: 1543611600000,
                registration_status: 'NOT_REGISTERED',
            },
            history_records: [
                {
                    title: 'Страховой полис',
                    insurance_record: {
                        date: 1617092491774,
                        insurance_type: 'KASKO',
                        from: 1617192691774,
                        to: 1617292691774,
                        mileage: 987654321,
                        region_name: 'Саратов',
                        partner_name: 'Яндекс',
                        partner_url: 'ya.ru',
                        mileage_status: 'OK',
                        serial: '12345',
                        number: '54321',
                        insurer_name: 'Я.Страхование',
                        insurance_status: 'ACTIVE',
                        policy_status: 'Ждет одобрения',
                    },
                },
            ],
        },
    ],
};

const timelineWithBrandCertificationRecord = {
    header: {
        title: 'История эксплуатации',
        timestamp_update: '1608560284675',
        is_updating: false,
    },
    owners: [
        {
            owner: {
                index: 0,
                owner_type: {
                    region: 'Эстония',
                },
                time_from: 1540155600000,
                time_to: 1543611600000,
                registration_status: 'NOT_REGISTERED',
            },
            history_records: [
                {
                    brand_certification_record: {
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
                            // eslint-disable-next-line max-len
                            description_html: 'В&nbsp;рамках специальной программы Mercedes-Benz Certified официальные дилеры &laquo;Мерседес-Бенц&raquo; предлагают вам приобрести сертифицированные автомобили с&nbsp;пробегом. Каждый сертифицированный автомобиль включает в&nbsp;себя: <ul><li>постгарантийную поддержку&nbsp;&mdash; 12&nbsp;месяцев без ограничения пробега;</li><li>поддержку на&nbsp;дорогах 24/7;</li><li>тщательную юридическую и&nbsp;техническую проверку.</li></ul>',
                            logo: [
                                {
                                    sizes: {
                                        full: '//yastatic.net/s3/vertis-frontend/autoru-frontend/manufacturer-cert-logo/MercedesBenz/logo.png',
                                    },
                                },
                            ],
                            description_url: 'https://used.mercedes-benz.ru/Certified/',
                        },
                        meta: {
                            source: {},
                        },
                    },
                },
            ],
        },
    ],
};

const timelineWithRecallRecord = {
    header: {
        title: 'История эксплуатации',
        timestamp_update: '1608560284675',
        is_updating: false,
    },
    owners: [
        {
            owner: {
                index: 0,
                owner_type: {
                    region: 'Эстония',
                },
                time_from: 1540155600000,
                time_to: 1543611600000,
                registration_status: 'NOT_REGISTERED',
            },
            history_records: [
                {
                    recall_record: {
                        recall: {
                            recall_id: '1816',
                            campaign_id: '425',
                            title: 'Обжимаемый участок фитинга тормозного шланга имеет недостаточное уплотнение',
                            // eslint-disable-next-line max-len
                            description: 'Причиной отзыва транспортных средств является то, что в результате недостаточной оценки долговечности тормозного шланга под действием давления тормозной жидкости на некоторых автомобилях обжимаемый участок фитинга шланга имеет недостаточное уплотнение. В результате этого тормозная жидкость может выдавливаться между внутренним и внешним слоями шланга, приводя к его вздутию.',
                            // eslint-disable-next-line max-len
                            url: 'https://www.rst.gov.ru/portal/gost/home/presscenter/news?portal:componentId=88beae40-0e16-414c-b176-d0ab5de82e16&navigationalstate=JBPNS_rO0ABXczAAZhY3Rpb24AAAABAA5zaW5nbGVOZXdzVmlldwACaWQAAAABAAQ2NTI2AAdfX0VPRl9f',
                            published_timestamp: '1575968400000',
                        },
                        meta: {
                            source: {},
                        },
                    },
                },
            ],
        },
    ],
};

const timelineWithOffersRecord = {
    header: {
        title: 'История эксплуатации',
        timestamp_update: '1608560284675',
        is_updating: false,
    },
    owners: [
        {
            owner: {
                index: 0,
                owner_type: {
                    region: 'Эстония',
                },
                time_from: 1540155600000,
                time_to: 1543611600000,
                registration_status: 'NOT_REGISTERED',
            },
            history_records: [
                {
                    offer_record: {
                        time_of_placement: '1584626414000',
                        price: 5954000,
                        region_name: 'Москва',
                        offer_link: 'https://auto.ru/cars/used/sale/1096920492-7ee34a72/',
                        mileage: 9511,
                        seller_type: 'COMMERCIAL',
                        title: 'Размещение на Авто.ру',
                        mileage_status: 'OK',
                        meta: {
                            source: {},
                        },
                    },
                },
                {
                    offer_record: {
                        time_of_placement: '1590477156000',
                        price: 5954000,
                        region_name: 'Сочи',
                        offer_link: 'https://auto.ru/cars/used/sale/1098329048-59d2bd69/',
                        mileage: 9511,
                        seller_type: 'COMMERCIAL',
                        title: 'Размещение на Авто.ру',
                        mileage_status: 'OK',
                        meta: {
                            source: {},
                        },
                    },
                },
            ],
        },
    ],
};

const timelineWithVehiclePhotoRecord = {
    header: {
        title: 'История эксплуатации',
        timestamp_update: '1608560284675',
        is_updating: false,
    },
    owners: [
        {
            owner: {
                index: 0,
                owner_type: {
                    region: 'Эстония',
                },
                time_from: 1540155600000,
                time_to: 1543611600000,
                registration_status: 'NOT_REGISTERED',
            },
            history_records: [
                {
                    photo_record: {
                        date: '1430341200000',
                        gallery: [
                            {
                                name: 'autoru-carfax:2926091-Fy4IoqKAXEuQsw2QU4AKFvwmZLbRNpbxz',
                                sizes: {
                                    thumb_m: '//images.mds-proxy.test.avto.ru/get-autoru-carfax/2926091/Fy4IoqKAXEuQsw2QU4AKFvwmZLbRNpbxz/thumb_m',
                                },
                            },
                        ],
                    },
                },
                {
                    photo_record: {
                        date: '1560632400000',
                        gallery: [
                            {
                                name: 'autoru-carfax:2926091-qcVc4e1hyd45mvmpsdvQHYYsr2p84FLqr',
                                sizes: {
                                    thumb_m: '//images.mds-proxy.test.avto.ru/get-autoru-carfax/2926091/qcVc4e1hyd45mvmpsdvQHYYsr2p84FLqr/thumb_m',
                                },
                            },
                        ],
                    },
                },
            ],
        },
    ],
};

const timelineWithVehicleReviewsRecord = {
    header: {
        title: 'История эксплуатации',
        timestamp_update: '1608560284675',
        is_updating: false,
    },
    owners: [
        {
            owner: {
                index: 0,
                owner_type: {
                    region: 'Эстония',
                },
                time_from: 1540155600000,
                time_to: 1543611600000,
                registration_status: 'NOT_REGISTERED',
            },
            history_records: [
                {
                    review_record: {
                        url: 'ya.ru',
                        published_timestamp: '1608560284675',
                        photos: [
                            {
                                name: 'autoru-carfax:2926091-Fy4IoqKAXEuQsw2QU4AKFvwmZLbRNpbxz',
                                sizes: {
                                    thumb_m: '//images.mds-proxy.test.avto.ru/get-autoru-carfax/2926091/Fy4IoqKAXEuQsw2QU4AKFvwmZLbRNpbxz/thumb_m',
                                },
                            },
                        ],
                    },
                },
            ],
        },
    ],
};

const timelineWithPartnerRecord = {
    header: {
        title: 'История эксплуатации',
        timestamp_update: '1608560284675',
        is_updating: false,
    },
    owners: [
        {
            owner: {
                index: 0,
                owner_type: {
                    region: 'Эстония',
                },
                time_from: 1540155600000,
                time_to: 1543611600000,
                registration_status: 'NOT_REGISTERED',
            },
            history_records: [
                {
                    partner_record: {
                        timestamp: '1502485200000',
                        mileage: 83000,
                        partner_name: 'Данные техосмотра',
                        diagnostic_card_text: '084290011710524',
                        title: 'Отметка о пробеге',
                        mileage_status: 'OK',
                        valid_until_timestamp: '1534107600000',
                    },
                },
                {
                    partner_record: {
                        timestamp: '1502485200000',
                        mileage: 83000,
                        partner_name: 'Данные техосмотра',
                        diagnostic_card_text: '084290011710524',
                        title: 'Отметка о пробеге',
                        mileage_status: 'OK',
                    },
                },
            ],
        },
    ],
};

import { RequestStatus } from 'realty-core/types/network';

export const withoutReports = {
    order: [],
    byId: {},
};

export const initialStoreReports = {
    network: {
        pageStatus: RequestStatus.LOADED,
    },
    reports: withoutReports,
};

export const withReports = {
    order: ['17e4ef21bbfd4f759a1595a21cc7570d', '369b5f614b3b476cb71d2e813c495f68', '48df4cbd38e841e185f5ce41c47fbd23'],
    byId: {
        '17e4ef21bbfd4f759a1595a21cc7570d': {
            paidReportId: '17e4ef21bbfd4f759a1595a21cc7570d',
            reportDate: '2020-07-28T09:34:20.202Z',
            reportStatus: 'DONE',
            address: 'Россия, Москва, р-н Нагатино-Садовники, наб Нагатинская, д 18, корп 1',
            offerId: '7064892505662726657',
            area: 37.6,
            floor: '1',
            cadastralNumber: '77:08:0009030:1552',
            isOfferAccessibleByLink: true,
        },
        '369b5f614b3b476cb71d2e813c495f68': {
            paidReportId: '369b5f614b3b476cb71d2e813c495f68',
            reportDate: '2020-07-30T19:10:03.582Z',
            reportStatus: 'DONE',
            address: 'Россия, Москва, Конюшковская улица, 28',
            offerId: '1369504662323673344',
            area: 49.5,
            floor: '5',
            cadastralNumber: '77:01:0004028:2063',
            isOfferAccessibleByLink: true,
        },
        '48df4cbd38e841e185f5ce41c47fbd23': {
            paidReportId: '48df4cbd38e841e185f5ce41c47fbd23',
            reportDate: '2020-08-03T12:21:21.390Z',
            reportStatus: 'DONE',
            address: 'Россия, Москва, р-н Нагатино-Садовники, наб Нагатинская, д 18, корп 1',
            offerId: '7064892505662726657',
            area: 37.6,
            floor: '1',
            cadastralNumber: '77:08:0009030:1552',
            isOfferAccessibleByLink: true,
        },
    },
};

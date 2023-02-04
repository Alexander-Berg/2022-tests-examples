import { IOfferCard, OfferType } from 'realty-core/types/offerCard';
import {
    FreeReportAccessibility,
    OwnerType,
    PaidReportAccessibility,
    PaidReportStatus,
} from 'realty-core/view/react/common/types/egrnPaidReport';

export const offer: Partial<IOfferCard> = {
    offerType: OfferType.SELL,
    obsolete: false,
    withExcerpt: true,
    freeReportAccessibility: FreeReportAccessibility.FRA_READY,
    paidReportAccessibility: PaidReportAccessibility.PPA_ALLOWED_TO_BUY,
    excerptReport: {
        cadastralNumber: '78:11:0006040:****',
        createDate: '2020-08-05T21:44:26.534Z',
        flatReport: {
            buildingInfo: {
                area: 31.1,
                floorString: '3',
                address: 'пр-кт Энергетиков, д 30, корп 1, литера А, кв *',
            },
            costInfo: {
                cadastralCost: 3046660,
            },
            currentRights: [
                {
                    owners: [
                        {
                            type: OwnerType.NATURAL_PERSON,
                            name: 'Ф****** М.С.',
                        },
                    ],
                    registration: {
                        idRecord: '369411403678',
                        regNumber: '78:11:0006040:****-78/032/2017-3',
                        type: 'SHARE_OWNERSHIP',
                        regDate: '2017-05-23T00:00:00Z',
                        shareText: '1/2',
                    },
                },
                {
                    owners: [
                        {
                            type: OwnerType.NATURAL_PERSON,
                            name: 'К**** Н.В.',
                        },
                    ],
                    registration: {
                        idRecord: '369411402878',
                        regNumber: '78:11:0006040:****-78/032/2017-2',
                        type: 'SHARE_OWNERSHIP',
                        regDate: '2017-05-23T00:00:00Z',
                        shareText: '1/2',
                    },
                },
            ],
            previousRights: [
                {
                    owners: [
                        {
                            type: OwnerType.GOVERNANCE,
                        },
                    ],
                    registration: {
                        idRecord: '369411402278',
                        regNumber: '40000***',
                        type: 'OWNERSHIP',
                        regDate: '1997-10-16T00:00:00Z',
                        endDate: '2017-05-23T00:00:00Z',
                    },
                },
            ],
            encumbrances: [],
            currentOwnersCount: 2,
        },
    },
    paidReportsInfo: {
        price: {
            base: 410,
            effective: 123,
            isAvailable: true,
            modifiers: {},
            reasons: [],
        },
        paidReports: [
            {
                paidReportId: 'da490952140548fd81f19e9fc109700d',
                reportStatus: PaidReportStatus.DONE,
                reportDate: '2020-08-31T13:47:50.795Z',
            },
        ],
    },
};

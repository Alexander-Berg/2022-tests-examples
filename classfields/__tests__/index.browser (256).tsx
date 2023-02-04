import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { IOfferCard, OfferType } from 'realty-core/types/offerCard';
import { PaidReportAccessibility } from 'realty-core/view/react/common/types/egrnPaidReport';

import { OfferCardEGRNReportComponent } from '../';

const multiplePreviousRights = [
    {
        owners: [{ type: 'NATURAL_PERSON', name: 'А**** Н.В.' }],
        registration: {
            idRecord: '3694113987278',
            regNumber: '12300***',
            type: 'OWNERSHIP',
            regDate: '2018-05-23T00:00:00Z',
            endDate: '2019-05-23T00:00:00Z',
        },
    },
    {
        owners: [{ type: 'GOVERNANCE' }],
        registration: {
            idRecord: '369411402278',
            regNumber: '40000***',
            type: 'OWNERSHIP',
            regDate: '1997-10-16T00:00:00Z',
            endDate: '2018-05-23T00:00:00Z',
        },
    },
];

const offerMock = ({
    offerType: OfferType.SELL,
    withExcerpt: true,
    excerptReport: {
        cadastralNumber: '78:11:0006040:****',
        createDate: '2020-08-05T21:44:26.534Z',
        flatReport: {
            buildingInfo: { area: 31.1, floorString: '3', address: 'пр-кт Энергетиков, д 30, корп 1, литера А, кв *' },
            costInfo: { cadastralCost: '3046660' },
            currentRights: [
                {
                    owners: [{ type: 'NATURAL_PERSON', name: 'Ф****** М.С.' }],
                    registration: {
                        idRecord: '369411403678',
                        regNumber: '78:11:0006040:****-78/032/2017-3',
                        type: 'SHARE_OWNERSHIP',
                        regDate: '2019-05-23T00:00:00Z',
                        shareText: '1/2',
                    },
                },
                {
                    owners: [{ type: 'NATURAL_PERSON', name: 'К**** Н.В.' }],
                    registration: {
                        idRecord: '369411402878',
                        regNumber: '78:11:0006040:****-78/032/2017-2',
                        type: 'SHARE_OWNERSHIP',
                        regDate: '2019-05-23T00:00:00Z',
                        shareText: '1/2',
                    },
                },
            ],
            previousRights: [
                {
                    owners: [{ type: 'GOVERNANCE' }],
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
} as unknown) as IOfferCard;

const spoilerButtonSelector = 'button[class^="Spoiler__button"]';

const emptyProps = {
    openEGRNPaymentPopup: () => undefined,
    url: '',
    retPath: '',
    buildLoginUrl: () => '',
    buildRetpath: () => '',
    link: () => '',
};

const OFFER_CARD_EGRN_REPORT_TEST_CASES = [
    {
        isAuth: false,
        pageParams: {},
        offer: { ...offerMock, excerptReport: undefined, excerptReportBrief: {} } as IOfferCard,
        description: 'рендерится для незалогина',
        height: 300,
    },
    { isAuth: true, pageParams: {}, offer: offerMock, description: 'рендерится для залогина', height: 300 },
    {
        isAuth: true,
        pageParams: { fromEGRN: 'free' },
        offer: offerMock,
        description: 'рендерится для залогина, развёрнут',
        height: 900,
    },
    {
        isAuth: true,
        pageParams: { fromEGRN: 'free' },
        offer: {
            ...offerMock,
            excerptReport: {
                ...offerMock.excerptReport,
                flatReport: { ...offerMock.excerptReport?.flatReport, previousRights: multiplePreviousRights },
            },
        } as IOfferCard,
        description: 'рендерится для залогина с несколькими предыдущими собственниками, развёрнут',
        height: 900,
    },
    {
        isAuth: true,
        pageParams: { fromEGRN: 'free' },
        offer: { ...offerMock, offerType: OfferType.RENT },
        description: 'рендерится на арендном оффере для залогина, развёрнут',
        height: 900,
    },
] as const;

describe('OfferCardEGRNReport', () => {
    OFFER_CARD_EGRN_REPORT_TEST_CASES.forEach(({ isAuth, pageParams, offer, description, height }) => {
        it(`${description}; узкий экран`, async () => {
            await render(
                <OfferCardEGRNReportComponent
                    isAuth={isAuth}
                    pageParams={pageParams}
                    pageType="offer"
                    offer={offer}
                    {...emptyProps}
                />,
                { viewport: { width: 340, height } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it(`${description}; альбомная ориентация`, async () => {
            await render(
                <OfferCardEGRNReportComponent
                    isAuth={isAuth}
                    pageParams={pageParams}
                    pageType="offer"
                    offer={offer}
                    {...emptyProps}
                />,
                { viewport: { width: 650, height } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    // eslint-disable-next-line max-len
    it('рендерится для залогина с несколькими предыдущими собственниками (спойлеры раскрыты), развёрнут, узкий экран', async () => {
        await render(
            <OfferCardEGRNReportComponent
                isAuth
                pageParams={{ fromEGRN: 'free' }}
                pageType="offer"
                offer={
                    {
                        ...offerMock,
                        excerptReport: {
                            ...offerMock.excerptReport,
                            flatReport: {
                                ...offerMock.excerptReport?.flatReport,
                                previousRights: multiplePreviousRights,
                            },
                        },
                    } as IOfferCard
                }
                {...emptyProps}
            />,
            { viewport: { width: 340, height: 1000 } }
        );

        await page.click(spoilerButtonSelector);
        await page.click(spoilerButtonSelector);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерит текст про 0 собственников; узкий экран', async () => {
        await render(
            <OfferCardEGRNReportComponent
                isAuth
                pageParams={{}}
                pageType="offer"
                offer={
                    {
                        ...offerMock,
                        excerptReport: {
                            ...offerMock.excerptReport,
                            flatReport: { ...offerMock.excerptReport?.flatReport, currentOwnersCount: 0 },
                        },
                    } as IOfferCard
                }
                {...emptyProps}
            />,
            { viewport: { width: 340, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерит текст про более 4 собственников; узкий экран', async () => {
        await render(
            <OfferCardEGRNReportComponent
                isAuth
                pageParams={{}}
                pageType="offer"
                offer={
                    {
                        ...offerMock,
                        excerptReport: {
                            ...offerMock.excerptReport,
                            flatReport: { ...offerMock.excerptReport?.flatReport, currentOwnersCount: 5 },
                        },
                    } as IOfferCard
                }
                {...emptyProps}
            />,
            { viewport: { width: 340, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерит свёрнутый отчёт с кнопкой покупки; узкий экран', async () => {
        await render(
            <OfferCardEGRNReportComponent
                isAuth
                pageParams={{}}
                pageType="offer"
                offer={
                    {
                        ...offerMock,
                        paidReportAccessibility: PaidReportAccessibility.PPA_ALLOWED_TO_BUY,
                        paidReportsInfo: {
                            ...offerMock.paidReportsInfo,
                            price: {
                                ...offerMock.paidReportsInfo?.price,
                                base: 400,
                                effective: 123,
                            },
                        },
                    } as IOfferCard
                }
                {...emptyProps}
            />,
            { viewport: { width: 340, height: 700 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерит свёрнутый отчёт с кнопкой покупки но без другой информации; узкий экран', async () => {
        await render(
            <OfferCardEGRNReportComponent
                isAuth
                pageParams={{}}
                pageType="offer"
                offer={
                    {
                        ...offerMock,
                        paidReportAccessibility: PaidReportAccessibility.PPA_ALLOWED_TO_BUY,
                        paidReportsInfo: {
                            ...offerMock.paidReportsInfo,
                            price: {
                                ...offerMock.paidReportsInfo?.price,
                                base: 400,
                                effective: 123,
                            },
                        },
                        excerptReport: undefined,
                    } as IOfferCard
                }
                {...emptyProps}
            />,
            { viewport: { width: 340, height: 700 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});

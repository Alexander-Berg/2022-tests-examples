import React from 'react';
import { render } from 'jest-puppeteer-react';
import { advanceTo } from 'jest-date-mock';
import { DeepPartial } from 'redux';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { IOfferCard } from 'realty-core/types/offerCard';
import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { AnyObject } from 'realty-core/types/utils';

import { IOfferPageStore } from 'view/react/deskpad/reducers/roots/offer';

import { OfferCardEGRNReport, IOfferEGRNReportComponentProps } from '../';

import { offer } from './mock';

const TEST_CASES = [
    {
        offer,
        description: 'рендерится со всеми тремя блоками (купленные отчёты, бесплатный отчёт, оплата)',
        height: 1300,
    },
    {
        offer: { ...offer, paidReportsInfo: { ...offer.paidReportsInfo, paidReports: [] } } as IOfferCard,
        description: 'рендерится без блока купленных отчётов',
        height: 1000,
    },
    {
        offer: { ...offer, excerptReport: undefined } as IOfferCard,
        description: 'рендерится без блока бесплатного отчёта',
        height: 750,
    },
    {
        offer: {
            ...offer,
            excerptReport: undefined,
            excerptReportBrief: { cadastralNumber: '78:11:0006040:****' },
        } as IOfferCard,
        description: 'рендерится c блоком бесплатного отчёта для незалогина',
        height: 950,
    },
    {
        offer: { ...offer, paidReportsInfo: undefined } as IOfferCard,
        description: 'рендерится без блоков купленных отчётов и оплаты',
        height: 500,
    },
] as const;

const initialState: DeepPartial<IOfferPageStore> = {
    payment: {
        EGRNPaidReport: {
            popup: {},
            stages: {
                init: {},
                perform: {},
                status: {},
            },
        },
        juridicalEGRNPaidReport: {
            popup: {},
            stages: {
                init: {},
                perform: {},
                status: {},
            },
        },
    },
};

advanceTo(new Date('2020-10-15'));

const renderComponent = (props: AnyObject, height: number) =>
    render(
        <AppProvider initialState={initialState}>
            <OfferCardEGRNReport {...(props as IOfferEGRNReportComponentProps)} />
        </AppProvider>,
        { viewport: { width: 850, height } }
    );

describe('OfferCardEGRNReport', () => {
    TEST_CASES.forEach(({ offer, description, height }) => {
        it(description, async () => {
            await renderComponent({ offer }, height);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});

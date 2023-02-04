import React, { ComponentProps } from 'react';
import { render } from 'jest-puppeteer-react';
import noop from 'lodash/noop';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import {
    HouseServicesPeriodType,
    HouseServicesAggregatedMeterReadingsStatus,
    HouseServicesPeriodBillStatus,
    HouseServicesPeriodReceiptStatus,
    HouseServicesPeriodPaymentConfirmationStatus,
    HouseServicesPeriodId,
} from 'types/houseServices';

import { AppProvider } from 'view/libs/test-helpers';

import { HouseServicesPeriodsList } from '../';

const mobileViewports = [
    { width: 345, height: 200 },
    { width: 360, height: 200 },
] as const;

const desktopViewports = [
    { width: 1000, height: 200 },
    { width: 1200, height: 200 },
] as const;

const viewports = [...mobileViewports, ...desktopViewports] as const;

const Component: React.ComponentType<ComponentProps<typeof HouseServicesPeriodsList>> = (props) => {
    return (
        <AppProvider
            fakeTimers={{
                now: new Date('2021-11-12T03:00:00.111Z').getTime(),
            }}
        >
            <HouseServicesPeriodsList {...props} />
        </AppProvider>
    );
};

describe('HouseServicesPeriodsList', () => {
    describe('Жилец', () => {
        viewports.forEach((viewport) => {
            it(`${viewport.width} px`, async () => {
                await render(
                    <Component
                        periods={[
                            {
                                periodId: 'f2b53938d9aaa5e5dbeac48aca2c379d' as HouseServicesPeriodId,
                                period: '2021-08',
                                periodType: HouseServicesPeriodType.REGULAR,
                                meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.NOT_SENT,
                                billStatus: HouseServicesPeriodBillStatus.NOT_SENT,
                                receiptStatus: HouseServicesPeriodReceiptStatus.SENT,
                                confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.SENT,
                            },
                            {
                                periodId: 'f2b53938d9aaa5e5dbeac48aca2c37df' as HouseServicesPeriodId,
                                period: '2021-07',
                                periodType: HouseServicesPeriodType.REGULAR,
                                meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.SENT,
                                billStatus: HouseServicesPeriodBillStatus.NOT_SENT,
                                receiptStatus: HouseServicesPeriodReceiptStatus.DECLINED,
                                confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.SENT,
                            },
                            {
                                periodId: '34a6cb9a71eb4d5dedaf57e1fb813463' as HouseServicesPeriodId,
                                period: '2021-06',
                                periodType: HouseServicesPeriodType.REGULAR,
                                meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.DECLINED,
                                billStatus: HouseServicesPeriodBillStatus.DECLINED,
                                receiptStatus: HouseServicesPeriodReceiptStatus.SENT,
                                confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.SENT,
                            },
                            {
                                periodId: 'dafd43e398c858fe1d54d22f1bf7ba46' as HouseServicesPeriodId,
                                period: '2021-05',
                                periodType: HouseServicesPeriodType.REGULAR,
                                meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.SENT,
                                billStatus: HouseServicesPeriodBillStatus.PAID,
                                receiptStatus: HouseServicesPeriodReceiptStatus.SENT,
                                confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.SENT,
                            },
                        ]}
                        onPeriodClick={noop}
                        role="tenant"
                    />,
                    { viewport }
                );

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
    describe('Собственник', () => {
        viewports.forEach((viewport) => {
            it(`${viewport.width} px`, async () => {
                await render(
                    <Component
                        periods={[
                            {
                                periodId: 'f2b53938d9aaa5e5dbeac48aca2c379a' as HouseServicesPeriodId,
                                period: '2021-08',
                                periodType: HouseServicesPeriodType.REGULAR,
                                meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.NOT_SENT,
                                billStatus: HouseServicesPeriodBillStatus.DECLINED,
                                receiptStatus: HouseServicesPeriodReceiptStatus.NOT_SENT,
                                confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.SENT,
                            },
                            {
                                periodId: 'f2b53938d9aaa5e5dbeac48aca2c379d' as HouseServicesPeriodId,
                                period: '2021-07',
                                periodType: HouseServicesPeriodType.REGULAR,
                                meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.NOT_SENT,
                                billStatus: HouseServicesPeriodBillStatus.SHOULD_BE_SENT,
                                receiptStatus: HouseServicesPeriodReceiptStatus.NOT_SENT,
                                confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.SENT,
                            },
                            {
                                periodId: '34a6cb9a71eb4d5dedaf57e1fb813463' as HouseServicesPeriodId,
                                period: '2021-06',
                                periodType: HouseServicesPeriodType.REGULAR,
                                meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.DECLINED,
                                billStatus: HouseServicesPeriodBillStatus.SHOULD_BE_PAID,
                                receiptStatus: HouseServicesPeriodReceiptStatus.SENT,
                                confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.SENT,
                            },
                            {
                                periodId: 'dafd43e398c858fe1d54d22f1bf7ba46' as HouseServicesPeriodId,
                                period: '2021-05',
                                periodType: HouseServicesPeriodType.REGULAR,
                                meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.SENT,
                                billStatus: HouseServicesPeriodBillStatus.PAID,
                                receiptStatus: HouseServicesPeriodReceiptStatus.SENT,
                                confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.SENT,
                            },
                        ]}
                        onPeriodClick={noop}
                        role="owner"
                    />,
                    { viewport }
                );

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});

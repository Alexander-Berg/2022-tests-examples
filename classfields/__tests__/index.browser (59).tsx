import React from 'react';
import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';

import { OwnerHouseServicesPeriodContainer } from '../container';

import {
    store,
    shouldSendBillStore,
    declinedBillStore,
    sentBillStore,
    paidBillStore,
    declinedMetricStore,
    declinedConfirmationStore,
    declinedReceiptStore,
    skeletonStore,
} from './stub/store';

const renderOptions = [
    { viewport: { width: 1000, height: 900 } },
    { viewport: { width: 630, height: 900 } },
    { viewport: { width: 375, height: 900 } },
];

const Component: React.FunctionComponent<{ store: DeepPartial<IUniversalStore> }> = ({ store }) => (
    <AppProvider
        initialState={store}
        fakeTimers={{
            now: new Date('2021-11-12T03:00:00.111Z').getTime(),
        }}
        bodyBackgroundColor={AppProvider.PageColor.USER_LK}
    >
        <OwnerHouseServicesPeriodContainer />
    </AppProvider>
);

describe('OwnerHouseServicesPeriod', () => {
    describe('Базовое состояние', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width} px`, async () => {
                await render(<Component store={store} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Показ нотификаций', () => {
        renderOptions.forEach((renderOption) => {
            it(`Требуется выслать счёт ${renderOption.viewport.width} px`, async () => {
                await render(<Component store={shouldSendBillStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        renderOptions.forEach((renderOption) => {
            it(`Жилец отклонил счёт, надо выслать заново ${renderOption.viewport.width} px`, async () => {
                await render(<Component store={declinedBillStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        renderOptions.forEach((renderOption) => {
            it(`Счёт выслан ${renderOption.viewport.width} px`, async () => {
                await render(<Component store={sentBillStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        renderOptions.forEach((renderOption) => {
            it(`Счёт оплачен ${renderOption.viewport.width} px`, async () => {
                await render(<Component store={paidBillStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        renderOptions.forEach((renderOption) => {
            it(`Отклонены показания ${renderOption.viewport.width} px`, async () => {
                await render(<Component store={declinedMetricStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        renderOptions.forEach((renderOption) => {
            it(`Отклонены квитанции ${renderOption.viewport.width} px`, async () => {
                await render(<Component store={declinedReceiptStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        renderOptions.forEach((renderOption) => {
            it(`Отклонено подтверждение оплаты ${renderOption.viewport.width} px`, async () => {
                await render(<Component store={declinedConfirmationStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Показ скелетона', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width} px`, async () => {
                await render(<Component store={skeletonStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});

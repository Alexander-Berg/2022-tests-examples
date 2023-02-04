import React from 'react';
import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';

import { OwnerHouseServicesPeriodsContainer } from '../container';

import {
    metricSendStore,
    paymentConfirmationSendStore,
    receiptSendStore,
    shouldSendBillStore,
    shouldPaidBillStore,
    emptyStore,
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
        <OwnerHouseServicesPeriodsContainer />
    </AppProvider>
);

describe('OwnerHouseServicesPeriods', () => {
    describe('Рендерится корректно', () => {
        renderOptions.forEach((renderOption) => {
            it(`Жилец передал показания${renderOption.viewport.width} px`, async () => {
                await render(<Component store={metricSendStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        renderOptions.forEach((renderOption) => {
            it(`Жилец передал фото об оплате${renderOption.viewport.width} px`, async () => {
                await render(<Component store={paymentConfirmationSendStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        renderOptions.forEach((renderOption) => {
            it(`Жилец передал квитанцию${renderOption.viewport.width} px`, async () => {
                await render(<Component store={receiptSendStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        renderOptions.forEach((renderOption) => {
            it(`Можно выставить счёт${renderOption.viewport.width} px`, async () => {
                await render(<Component store={shouldSendBillStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        renderOptions.forEach((renderOption) => {
            it(`Счёт выставлен и не оплачен${renderOption.viewport.width} px`, async () => {
                await render(<Component store={shouldPaidBillStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        renderOptions.forEach((renderOption) => {
            it(`Пустой список ${renderOption.viewport.width} px`, async () => {
                await render(<Component store={emptyStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        renderOptions.forEach((renderOption) => {
            it(`Скелетон ${renderOption.viewport.width} px`, async () => {
                await render(<Component store={skeletonStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});

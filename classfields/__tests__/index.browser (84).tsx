import React from 'react';
import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';

import { TenantHouseServicesPeriodsContainer } from '../container';

import {
    canSendMetricsStore,
    sholdPayStore,
    declinedReceiptStore,
    declinedConfirmationStore,
    declinedReceiptAndConfirmationStore,
    shouldSendReceiptStore,
    shouldSendConfirmationStore,
    shouldSendAllDataStore,
    withoutNotificationStore,
    emptyStore,
    skeletonStore,
} from './stub/store';

const renderOptions = [{ viewport: { width: 1000, height: 900 } }, { viewport: { width: 375, height: 900 } }];

const Component: React.FunctionComponent<{ store: DeepPartial<IUniversalStore> }> = ({ store }) => (
    <AppProvider
        initialState={store}
        fakeTimers={{
            now: new Date('2021-11-12T03:00:00.111Z').getTime(),
        }}
        bodyBackgroundColor={AppProvider.PageColor.USER_LK}
    >
        <TenantHouseServicesPeriodsContainer />
    </AppProvider>
);

describe('TenantHouseServicesPeriods', () => {
    describe('Рендерится корректно', () => {
        renderOptions.forEach((renderOption) => {
            it(`Можно отправить показания${renderOption.viewport.width} px`, async () => {
                await render(<Component store={canSendMetricsStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        renderOptions.forEach((renderOption) => {
            it(`Соб отклонил квитанции${renderOption.viewport.width} px`, async () => {
                await render(<Component store={declinedReceiptStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        renderOptions.forEach((renderOption) => {
            it(`Соб отклонил подтверждения оплаты${renderOption.viewport.width} px`, async () => {
                await render(<Component store={declinedConfirmationStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
        renderOptions.forEach((renderOption) => {
            it(`Соб отклонил квитанции и подтверждения оплаты${renderOption.viewport.width} px`, async () => {
                await render(<Component store={declinedReceiptAndConfirmationStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        renderOptions.forEach((renderOption) => {
            it(`Надо оплатить счёт${renderOption.viewport.width} px`, async () => {
                await render(<Component store={sholdPayStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        renderOptions.forEach((renderOption) => {
            it(`Надо отправить квитанции ${renderOption.viewport.width} px`, async () => {
                await render(<Component store={shouldSendReceiptStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        renderOptions.forEach((renderOption) => {
            it(`Надо отправить подтверждения оплаты ${renderOption.viewport.width} px`, async () => {
                await render(<Component store={shouldSendConfirmationStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        renderOptions.forEach((renderOption) => {
            it(`Надо отправить все данные ${renderOption.viewport.width} px`, async () => {
                await render(<Component store={shouldSendAllDataStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        renderOptions.forEach((renderOption) => {
            it(`Без нотификации ${renderOption.viewport.width} px`, async () => {
                await render(<Component store={withoutNotificationStore} />, renderOption);

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

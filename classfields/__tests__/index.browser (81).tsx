import React from 'react';
import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import ModalDisplay from 'view/components/ModalDisplay';
import { userReducer } from 'view/entries/user/reducer';

import { TenantHouseServicesPeriodContainer } from '../container';

import {
    sentStore,
    shouldBeSentStore,
    declinedMetricStore,
    declinedReceiptStore,
    declinedConfirmationStore,
    declinedSeveralDataStore,
    billPayedStore,
    shouldPayBillStore,
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
        rootReducer={userReducer}
        fakeTimers={{
            now: new Date('2021-11-12T03:00:00.111Z').getTime(),
        }}
        bodyBackgroundColor={AppProvider.PageColor.USER_LK}
    >
        <TenantHouseServicesPeriodContainer />
        <ModalDisplay />
    </AppProvider>
);

describe('TenantHouseServicesPeriod', () => {
    describe('Рендерится корректно', () => {
        renderOptions.forEach((renderOption) => {
            it(`Нужно отправить показания ${renderOption.viewport.width} px`, async () => {
                await render(<Component store={shouldBeSentStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        renderOptions.forEach((renderOption) => {
            it(`Показания отправлены ${renderOption.viewport.width} px`, async () => {
                await render(<Component store={sentStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        renderOptions.forEach((renderOption) => {
            it(`Одно из показаний отклонено ${renderOption.viewport.width} px`, async () => {
                await render(<Component store={declinedMetricStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        renderOptions.forEach((renderOption) => {
            it(`Отклонены фото квитанции ${renderOption.viewport.width} px`, async () => {
                await render(<Component store={declinedReceiptStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        renderOptions.forEach((renderOption) => {
            it(`Отклонены фото подтверждения оплаты ${renderOption.viewport.width} px`, async () => {
                await render(<Component store={declinedConfirmationStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        renderOptions.forEach((renderOption) => {
            it(`Отклонено несколько данных ${renderOption.viewport.width} px`, async () => {
                await render(<Component store={declinedSeveralDataStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        renderOptions.forEach((renderOption) => {
            it(`Счёт оплачен ${renderOption.viewport.width} px`, async () => {
                await render(<Component store={billPayedStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        renderOptions.forEach((renderOption) => {
            it(`Необходимо оплатить счёт ${renderOption.viewport.width} px`, async () => {
                await render(<Component store={shouldPayBillStore} />, renderOption);

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

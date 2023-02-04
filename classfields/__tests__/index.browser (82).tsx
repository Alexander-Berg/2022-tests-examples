import React from 'react';
import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import { userReducer } from 'view/entries/user/reducer';

import { TenantHouseServicesPeriodPaymentConfirmationContainer } from '../container';

import { notSentStore, shouldSentStore, sentStore, declinedStore, skeletonStore } from './stub';

const renderOptions = [
    { viewport: { width: 1200, height: 900 } },
    { viewport: { width: 750, height: 900 } },
    { viewport: { width: 375, height: 900 } },
];

const Component: React.FunctionComponent<{ store: DeepPartial<IUniversalStore> }> = ({ store }) => (
    <AppProvider initialState={store} rootReducer={userReducer} bodyBackgroundColor={AppProvider.PageColor.USER_LK}>
        <TenantHouseServicesPeriodPaymentConfirmationContainer />
    </AppProvider>
);

describe('TenantHouseServicesPeriodReceipts', () => {
    describe('Базовые состояния', () => {
        renderOptions.forEach((renderOption) => {
            it(`Фотографии подтверждения оплаты не отправлены ${renderOption.viewport.width} px`, async () => {
                await render(<Component store={notSentStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });

            it(`Фотографии должны быть отправлены ${renderOption.viewport.width} px`, async () => {
                await render(<Component store={shouldSentStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });

            it(`Фотографии подтверждения оплаты отправлены ${renderOption.viewport.width} px`, async () => {
                await render(<Component store={sentStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });

            it(`Фотографии подтверждения оплаты отклонены ${renderOption.viewport.width} px`, async () => {
                await render(<Component store={declinedStore} />, renderOption);

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

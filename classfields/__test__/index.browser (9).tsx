import React from 'react';
import { render } from 'jest-puppeteer-react';

import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/libs/test-helpers';
import { rootReducer } from 'view/entries/manager/reducer';
import { IUniversalStore } from 'view/modules/types';

import { ManagerUserPaymentMethodsContainer } from '../container';

import * as stubs from './stubs/cards-store';

const renderOptions = [{ viewport: { width: 1200, height: 1000 } }, { viewport: { width: 460, height: 900 } }];

const Component: React.FunctionComponent<{ store: DeepPartial<IUniversalStore> }> = ({ store }) => (
    <AppProvider rootReducer={rootReducer} initialState={store}>
        <ManagerUserPaymentMethodsContainer />
    </AppProvider>
);

describe('ManagerUserPaymentMethods', () => {
    describe('скелетон страницы', () => {
        renderOptions.forEach((option) => {
            it(`${option.viewport.width}px${option.viewport.height}px`, async () => {
                await render(<Component store={stubs.storeWithSkeleton} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('У собственника нет карт для выплат', () => {
        renderOptions.forEach((option) => {
            it(`${option.viewport.width}px${option.viewport.height}px`, async () => {
                await render(<Component store={stubs.baseStore} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('У собственника есть одна карта', () => {
        renderOptions.forEach((option) => {
            it(`${option.viewport.width}px${option.viewport.height}px`, async () => {
                await render(<Component store={stubs.storeWithUserCards(1)} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('У собственника есть две карты', () => {
        renderOptions.forEach((option) => {
            it(`${option.viewport.width}px${option.viewport.height}px`, async () => {
                await render(<Component store={stubs.storeWithUserCards(2)} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });
});

import React from 'react';
import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';

import { AppProvider } from 'view/libs/test-helpers';
import { rootReducer } from 'view/entries/public/reducer';
import { IUniversalStore } from 'view/modules/types';

import { LandingCalculatorArenda } from '../index';

import { getStore, getStoreWithForm, getStoreWithLoadingForm } from './stubs';

const Component: React.FunctionComponent<{ store: DeepPartial<IUniversalStore>; Gate?: AnyObject }> = ({
    store,
    Gate,
}) => (
    <AppProvider rootReducer={rootReducer} initialState={store} Gate={Gate}>
        <LandingCalculatorArenda />
    </AppProvider>
);

const renderOptions = [
    {
        display: {
            viewport: {
                width: 1400,
                height: 1000,
            },
        },
    },
    {
        isMobile: 'mobile',
        display: {
            viewport: {
                width: 768,
                height: 810,
            },
        },
    },
    {
        isMobile: 'mobile',
        display: {
            viewport: {
                width: 375,
                height: 896,
            },
        },
    },
];

describe('LandingCalculatorArenda', () => {
    describe('Базовый рендеринг', () => {
        renderOptions.forEach(({ isMobile, display }) => {
            it(`${display.viewport.width}px ${display.viewport.height}`, async () => {
                await render(<Component store={getStore(isMobile)} />, display);
                await page.addStyleTag({ content: 'body{padding: 0}' });

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Состояние загрузки формы', () => {
        renderOptions.forEach(({ isMobile, display }) => {
            it(`${display.viewport.width}px ${display.viewport.height}`, async () => {
                await render(<Component store={getStoreWithLoadingForm(isMobile)} />, display);
                await page.addStyleTag({ content: 'body{padding: 0}' });

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Показ формы оценки', () => {
        renderOptions.forEach(({ isMobile, display }) => {
            it(`${display.viewport.width}px ${display.viewport.height}`, async () => {
                await render(<Component store={getStoreWithForm(isMobile)} />, display);
                await page.addStyleTag({ content: 'body{padding: 0}' });

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});

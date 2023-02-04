import React from 'react';
import { render } from 'jest-puppeteer-react';

import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/libs/test-helpers';

import { rootReducer } from 'view/entries/manager/reducer';

import { IUniversalStore } from 'view/modules/types';

import { ManagerSearch } from '../index';

import { ManagerSearchTab } from '../types';

import flatStyles from '../ManagerSearchFlats/ManagerSearchFlatsFilters/styles.module.css';

import { getFlatsStore } from './stubs/flats';
import * as stubs from './stubs/users';

const renderOptions = [{ viewport: { width: 1200, height: 1000 } }, { viewport: { width: 460, height: 800 } }];

const selectors = {
    flatInput: `.${flatStyles.query} input`,
    selectButton: `.${flatStyles.select} button`,
    allCheckbox: `.${flatStyles.allCheckbox}`,
};

const FlatsComponent: React.FunctionComponent<{ store: DeepPartial<IUniversalStore> }> = ({ store }) => (
    <AppProvider rootReducer={rootReducer} initialState={store}>
        <ManagerSearch tab={ManagerSearchTab.FLATS} />
    </AppProvider>
);

describe('ManagerSearchFlats', () => {
    describe('пустая страница', () => {
        renderOptions.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<FlatsComponent store={getFlatsStore(0, 1)} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
    describe('рендер скелетона', () => {
        renderOptions.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<FlatsComponent store={stubs.onPending} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
    describe('первая страница', () => {
        renderOptions.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<FlatsComponent store={getFlatsStore(5, 1)} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
    describe(' последняя страница', () => {
        renderOptions.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<FlatsComponent store={getFlatsStore(1, 30)} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
    describe('ввод в инпут на странице', () => {
        renderOptions.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<FlatsComponent store={getFlatsStore(5, 1)} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                await page.type(selectors.flatInput, 'г Москва улица Кушелева д 5, кв 34');
                await page.$eval(selectors.flatInput, async (e) => {
                    (e as HTMLInputElement).blur();
                });

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
    describe('клик на селектор', () => {
        renderOptions.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<FlatsComponent store={getFlatsStore(5, 1)} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.selectButton);

                await page.waitForSelector('.Popup_visible');

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
    describe('клик на чекбокс  - "Все" ', () => {
        renderOptions.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<FlatsComponent store={getFlatsStore(5, 1)} />, option);

                await page.click(selectors.selectButton);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.allCheckbox);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.allCheckbox);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});

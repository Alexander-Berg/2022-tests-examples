import React from 'react';
import { render } from 'jest-puppeteer-react';

import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/libs/test-helpers';

import { rootReducer } from 'view/entries/manager/reducer';

import { IUniversalStore } from 'view/modules/types';

import { ManagerSearch } from '../index';

import { ManagerSearchTab } from '../types';

import styles from '../ManagerSearchUsers/ManagerSearchUsersFilters/styles.module.css';

import * as stubs from './stubs/users';

const renderOptions = [{ viewport: { width: 1200, height: 1000 } }, { viewport: { width: 460, height: 800 } }];

const selectors = {
    input: `.${styles.query} input`,
};

const UsersComponent: React.FunctionComponent<{ store: DeepPartial<IUniversalStore> }> = ({ store }) => (
    <AppProvider rootReducer={rootReducer} initialState={store}>
        <ManagerSearch tab={ManagerSearchTab.USERS} />
    </AppProvider>
);

describe('ManagerSearchUsers', () => {
    describe('рендер пустой страницы', () => {
        renderOptions.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<UsersComponent store={stubs.baseStore} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
    describe('рендер скелетона', () => {
        renderOptions.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<UsersComponent store={stubs.onPending} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
    describe('рендер четырех сниппетов', () => {
        renderOptions.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<UsersComponent store={stubs.toTenUsers} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
    describe('последняя страница', () => {
        renderOptions.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<UsersComponent store={stubs.toLastPage} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
    describe('ввод текста в инпут', () => {
        renderOptions.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<UsersComponent store={stubs.toTenUsers} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                await page.type(selectors.input, 'Имя Фамилия');
                await page.$eval(selectors.input, async (e) => {
                    (e as HTMLInputElement).blur();
                });
                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});

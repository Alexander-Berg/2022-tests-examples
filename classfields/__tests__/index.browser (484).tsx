import React from 'react';
import { render as _render } from 'jest-puppeteer-react';
import { advanceTo } from 'jest-date-mock';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider, IAppProviderProps } from 'realty-core/view/react/libs/test-helpers';

import rootReducer from 'view/react/deskpad/reducers/roots/favorites';

import { SharedFavorites } from '../';
import styles from '../styles.module.css';

import { initialState, Gate, sharedEmptyInitialState } from './mocks';

const render = async (props: Partial<IAppProviderProps> = {}) => {
    await _render(
        <AppProvider rootReducer={rootReducer} initialState={initialState} Gate={Gate} {...props}>
            <SharedFavorites />
        </AppProvider>,
        { viewport: { width: 1200, height: 500 } }
    );
    await page.addStyleTag({ content: 'body{padding: 0}' });
};

advanceTo(new Date('2021-04-07 15:59:00'));

describe('SharedFavorites', () => {
    it('рисует дефолтное состояние', async () => {
        await render();

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('добавляет все в избранное', async () => {
        await render();

        await page.click(`.${styles.headAddAllBtn} button`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('кнопка появляется снизу при скролле', async () => {
        await render();

        await page.evaluate(() => {
            window.scrollTo(0, 500);
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует с пустым списком', async () => {
        await render({ initialState: sharedEmptyInitialState });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});

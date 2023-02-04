import React from 'react';
import { render as _render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider, IAppProviderProps } from 'realty-core/view/react/libs/test-helpers';

import reducer from 'view/reducers/pages/SharedFavoritesPage';

import { SharedFavoritesContainer } from '../container';

import { initialState, Gate, sharedEmptyInitialState } from './mocks';

const render = (props: Partial<IAppProviderProps> = {}) =>
    _render(
        <AppProvider rootReducer={reducer} initialState={initialState} Gate={Gate} {...props}>
            <SharedFavoritesContainer pageType="shared-favorites" />
        </AppProvider>,
        { viewport: { width: 320, height: 1000 } }
    );

describe('SharedFavorites_mobile', () => {
    it('рисует дефолтное состояние', async () => {
        await render();

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('кнопка появляется снизу при скролле', async () => {
        await render();

        await page.evaluate(() => {
            window.scrollTo(0, 300);
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует с пустым списком', async () => {
        await render({ initialState: sharedEmptyInitialState });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});

import React from 'react';
import { render as _render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider, IAppProviderProps } from 'realty-core/view/react/libs/test-helpers';

import { DeveloperLandingHeaderContainer } from '../container';
import componentStyles from '../styles.module.css';

import { initialState, userLoginInitialState } from './mocks';

const desktopViewports = [1000, 1440] as const;

const render = async (props: IAppProviderProps = { initialState }, height = 100) => {
    for (const width of desktopViewports) {
        await _render(
            <AppProvider {...props}>
                <DeveloperLandingHeaderContainer view="samolet" />
            </AppProvider>,
            { viewport: { width, height } }
        );

        expect(
            await takeScreenshot({
                fullPage: true,
            })
        ).toMatchImageSnapshot();
    }
};

const renderOnce = (props: IAppProviderProps = { initialState }, height = 100) =>
    _render(
        <AppProvider {...props}>
            <DeveloperLandingHeaderContainer view="samolet" />
        </AppProvider>,
        { viewport: { width: 1000, height } }
    );

describe('DeveloperLandingHeader', () => {
    it('рендерится корректно', async () => {
        await render();
    });

    it('ховер на ссылку', async () => {
        await renderOnce();

        await page.hover(`.${componentStyles.navItem}`);

        expect(await takeScreenshot({ keepCursor: true, fullPage: true })).toMatchImageSnapshot();
    });

    it('рендерится с залогиненным пользователем', async () => {
        await render({ initialState: userLoginInitialState });
    });

    it('ховер на иконку пользователя', async () => {
        await renderOnce({ initialState: userLoginInitialState }, 700);

        await page.hover('.User');

        expect(await takeScreenshot({ keepCursor: true, fullPage: true })).toMatchImageSnapshot();
    });
});

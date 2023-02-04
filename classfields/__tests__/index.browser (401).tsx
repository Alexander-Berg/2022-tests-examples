import React from 'react';

import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import reducer from 'view/react/deskpad/reducers/roots/sites';

import { SiteCardGenPlanContainer } from '../container';
import styles from '../styles.module.css';

import { siteCard, siteCardWithFlyover, initialState, Gate } from './mocks';

describe('SiteCardGenPlan', () => {
    it('Рендерится корректно', async () => {
        await render(
            <AppProvider initialState={initialState}>
                <SiteCardGenPlanContainer card={siteCard} />
            </AppProvider>,
            { viewport: { width: 800, height: 800 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('Рендерится корректно c 3д полетом', async () => {
        await render(
            <AppProvider initialState={initialState}>
                <SiteCardGenPlanContainer card={siteCardWithFlyover} />
            </AppProvider>,
            { viewport: { width: 800, height: 800 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерится корректно очень большой экран', async () => {
        await render(
            <AppProvider initialState={initialState}>
                <SiteCardGenPlanContainer card={siteCard} />
            </AppProvider>,
            { viewport: { width: 1950, height: 800 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('Ховер на полигон', async () => {
        await render(
            <AppProvider initialState={initialState} Gate={Gate} rootReducer={reducer}>
                <SiteCardGenPlanContainer card={siteCard} />
            </AppProvider>,
            { viewport: { width: 800, height: 800 } }
        );

        await page.hover(`.${styles.polygon}`);

        expect(await takeScreenshot({ fullPage: true, keepCursor: true })).toMatchImageSnapshot();
    });
});

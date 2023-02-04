import React from 'react';

import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import reducer from 'view/reducers/pages/NewbuildingPage';

import styles from '../styles.module.css';
import { NewbuildingCardGenPlan } from '..';

import { siteCard, initialState, Gate, siteCardWithFlyover } from './mocks';

describe('NewbuildingCardGenPlan', () => {
    it('Рендерится корректно', async () => {
        await render(
            <AppProvider initialState={initialState}>
                <NewbuildingCardGenPlan card={siteCard} />
            </AppProvider>,
            { viewport: { width: 320, height: 800 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерится корректно c 3д полетом', async () => {
        await render(
            <AppProvider initialState={initialState}>
                <NewbuildingCardGenPlan card={siteCardWithFlyover} />
            </AppProvider>,
            { viewport: { width: 320, height: 800 } }
        );

        await page.addStyleTag({ content: 'body{padding: 0}' });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Клик на полигон', async () => {
        await render(
            <AppProvider initialState={initialState} Gate={Gate} rootReducer={reducer}>
                <NewbuildingCardGenPlan card={siteCard} />
            </AppProvider>,
            { viewport: { width: 320, height: 800 } }
        );

        await page.touchscreen.tap(150, 150);

        await page.click(`.${styles.polygonCenter}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});

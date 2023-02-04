import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { NewbuildingProgressQuarterPhotos } from '../index';
import styles from '../styles.module.css';

import { propsWithLotPhotos, propsWithFewPhotos } from './mocks';

describe('NewbuildingProgressQuarterPhotos', () => {
    it('корректно рендерит компонент', async () => {
        await render(
            <AppProvider>
                <NewbuildingProgressQuarterPhotos {...propsWithLotPhotos} />
            </AppProvider>,
            { viewport: { width: 375, height: 600 } }
        );

        expect(
            await takeScreenshot({
                fullPage: true,
            })
        ).toMatchImageSnapshot();
    });

    it('корректно рендерит компонент при малом количестве фото', async () => {
        await render(
            <AppProvider>
                <NewbuildingProgressQuarterPhotos {...propsWithFewPhotos} />
            </AppProvider>,
            { viewport: { width: 375, height: 600 } }
        );

        expect(
            await takeScreenshot({
                fullPage: true,
            })
        ).toMatchImageSnapshot();
    });

    it('отрабатывает клик по спойлеру для фото', async () => {
        await render(
            <AppProvider>
                <NewbuildingProgressQuarterPhotos {...propsWithLotPhotos} />
            </AppProvider>,
            { viewport: { width: 375, height: 600 } }
        );

        await page.click(`.${styles.dropdown}`);

        expect(
            await takeScreenshot({
                fullPage: true,
            })
        ).toMatchImageSnapshot();

        await page.click(`.${styles.dropdown}`);

        expect(
            await takeScreenshot({
                fullPage: true,
            })
        ).toMatchImageSnapshot();
    });
});

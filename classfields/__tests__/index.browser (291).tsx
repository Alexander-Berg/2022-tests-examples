import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SamoletMortgage } from '../';
import styles from '../styles.module.css';

const WIDTHS = [320, 360];

const Component = () => (
    <AppProvider>
        <SamoletMortgage rgid={1} />
    </AppProvider>
);

describe('SamoletMortgage', () => {
    WIDTHS.forEach((width) => {
        const renderParams = { viewport: { width, height: 1000 } };

        it(`Вкладка "Ипотека" (${width}px)`, async () => {
            await render(<Component />, renderParams);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it(`Вкладка "Военная ипотека" (${width}px)`, async () => {
            await render(<Component />, renderParams);

            await page.click(`.${styles.tab}:nth-child(2)`);

            await customPage.waitForAllImagesLoaded();

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it(`Вкладка "Рассрочка" (${width}px)`, async () => {
            await render(<Component />, renderParams);

            await page.click(`.${styles.tab}:nth-child(3)`);

            await customPage.waitForAllImagesLoaded();

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it(`Вкладка "Материнский капитал" (${width}px)`, async () => {
            await render(<Component />, renderParams);

            await page.click(`.${styles.tab}:nth-child(4)`);

            await customPage.waitForAllImagesLoaded();

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it(`Вкладка "Субсидии" (${width}px)`, async () => {
            await render(<Component />, renderParams);

            await page.click(`.${styles.tab}:nth-child(5)`);

            await customPage.waitForAllImagesLoaded();

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it(`Вкладка "Семейная ипотека" (${width}px)`, async () => {
            await render(<Component />, renderParams);

            await page.click(`.${styles.tab}:nth-child(6)`);

            await customPage.waitForAllImagesLoaded();

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});

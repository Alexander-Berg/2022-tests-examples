import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SamoletMortgage } from '../';
import styles from '../styles.module.css';

const WIDTHS = {
    1040: 'Узкий',
    1260: 'Широкий',
};

describe('SamoletMortgage', () => {
    Object.keys(WIDTHS).forEach((width) => {
        const desc = WIDTHS[width];
        const renderParams = { viewport: { width: Number(width), height: 700 } };

        it(`Вкладка "Ипотека" (${desc} экран)`, async () => {
            await render(
                <AppProvider>
                    <SamoletMortgage rgid={1} />
                </AppProvider>,
                renderParams
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it(`Вкладка "Военная ипотека" (${desc} экран)`, async () => {
            await render(
                <AppProvider>
                    <SamoletMortgage rgid={1} />
                </AppProvider>,
                renderParams
            );

            await page.click(`.${styles.tab}:nth-child(2)`);

            await customPage.waitForAllImagesLoaded();

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it(`Вкладка "Рассрочка" (${desc} экран)`, async () => {
            await render(
                <AppProvider>
                    <SamoletMortgage rgid={1} />
                </AppProvider>,
                renderParams
            );

            await page.click(`.${styles.tab}:nth-child(3)`);

            await customPage.waitForAllImagesLoaded();

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it(`Вкладка "Материнский капитал" (${desc} экран)`, async () => {
            await render(
                <AppProvider>
                    <SamoletMortgage rgid={1} />
                </AppProvider>,
                renderParams
            );

            await page.click(`.${styles.tab}:nth-child(4)`);

            await customPage.waitForAllImagesLoaded();

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it(`Вкладка "Субсидии" (${desc} экран)`, async () => {
            await render(
                <AppProvider>
                    <SamoletMortgage rgid={1} />
                </AppProvider>,
                renderParams
            );

            await page.click(`.${styles.tab}:nth-child(5)`);

            await customPage.waitForAllImagesLoaded();

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it(`Вкладка "Семейная ипотека" (${desc} экран)`, async () => {
            await render(
                <AppProvider>
                    <SamoletMortgage rgid={1} />
                </AppProvider>,
                renderParams
            );

            await page.click(`.${styles.tab}:nth-child(6)`);

            await customPage.waitForAllImagesLoaded();

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});

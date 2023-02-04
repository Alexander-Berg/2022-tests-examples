import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SamoletPromo } from '../';
import styles from '../styles.module.css';

const WIDTHS = {
    1040: 'Узкий',
    1260: 'Широкий',
};

describe('SamoletPromo', () => {
    Object.keys(WIDTHS).forEach((width) => {
        const desc = WIDTHS[width];
        const renderParams = { viewport: { width: Number(width), height: 700 } };

        it(`Рендерится корректно (${desc} экран)`, async () => {
            await render(
                <AppProvider>
                    <SamoletPromo rgid={1} />
                </AppProvider>,
                renderParams
            );

            await page.addStyleTag({ content: 'body{background-color: #f4f6fb}' });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    it(`Модалка квартира без приключений`, async () => {
        await render(
            <AppProvider>
                <SamoletPromo rgid={1} />
            </AppProvider>,
            { viewport: { width: 1260, height: 700 } }
        );

        await page.addStyleTag({ content: 'body{background-color: #f4f6fb}' });
        await page.click(`.${styles.leftItem2}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});

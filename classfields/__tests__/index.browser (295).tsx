import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SamoletPromo } from '../';
import styles from '../styles.module.css';

const WIDTHS = [320, 360];

describe('SamoletPromo', () => {
    WIDTHS.forEach((width) => {
        const renderParams = { viewport: { width: Number(width), height: 700 } };

        it(`Рендерится корректно (${width}px)`, async () => {
            await render(
                <AppProvider context={{ router: { params: { section: 'discounts' } } }}>
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
            <AppProvider context={{ router: { params: { section: 'discounts' } } }}>
                <SamoletPromo rgid={1} />
            </AppProvider>,
            { viewport: { width: 360, height: 700 } }
        );

        await page.click(`.${styles.item2}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});

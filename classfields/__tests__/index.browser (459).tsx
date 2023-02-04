import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SamoletAbout } from '../';

const WIDTHS = {
    1040: 'Узкий',
    1440: 'Широкий',
};

describe('SamoletAbout', () => {
    Object.keys(WIDTHS).forEach((width) => {
        const desc = WIDTHS[width];
        const renderParams = { viewport: { width: Number(width), height: 700 } };

        it(`Рендерится корректно (${desc} экран)`, async () => {
            await render(
                <AppProvider>
                    <SamoletAbout />
                </AppProvider>,
                renderParams
            );

            await page.addStyleTag({ content: 'body{background-color: #f4f6fb}' });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});

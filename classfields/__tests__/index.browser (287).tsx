import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SamoletAbout } from '../';

const WIDTHS = [320, 640];

describe('SamoletAbout', () => {
    WIDTHS.forEach((width) => {
        const renderParams = { viewport: { width, height: 1500 } };

        it(`Рендерится корректно (${width}px)`, async () => {
            await render(
                <AppProvider>
                    <SamoletAbout />
                </AppProvider>,
                renderParams
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
